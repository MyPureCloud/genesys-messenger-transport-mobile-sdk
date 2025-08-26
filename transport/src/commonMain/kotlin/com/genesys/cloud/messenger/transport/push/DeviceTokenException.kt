package com.genesys.cloud.messenger.transport.push

import com.genesys.cloud.messenger.transport.core.ErrorCode

/**
 * Exception class representing errors that occur during device token operations.
 * This exception is thrown when there are issues related to device token management,
 *
 * @property errorCode an [ErrorCode] representing the specific type of error that occurred.
 * @property message an optional string providing additional details about the error.
 * @property cause an optional underlying exception that caused this error.
 **/
class DeviceTokenException(
    val errorCode: ErrorCode,
    message: String?,
    cause: Throwable?,
) : Exception(message, cause)
