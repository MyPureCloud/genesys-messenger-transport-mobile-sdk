package com.genesys.cloud.messenger.transport.network

import com.genesys.cloud.messenger.transport.core.ErrorCode

internal sealed class Response<out T : Any> {
    data class Success<out T : Any>(val value: T) : Response<T>()
    data class Failure(val errorCode: ErrorCode, val message: String?) : Response<Nothing>()
}
