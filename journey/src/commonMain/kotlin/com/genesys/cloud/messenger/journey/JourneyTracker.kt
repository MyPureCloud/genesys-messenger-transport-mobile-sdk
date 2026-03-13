package com.genesys.cloud.messenger.journey

import com.genesys.cloud.messenger.journey.config.BuildKonfig
import com.genesys.cloud.messenger.journey.config.DeploymentConfigChecker
import com.genesys.cloud.messenger.journey.model.App
import com.genesys.cloud.messenger.journey.model.AppEventRequest
import com.genesys.cloud.messenger.journey.model.Device
import com.genesys.cloud.messenger.journey.model.NetworkConnectivity
import com.genesys.cloud.messenger.journey.model.SdkLibrary
import com.genesys.cloud.messenger.journey.network.JourneyApi
import com.genesys.cloud.messenger.journey.network.JourneyUrls
import com.genesys.cloud.messenger.journey.network.defaultJourneyHttpClient
import com.genesys.cloud.messenger.journey.storage.CookieIdStorage
import com.genesys.cloud.messenger.journey.storage.CustomerCookieIdManager
import com.genesys.cloud.messenger.journey.util.logs.Log
import com.genesys.cloud.messenger.journey.util.logs.LogTag
import com.genesys.cloud.messenger.journey.validation.EventValidator
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

private const val SCREEN_VIEWED_EVENT = "screen_viewed"
private const val SEARCH_PERFORMED_EVENT = "search_performed"
private const val SDK_NAME = "GenesysCloudMessengerJourney"

/**
 * Main entry point for Journey App Events tracking.
 *
 * All session metadata must be set via setter methods before sending events.
 * The SDK auto-populates `customerCookieId` and `createdDate`.
 */
class JourneyTracker(private val configuration: JourneyConfiguration) {

    companion object {
        /**
         * Platform-specific context required for persistent storage.
         * On Android, set this to the application [android.content.Context] before
         * creating a [JourneyTracker] instance. On iOS this is unused.
         */
        var platformContext: Any? = null
    }

    init {
        initPlatformStorage(platformContext)
    }

    private val log = Log(configuration.logging, LogTag.JOURNEY_TRACKER)
    private val urls = JourneyUrls(configuration.domain, configuration.deploymentId)
    private val httpClient = defaultJourneyHttpClient(configuration.logging)
    private val api = JourneyApi(urls, httpClient, log.withTag(LogTag.JOURNEY_API))
    private val configChecker = DeploymentConfigChecker(urls, httpClient, log)
    private val cookieIdManager = CustomerCookieIdManager(
        CookieIdStorage(),
        log.withTag(LogTag.COOKIE_STORAGE),
    )
    private val eventValidator = EventValidator(log)

    // --- App fields (required) ---
    private var appName: String? = null
    private var appNamespace: String? = null
    private var appVersion: String? = null
    private var appBuildNumber: String? = null

    // --- Device fields (required) ---
    private var deviceCategory: String? = null
    private var deviceType: String? = null
    private var osFamily: String? = null
    private var osVersion: String? = null

    // --- Device fields (optional) ---
    private var isMobile: Boolean? = null
    private var screenHeight: Int? = null
    private var screenWidth: Int? = null
    private var screenDensity: Int? = null
    private var fingerprint: String? = null
    private var manufacturer: String? = null

    // --- Network connectivity (optional) ---
    private var carrier: String? = null
    private var bluetoothEnabled: Boolean? = null
    private var cellularEnabled: Boolean? = null
    private var wifiEnabled: Boolean? = null

    // --- Identity fields (optional) ---
    private var externalId: String? = null
    private var globalTraits: Map<String, String>? = null
    private var referrerUrl: String? = null

    // region Session Metadata Setters

    /** Sets the app name (maps to `app.name` in the API request). Required. */
    fun setAppName(name: String) { appName = name }

    /** Sets the app namespace (maps to `app.namespace`). Required. */
    fun setAppNamespace(namespace: String) { appNamespace = namespace }

    /** Sets the app version (maps to `app.version`). Required. */
    fun setAppVersion(version: String) { appVersion = version }

    /** Sets the app build number (maps to `app.buildNumber`). Required. */
    fun setAppBuildNumber(buildNumber: String) { appBuildNumber = buildNumber }

