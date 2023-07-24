package com.genesys.cloud.messenger.transport.shyrka.receive

internal enum class MessageClassName(val value: String) {
    STRING_MESSAGE("string"),
    SESSION_RESPONSE("SessionResponse"),
    STRUCTURED_MESSAGE("StructuredMessage"),
    PRESIGNED_URL_RESPONSE("PresignedUrlResponse"),
    ATTACHMENT_DELETED_RESPONSE("AttachmentDeletedResponse"),
    UPLOAD_FAILURE_EVENT("UploadFailureEvent"),
    UPLOAD_SUCCESS_EVENT("UploadSuccessEvent"),
    JWT_RESPONSE("JwtResponse"),
    GENERATE_URL_ERROR("GenerateUrlError"),
    SESSION_EXPIRED_EVENT("SessionExpiredEvent"),
    TOO_MANY_REQUESTS_ERROR_MESSAGE("TooManyRequestsErrorMessage"),
    CONNECTION_CLOSED_EVENT("ConnectionClosedEvent"),
    LOGOUT_EVENT("LogoutEvent"),
    SESSION_CLEARED_EVENT("SessionClearedEvent"),
}
