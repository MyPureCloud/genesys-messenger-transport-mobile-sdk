package com.genesys.cloud.messenger.transport.network

/**
 * A code that indicates why a WebSocket connection closed. Follows the codes defined in RFC 6455.
 */
internal enum class SocketCloseCode(val value: Int) {
    INVALID(0),
    NORMAL_CLOSURE(1000),
    GOING_AWAY(1001),
    PROTOCOL_ERROR(1002),
    UNSUPPORTED_DATA(1003),
    NO_STATUS_RECEIVED(1005),
    ABNORMAL_CLOSURE(1006),
    INVALID_FRAME_PAYLOAD_DATA(1007),
    POLICY_VIOLATION(1008),
    MESSAGE_TOO_BIG(1009),
    MANDATORY_EXTENSION_MISSING(1010),
    INTERNAL_SERVER_ERROR(1011),
    TLS_HANDSHAKE_FAILURE(1015)
}