    /** Sets the device category (maps to `device.category`). Values: "mobile", "desktop", "tablet", "other". Required. */
    fun setDeviceCategory(category: String) { deviceCategory = category }

    /** Sets the device type (maps to `device.type`). Required. */
    fun setDeviceType(type: String) { deviceType = type }

    /** Sets the OS family (maps to `device.osFamily`). Required. */
    fun setOsFamily(osFamily: String) { this.osFamily = osFamily }

    /** Sets the OS version (maps to `device.osVersion`). Required. */
    fun setOsVersion(osVersion: String) { this.osVersion = osVersion }

    /** Sets whether the device is mobile (maps to `device.isMobile`). Optional. */
    fun setIsMobile(isMobile: Boolean) { this.isMobile = isMobile }

    /** Sets the screen height in pixels (maps to `device.screenHeight`). Optional. */
    fun setScreenHeight(screenHeight: Int) { this.screenHeight = screenHeight }

    /** Sets the screen width in pixels (maps to `device.screenWidth`). Optional. */
    fun setScreenWidth(screenWidth: Int) { this.screenWidth = screenWidth }

    /** Sets the screen density (maps to `device.screenDensity`). Optional. */
    fun setScreenDensity(screenDensity: Int) { this.screenDensity = screenDensity }

    /** Sets a device fingerprint (maps to `device.fingerprint`). Optional. */
    fun setFingerprint(fingerprint: String) { this.fingerprint = fingerprint }

    /** Sets the device manufacturer (maps to `device.manufacturer`). Optional. */
    fun setManufacturer(manufacturer: String) { this.manufacturer = manufacturer }

    /** Sets the mobile carrier name (maps to `networkConnectivity.carrier`). Optional. */
    fun setCarrier(carrier: String) { this.carrier = carrier }

    /** Sets whether Bluetooth is enabled (maps to `networkConnectivity.bluetoothEnabled`). Optional. */
    fun setBluetoothEnabled(bluetoothEnabled: Boolean) { this.bluetoothEnabled = bluetoothEnabled }

    /** Sets whether cellular is enabled (maps to `networkConnectivity.cellularEnabled`). Optional. */
    fun setCellularEnabled(cellularEnabled: Boolean) { this.cellularEnabled = cellularEnabled }

    /** Sets whether WiFi is enabled (maps to `networkConnectivity.wifiEnabled`). Optional. */
    fun setWifiEnabled(wifiEnabled: Boolean) { this.wifiEnabled = wifiEnabled }

    /** Sets the external ID for identity stitching (maps to `externalId`). Optional. */
    fun setExternalId(externalId: String) { this.externalId = externalId }

    /**
     * Sets global traits attached to every event.
     * Per-event traits passed to event methods will be merged with these; per-event values take precedence.
     */
    fun setTraits(traits: Map<String, String>) { globalTraits = traits }

    /** Sets a referrer URL (maps to `referrerUrl`). Optional. */
    fun setReferrerUrl(referrerUrl: String) { this.referrerUrl = referrerUrl }

    // endregion

    // region Event Methods

    /**
     * Sends a 'Screen Viewed' app event (AI-1302.3).
     *
     * @param screenName the name of the screen, view, or fragment.
     * @param attributes optional custom additional information.
     * @param searchQuery optional search query performed on this screen (AI-1302.4).
     * @param traits optional per-event traits for identity stitching.
     */
    suspend fun screenViewed(
        screenName: String,
        attributes: Map<String, Any>? = null,
        searchQuery: String? = null,
        traits: Map<String, String>? = null,
    ) {
        sendEvent(
            eventName = SCREEN_VIEWED_EVENT,
            screenName = screenName,
            attributes = attributes,
            searchQuery = searchQuery,
            traits = traits,
        )
    }

    /**
     * Sends a 'Search Performed' app event (AI-1302.5).
     *
     * @param screenName the name of the screen where the search took place.
     * @param attributes search attributes such as keywords, filters, and sorting options.
     * @param traits optional per-event traits for identity stitching.
     */
    suspend fun searchPerformed(
        screenName: String,
        attributes: Map<String, Any>? = null,
        traits: Map<String, String>? = null,
    ) {
        sendEvent(
            eventName = SEARCH_PERFORMED_EVENT,
            screenName = screenName,
            attributes = attributes,
            traits = traits,
        )
    }

