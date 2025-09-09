package com.genesys.cloud.messenger.transport.core

/**
 * Exception class representing errors that occur within the Transport SDK.
 * This exception is thrown when there are issues related to SDK operations and state management.
 *
 * @property errorCode an [ErrorCode] representing the specific type of error that occurred.
 * @property message an optional string providing additional details about the error.
 * @property cause an optional underlying exception that caused this error.
 */
class TransportSDKException(
    val errorCode: ErrorCode,
    message: String?,
    cause: Throwable? = null,
) : Exception(message, cause)
