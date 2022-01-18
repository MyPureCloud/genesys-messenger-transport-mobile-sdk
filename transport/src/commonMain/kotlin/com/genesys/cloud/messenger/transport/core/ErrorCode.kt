package com.genesys.cloud.messenger.transport.core

/**
 * List of all error codes used to report transport errors.
 */
sealed class ErrorCode(val code: Int) {
    object FeatureUnavailable : ErrorCode(4000)
    object FileTypeInvalid : ErrorCode(4001)
    object FileSizeInvalid : ErrorCode(4002)
    object FileContentInvalid : ErrorCode(4003)
    object FileNameInvalid : ErrorCode(4004)
    object FileNameTooLong : ErrorCode(4005)
    object SessionHasExpired : ErrorCode(4006)
    object SessionNotFound : ErrorCode(4007)
    object AttachmentHasExpired : ErrorCode(4008)
    object AttachmentNotFound : ErrorCode(4009)
    object AttachmentNotSuccessfullyUploaded : ErrorCode(4010)
    object MessageTooLong : ErrorCode(4011)
    object MissingParameter : ErrorCode(4020)
    object RequestRateTooHigh : ErrorCode(4029)
    object UnexpectedError : ErrorCode(5000)
    object WebsocketError : ErrorCode(1001)
    data class RedirectResponseError(val value: Int) : ErrorCode(value)
    data class ClientResponseError(val value: Int) : ErrorCode(value)
    data class ServerResponseError(val value: Int) : ErrorCode(value)

    internal companion object {
        fun mapFrom(value: Int): ErrorCode {
            return when (value) {
                1001 -> WebsocketError
                4000 -> FeatureUnavailable
                4001 -> FileTypeInvalid
                4002 -> FileSizeInvalid
                4003 -> FileContentInvalid
                4004 -> FileNameInvalid
                4005 -> FileNameTooLong
                4006 -> SessionHasExpired
                4007 -> SessionNotFound
                4008 -> AttachmentHasExpired
                4009 -> AttachmentNotFound
                4010 -> AttachmentNotSuccessfullyUploaded
                4011 -> MessageTooLong
                4020 -> MissingParameter
                in 300..399 -> RedirectResponseError(value)
                in 400..499 -> ClientResponseError(value)
                in 500..599 -> ServerResponseError(value)
                else -> UnexpectedError
            }
        }
    }
}
