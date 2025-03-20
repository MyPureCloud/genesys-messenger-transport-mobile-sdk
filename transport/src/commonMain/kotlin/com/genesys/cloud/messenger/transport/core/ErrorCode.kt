package com.genesys.cloud.messenger.transport.core

import com.genesys.cloud.messenger.transport.shyrka.receive.PushErrorResponse
import io.ktor.http.HttpStatusCode

/**
 * List of all error codes used to report transport errors.
 */
sealed class ErrorCode(val code: Int) {
    data object FeatureUnavailable : ErrorCode(4000)
    data object FileTypeInvalid : ErrorCode(4001)
    data object FileSizeInvalid : ErrorCode(4002)
    data object FileContentInvalid : ErrorCode(4003)
    data object FileNameInvalid : ErrorCode(4004)
    data object FileNameTooLong : ErrorCode(4005)
    data object SessionHasExpired : ErrorCode(4006)
    data object SessionNotFound : ErrorCode(4007)
    data object AttachmentHasExpired : ErrorCode(4008)
    data object AttachmentNotFound : ErrorCode(4009)
    data object AttachmentNotSuccessfullyUploaded : ErrorCode(4010)
    data object MessageTooLong : ErrorCode(4011)
    data object CustomAttributeSizeTooLarge : ErrorCode(4013)
    data object CannotDowngradeToUnauthenticated : ErrorCode(4017)
    data object MissingParameter : ErrorCode(4020)
    data object RequestRateTooHigh : ErrorCode(4029)
    data object UnexpectedError : ErrorCode(5000)
    data object WebsocketError : ErrorCode(1001)
    data object WebsocketAccessDenied : ErrorCode(1002)
    data object NetworkDisabled : ErrorCode(-1009)
    data object CancellationError : ErrorCode(6000)
    data object AuthFailed : ErrorCode(6001)
    data object AuthLogoutFailed : ErrorCode(6002)
    data object RefreshAuthTokenFailure : ErrorCode(6003)
    data object HistoryFetchFailure : ErrorCode(6004)
    data object ClearConversationFailure : ErrorCode(6005)
    // Push
    data object DeviceTokenOperationFailure : ErrorCode(6020)
    data object DeviceAlreadyRegistered : ErrorCode(6021)
    data object DeviceNotFound : ErrorCode(6022)
    data object ContactStitchingError : ErrorCode(6023)
    data object DeviceRegistrationFailure : ErrorCode(6024)
    data object DeviceUpdateFailure : ErrorCode(6025)
    data object DeviceDeleteFailure : ErrorCode(6026)

    data class RedirectResponseError(val value: Int) : ErrorCode(value)
    data class ClientResponseError(val value: Int) : ErrorCode(value)
    data class ServerResponseError(val value: Int) : ErrorCode(value)

    internal companion object {
        fun mapFrom(value: Int): ErrorCode {
            return when (value) {
                1001 -> WebsocketError
                1002 -> WebsocketAccessDenied
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
                4013 -> CustomAttributeSizeTooLarge
                4017 -> CannotDowngradeToUnauthenticated
                4020 -> MissingParameter
                4029 -> RequestRateTooHigh
                6000 -> CancellationError
                6001 -> AuthFailed
                6002 -> AuthLogoutFailed
                6003 -> RefreshAuthTokenFailure
                6004 -> HistoryFetchFailure
                6005 -> ClearConversationFailure
                6020 -> DeviceTokenOperationFailure
                6021 -> DeviceAlreadyRegistered
                6022 -> DeviceNotFound
                6023 -> ContactStitchingError
                6024 -> DeviceRegistrationFailure
                6025 -> DeviceUpdateFailure
                6026 -> DeviceDeleteFailure
                in 300..399 -> RedirectResponseError(value)
                in 400..499 -> ClientResponseError(value)
                in 500..599 -> ServerResponseError(value)
                -1009 -> NetworkDisabled
                else -> UnexpectedError
            }
        }
    }
}

