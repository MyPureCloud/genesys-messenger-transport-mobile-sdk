package com.genesys.cloud.messenger.composeapp.platform

import platform.Foundation.*
import platform.UIKit.*

/**
 * iOS implementation of PlatformContext interface
 */
actual interface PlatformContext

/**
 * iOS wrapper for context - implements the platform interface
 */
class IosPlatformContext(val context: Any) : PlatformContext

/**
 * iOS implementation of PlatformOperations
 */
actual class PlatformOperations actual constructor(context: PlatformContext) {
    
    private val iosContext: Any = (context as IosPlatformContext).context
    
    /**
     * Show an iOS alert message
     */
    actual fun showToast(message: String) {
        val alert = UIAlertController.alertControllerWithTitle(
            title = null,
            message = message,
            preferredStyle = UIAlertControllerStyleAlert
        )
        
        val okAction = UIAlertAction.actionWithTitle(
            title = "OK",
            style = UIAlertActionStyleDefault,
            handler = null
        )
        alert.addAction(okAction)
        
        // Get the root view controller to present the alert
        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        rootViewController?.presentViewController(alert, animated = true, completion = null)
    }
    
    /**
     * Get iOS device information
     */
    actual fun getDeviceInfo(): DeviceInfo {
        val device = UIDevice.currentDevice
        val bundle = NSBundle.mainBundle
        
        // Get app version from bundle
        val appVersion = bundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "Unknown"
        
        // Get device identifier
        val deviceId = device.identifierForVendor?.UUIDString ?: "Unknown"
        
        return DeviceInfo(
            model = device.model,
            osVersion = "${device.systemName} ${device.systemVersion}",
            appVersion = appVersion,
            deviceId = deviceId
        )
    }
    
    /**
     * Open URL in iOS default browser
     */
    actual fun openUrl(url: String) {
        val nsUrl = NSURL.URLWithString(url)
        if (nsUrl != null && UIApplication.sharedApplication.canOpenURL(nsUrl)) {
            UIApplication.sharedApplication.openURL(nsUrl)
        }
    }
    
    /**
     * Get iOS app storage directory
     */
    actual fun getStorageDirectory(): String {
        // Simplified implementation for iOS
        return NSTemporaryDirectory()
    }
    
    /**
     * Check if network is available on iOS
     */
    actual fun isNetworkAvailable(): Boolean {
        // Simplified network check for iOS
        // In a production app, you would use a more sophisticated reachability check
        return true // Placeholder implementation
    }
}