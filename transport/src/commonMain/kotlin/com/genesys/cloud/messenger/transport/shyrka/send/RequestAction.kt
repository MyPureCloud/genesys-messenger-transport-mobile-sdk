package com.genesys.cloud.messenger.transport.shyrka.send

internal enum class RequestAction(val value: String) {
    CONFIGURE_SESSION("configureSession"),
    CONFIGURE_AUTHENTICATED_SESSION("configureAuthenticatedSession"),
    ON_MESSAGE("onMessage"),
    ECHO_MESSAGE("echo"),
    ON_ATTACHMENT("onAttachment"),
    GET_ATTACHMENT("getAttachment"),
    DELETE_ATTACHMENT("deleteAttachment"),
    GET_JWT("getJwt"),
    CLOSE_SESSION("closeSession"),
}