    /**
     * Sends a custom app event (AI-1302.6).
     *
     * @param eventName the custom event type name. Must contain only alphanumeric characters,
     *        underscores, and hyphens, and be less than 255 characters long.
     * @param screenName the name of the screen where the event took place.
     * @param attributes optional data to collect as part of this event.
     * @param traits optional per-event traits for identity stitching.
     */
    suspend fun customEvent(
        eventName: String,
        screenName: String,
        attributes: Map<String, Any>? = null,
        traits: Map<String, String>? = null,
    ) {
        if (!eventValidator.isValidEventName(eventName)) {
            return
        }
        sendEvent(
            eventName = eventName,
            screenName = screenName,
            attributes = attributes,
            traits = traits,
        )
    }

    // endregion

    private suspend fun sendEvent(
        eventName: String,
        screenName: String,
        attributes: Map<String, Any>? = null,
        searchQuery: String? = null,
        traits: Map<String, String>? = null,
    ) {
        if (!ensureTrackingEnabled()) return

        val app = buildApp()
        val device = buildDevice()
        if (app == null || device == null) return

        val mergedTraits = mergeTraits(traits)
        val customerCookieId = cookieIdManager.getOrCreateCookieId()
        val now = kotlin.time.Clock.System.now()
        val truncated = kotlin.time.Instant.fromEpochSeconds(now.epochSeconds)
        val createdDate = truncated.toString().replace("Z", "+0000")

        val request = AppEventRequest(
            eventName = eventName,
            screenName = screenName,
            app = app,
            device = device,
            sdkLibrary = SdkLibrary(name = SDK_NAME, version = BuildKonfig.sdkVersion),
            networkConnectivity = buildNetworkConnectivity(),
            referrerUrl = referrerUrl,
            searchQuery = searchQuery,
            attributes = attributes?.toJsonElementMap(),
            traits = mergedTraits,
            externalId = externalId,
            customerCookieId = customerCookieId,
            createdDate = createdDate,
        )
        api.sendAppEvent(request)
    }

    private suspend fun ensureTrackingEnabled(): Boolean {
        val cached = configChecker.isTrackingEnabled
        if (cached != null) return cached
        return configChecker.check()
    }

    private fun buildApp(): App? {
        val name = appName
        val ns = appNamespace
        val ver = appVersion
        val build = appBuildNumber

        if (name == null || ns == null || ver == null || build == null) {
            log.e {
                "Cannot send app event: required app info not set. " +
                    "Call setAppName(), setAppNamespace(), setAppVersion(), and setAppBuildNumber() first."
            }
            return null
        }
        return App(name = name, namespace = ns, version = ver, buildNumber = build)
    }

    private fun buildDevice(): Device? {
        val cat = deviceCategory
        val type = deviceType
        val os = osFamily
        val osVer = osVersion

        if (cat == null || type == null || os == null || osVer == null) {
            log.e {
                "Cannot send app event: required device info not set. " +
                    "Call setDeviceCategory(), setDeviceType(), setOsFamily(), and setOsVersion() first."
            }
            return null
        }
        return Device(
            category = cat,
            type = type,
            osFamily = os,
            osVersion = osVer,
            isMobile = isMobile,
            screenHeight = screenHeight,
            screenWidth = screenWidth,
            screenDensity = screenDensity,
            fingerprint = fingerprint,
            manufacturer = manufacturer,
        )
    }

    private fun buildNetworkConnectivity(): NetworkConnectivity? {
        if (carrier == null && bluetoothEnabled == null && cellularEnabled == null && wifiEnabled == null) {
            return null
        }
        return NetworkConnectivity(
            carrier = carrier,
            bluetoothEnabled = bluetoothEnabled,
            cellularEnabled = cellularEnabled,
            wifiEnabled = wifiEnabled,
        )
    }

    private fun mergeTraits(perEventTraits: Map<String, String>?): Map<String, String>? {
        if (globalTraits == null && perEventTraits == null) return null
        val merged = mutableMapOf<String, String>()
        globalTraits?.let { merged.putAll(it) }
        perEventTraits?.let { merged.putAll(it) }
        return merged
    }

    private fun Map<String, Any>.toJsonElementMap(): Map<String, JsonElement> =
        mapValues { (_, value) -> JsonPrimitive(value.toString()) }
}
