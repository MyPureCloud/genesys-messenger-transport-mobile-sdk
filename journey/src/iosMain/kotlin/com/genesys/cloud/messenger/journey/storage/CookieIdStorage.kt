package com.genesys.cloud.messenger.journey.storage

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFAutorelease
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Foundation.timeIntervalSince1970
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.darwin.noErr

private const val SERVICE_NAME = "com.genesys.cloud.messenger.journey.cookie"
private const val KEY_COOKIE_ID = "customerCookieId"
private const val KEY_COOKIE_TIMESTAMP = "customerCookieIdTimestamp"
private const val ONE_YEAR_SECONDS = 365.0 * 24.0 * 60.0 * 60.0

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal actual class CookieIdStorage actual constructor() {

    actual fun getCustomerCookieId(): String? {
        val cookieId = getString(KEY_COOKIE_ID) ?: return null
        val timestampStr = getString(KEY_COOKIE_TIMESTAMP)
        if (timestampStr != null) {
            val storedTime = timestampStr.toDoubleOrNull() ?: 0.0
            val now = NSDate().timeIntervalSince1970
            if (now - storedTime > ONE_YEAR_SECONDS) {
                remove(KEY_COOKIE_ID)
                remove(KEY_COOKIE_TIMESTAMP)
                return null
            }
        }
        return cookieId
    }

    actual fun setCustomerCookieId(cookieId: String) {
        setString(KEY_COOKIE_ID, cookieId)
        setString(KEY_COOKIE_TIMESTAMP, NSDate().timeIntervalSince1970.toString())
    }

    private fun getString(key: String): String? {
        val query = keychainQuery(key, kSecReturnData to kCFBooleanTrue, kSecMatchLimit to kSecMatchLimitOne)
        return memScoped {
            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query, result.ptr)
            if (status.toUInt() == noErr) {
                val data = CFBridgingRelease(result.value) as? NSData ?: return null
                data.toKString()
            } else {
                null
            }
        }
    }

    private fun setString(key: String, value: String) {
        val data = value.toNSData() ?: return
        if (getString(key) != null) {
            val query = keychainQuery(key)
            val update = CFDictionaryCreateMutable(null, 1.convert(), null, null).apply {
                CFDictionaryAddValue(this, kSecValueData, CFBridgingRetain(data))
                CFAutorelease(this)
            }
            SecItemUpdate(query, update)
        } else {
            val query = keychainQuery(key, kSecValueData to CFBridgingRetain(data))
            SecItemAdd(query, null)
        }
    }

    private fun remove(key: String) {
        val query = keychainQuery(key)
        SecItemDelete(query)
    }

    private fun keychainQuery(key: String, vararg extras: Pair<CFTypeRef?, CFTypeRef?>): CFDictionaryRef? {
        val service = CFBridgingRetain(SERVICE_NAME)
        val account = CFBridgingRetain(key)
        val totalSize = 3 + extras.size
        return CFDictionaryCreateMutable(null, totalSize.convert(), null, null).apply {
            CFDictionaryAddValue(this, kSecClass, kSecClassGenericPassword)
            CFDictionaryAddValue(this, kSecAttrService, service)
            CFDictionaryAddValue(this, kSecAttrAccount, account)
            extras.forEach { (k, v) -> CFDictionaryAddValue(this, k, v) }
            CFAutorelease(this)
        }
    }

    private fun NSData.toKString(): String? =
        NSString.create(data = this, encoding = NSUTF8StringEncoding)?.toString()

    private fun String.toNSData(): NSData? =
        NSString.create(string = this).dataUsingEncoding(NSUTF8StringEncoding)
}
