package com.genesys.cloud.messenger.transport.core

internal sealed class Result<out T : Any> {
    data class Success<out T : Any>(val value: T) : Result<T>()

    data class Failure(val errorCode: ErrorCode, val message: String? = null, val throwable: Throwable? = null) : Result<Nothing>()
}

/**
 *  Use for Response.Success on requests with no body.
 */
internal class Empty
