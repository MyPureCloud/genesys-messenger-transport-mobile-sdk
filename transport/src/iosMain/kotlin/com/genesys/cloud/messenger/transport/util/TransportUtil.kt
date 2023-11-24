package com.genesys.cloud.messenger.transport.util

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.posix.memcpy

class TransportUtil {

    /**
     * Utility function that helps with conversion of NSData to KotlinByteArray.
     *
     * @param data the NSData to convert.
     *
     * @return the resulting KotlinByteArray.
     */
    fun nsDataToKotlinByteArray(data: NSData): ByteArray {
        return ByteArray(data.length.toInt()).apply {
            usePinned {
                memcpy(it.addressOf(0), data.bytes, data.length)
            }
        }
    }
}
