package com.genesys.cloud.messenger.transport.util

import kotlinx.cinterop.ExperimentalForeignApi
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
     * @return the resulting KotlinByteArray or KotlinByteArray(0) if provided NSDAta is empty.
     */
    @OptIn(ExperimentalForeignApi::class)
    fun nsDataToKotlinByteArray(data: NSData): ByteArray =
        try {
            ByteArray(data.length.toInt()).apply {
                usePinned {
                    memcpy(it.addressOf(0), data.bytes, data.length)
                }
            }
        } catch (exception: Exception) {
            ByteArray(0)
        }
}
