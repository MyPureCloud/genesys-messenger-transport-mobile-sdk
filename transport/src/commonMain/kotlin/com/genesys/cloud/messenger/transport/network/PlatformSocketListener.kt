package com.genesys.cloud.messenger.transport.network

import com.genesys.cloud.messenger.transport.core.ErrorCode
import com.genesys.cloud.messenger.transport.core.events.Event

internal interface PlatformSocketListener {
    fun onOpen()

    fun onFailure(
        t: Throwable,
        errorCode: ErrorCode = ErrorCode.WebsocketError
    )

    fun onMessage(text: String)

    fun onClosing(
        code: Int,
        reason: String
    )

    fun onClosed(
        code: Int,
        reason: String
    )
}
