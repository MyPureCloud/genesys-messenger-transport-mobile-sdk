package com.genesys.cloud.messenger.composeapp.model

/**
 * Represents the authentication state in the TestBed application.
 * Based on the Android TestBedViewModel reference implementation.
 */
sealed class AuthState {
    /**
     * No authentication has been performed
     */
    object NoAuth : AuthState()
    
    /**
     * Authentication code has been received from OAuth flow
     * @param authCode The authorization code received
     */
    data class AuthCodeReceived(val authCode: String) : AuthState()
    
    /**
     * User is successfully authorized
     */
    object Authorized : AuthState()
    
    /**
     * User has been logged out
     */
    object LoggedOut : AuthState()
    
    /**
     * An error occurred during authentication
     * @param errorCode The specific error code
     * @param message Human-readable error message
     * @param correctiveAction Suggested action to resolve the error
     */
    data class Error(
        val errorCode: ErrorCode,
        val message: String?,
        val correctiveAction: CorrectiveAction
    ) : AuthState()
}

/**
 * Error codes for authentication failures
 */
enum class ErrorCode {
    NETWORK_ERROR,
    INVALID_CREDENTIALS,
    TOKEN_EXPIRED,
    AUTHORIZATION_FAILED,
    UNKNOWN_ERROR
}

/**
 * Suggested corrective actions for authentication errors
 */
enum class CorrectiveAction {
    RETRY,
    RE_AUTHENTICATE,
    CHECK_NETWORK,
    CONTACT_SUPPORT,
    NONE
}