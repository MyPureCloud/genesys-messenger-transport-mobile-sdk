package com.genesys.cloud.messenger.transport.network

interface PlatformSocketListener {
    fun onOpen()
    fun onFailure(t: Throwable)
    fun onMessage(text: String)
    fun onClosing(code: Int, reason: String)
    fun onClosed(code: Int, reason: String)
}
