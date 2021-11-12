package com.genesys.cloud.messenger.transport.util

import kotlinx.cinterop.alloc
import kotlinx.cinterop.cValue
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSOSStatusErrorDomain
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUUID
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecCopyErrorMessageString
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.darwin.OSStatus
import platform.darwin.noErr

internal class TokenStoreImpl(private val storeKey: String) : TokenStore {

    override val token: String
        get() = getToken() ?: NSUUID().UUIDString().also {
            store(it)
        }

    private fun store(value: String) {
        (value as NSString).dataUsingEncoding(NSUTF8StringEncoding)?.let {
            val query = CFDictionaryCreateMutable(null, 4, null, null)
            CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
            CFDictionaryAddValue(query, kSecAttrAccount, CFBridgingRetain(TOKEN_KEY))
            CFDictionaryAddValue(query, kSecValueData, CFBridgingRetain(it))
            CFDictionaryAddValue(query, kSecAttrService, CFBridgingRetain(storeKey))

            val status = SecItemAdd(query, null)
            if (status.toUInt() != noErr) {
                logError(status)
            }
        }
    }

    private fun getToken(): String? {
        val query = CFDictionaryCreateMutable(null, 5, null, null)
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrAccount, CFBridgingRetain(TOKEN_KEY))
        CFDictionaryAddValue(query, kSecReturnData, kCFBooleanTrue)
        CFDictionaryAddValue(query, kSecMatchLimit, kSecMatchLimitOne)
        CFDictionaryAddValue(query, kSecAttrService, CFBridgingRetain(storeKey))

        memScoped {
            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query, result.ptr)
            return if (status.toUInt() == noErr) {
                val data = CFBridgingRelease(result.value) as NSData
                NSString.create(data, NSUTF8StringEncoding) as String?
            } else {
                logError(status)
                null
            }
        }
    }

    private fun logError(status: OSStatus) {
        if (NSProcessInfo().isOperatingSystemAtLeastVersion(cValue { majorVersion = 11; minorVersion = 3; patchVersion = 0 })) {
            val error = SecCopyErrorMessageString(status, null)
            val errorMessage = CFBridgingRelease(error)
            println("$errorMessage")
        } else {
            val error = NSError.errorWithDomain(NSOSStatusErrorDomain, status.toLong(), null)
            println(error.localizedDescription)
        }
    }
}