object ErrorMessage {
    const val FailedToReconnect = "Failed to reconnect."
    const val FailedToConfigureSession = "Failed to configure session."
    const val InternetConnectionIsOffline =
        "Network is disabled. Please enable wifi or cellular and try again."
    const val AutoRefreshTokenDisabled = "AutoRefreshTokenWhenExpired is disabled in Configuration."
    const val NoRefreshToken = "No refreshAuthToken. Authentication is required."
    const val FailedToClearConversation = "Failed to clear conversation."
    const val FileSizeIsToSmall = "Attachment size cannot be less than 1 byte"
    const val FileAttachmentIsDisabled = "File attachment is disabled in Deployment Configuration."
    fun detachFailed(attachmentId: String) = "Detach failed: Invalid attachment ID ($attachmentId)"
    const val INVALID_DEVICE_TOKEN = "DeviceToken can not be empty."
    const val INVALID_PUSH_PROVIDER = "PushProvider can not be null."
    fun fileSizeIsTooBig(maxFileSize: Long?) = "Reduce the attachment size to $maxFileSize KB or less."
    fun fileTypeIsProhibited(fileName: String) = "File type  $fileName is prohibited for upload."
    fun customAttributesSizeError(maxSize: Int) = "Error: Custom attributes exceed allowed max size of $maxSize bytes."
}

sealed class CorrectiveAction(val message: String) {
    object BadRequest : CorrectiveAction("Refer to the error details.")
    object Forbidden :
        CorrectiveAction("Access was refused. Check that your deploymentId, feature toggles, and configuration are correct.")

    object NotFound :
        CorrectiveAction("An object referenced in the request was not found, pass an id that exists.")

    object RequestTimeOut :
        CorrectiveAction("Was unable to fulfil the request in time. You can retry later.")

    object TooManyRequests : CorrectiveAction("Retry later.")
    object Unknown : CorrectiveAction("Action unknown.")
    object ReAuthenticate : CorrectiveAction("User re-authentication is required.")
    object CustomAttributeSizeTooLarge : CorrectiveAction("Shorten the custom attributes.")

    override fun toString(): String {
        return message
    }
}

internal fun ErrorCode.toCorrectiveAction(): CorrectiveAction = when (this.code) {
    400 -> CorrectiveAction.BadRequest
    403 -> CorrectiveAction.Forbidden
    404 -> CorrectiveAction.NotFound
    408 -> CorrectiveAction.RequestTimeOut
    429 -> CorrectiveAction.TooManyRequests
    4013 -> CorrectiveAction.CustomAttributeSizeTooLarge
    ErrorCode.AuthFailed.code,
    ErrorCode.AuthLogoutFailed.code,
    ErrorCode.RefreshAuthTokenFailure.code,
    -> CorrectiveAction.ReAuthenticate
    else -> CorrectiveAction.Unknown
}

internal fun ErrorCode.isUnauthorized(): Boolean =
    this.code == HttpStatusCode.Unauthorized.value

internal fun PushErrorResponse.toErrorCode(): ErrorCode = when (code) {
    "device.not.found" -> ErrorCode.DeviceNotFound
    "device.registration.failure" -> ErrorCode.DeviceRegistrationFailure
    "device.update.failure" -> ErrorCode.DeviceUpdateFailure
    "device.delete.failure" -> ErrorCode.DeviceDeleteFailure
    "device.already.registered" -> ErrorCode.DeviceAlreadyRegistered
    "contacts.stitching.error" -> ErrorCode.ContactStitchingError
    "feature.toggle.disabled" -> ErrorCode.FeatureUnavailable
    "too.many.requests.retry.after" -> ErrorCode.RequestRateTooHigh
    "required.fields.missing", "update.fields.missing" -> ErrorCode.MissingParameter
    else -> ErrorCode.DeviceTokenOperationFailure
}
