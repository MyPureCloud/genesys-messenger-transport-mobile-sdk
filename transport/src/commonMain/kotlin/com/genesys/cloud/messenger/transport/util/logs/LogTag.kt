package com.genesys.cloud.messenger.transport.util.logs

internal object LogTag {
    const val MESSAGING_CLIENT = "MMSDKTransportMessagingClient"
    const val API = "MMSDKTransportApi"
    const val WEBSOCKET = "MMSDKTransportWebSocket"
    const val OKHTTP = "MMSDKOkHttp"
    const val ATTACHMENT_HANDLER = "MMSDKAttachmentHandler"
    const val MESSAGE_STORE = "MMSDKMessageStore"
    const val TOKEN_STORE = "MMSDKTokenStore"
    const val HTTP_CLIENT = "MMSDKHttpClient"
    const val STATE_MACHINE = "Transport State Machine"
    const val RECONNECTION_HANDLER = "TransportReconnectionHandler"
    const val EVENT_HANDLER = "TransportEventHandler"
    const val TYPING_INDICATOR_PROVIDER = "TypingIndicatorProvider"
    const val HEALTH_CHECK_PROVIDER = "HealthCheckProvider"
}
