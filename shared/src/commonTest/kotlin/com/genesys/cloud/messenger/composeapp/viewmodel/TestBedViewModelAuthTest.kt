package com.genesys.cloud.messenger.composeapp.viewmodel

import com.genesys.cloud.messenger.composeapp.model.AuthState
import com.genesys.cloud.messenger.composeapp.model.CorrectiveAction
import com.genesys.cloud.messenger.composeapp.model.ErrorCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Comprehensive tests for authentication flow handling in TestBedViewModel.
 * Tests all authentication states, transitions, and error scenarios.
 * 
 * Requirements: 3.4
 */
class TestBedViewModelAuthTest {

    // MARK: - Authentication State Tests

    @Test
    fun testInitialAuthState() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        
        // Then
        assertEquals(AuthState.NoAuth, viewModel.authState, "Initial auth state should be NoAuth")
    }

    @Test
    fun testAuthStateTransition_NoAuthToAuthorized() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        assertEquals(AuthState.NoAuth, viewModel.authState)
        
        // When
        viewModel.command = "authorize VALID_AUTH_CODE_123"
        viewModel.onCommandSend()
        
        // Then
        assertEquals(AuthState.Authorized, viewModel.authState, "Should transition to Authorized state")
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.content.contains("Authorization successful") })
    }

    @Test
    fun testAuthStateTransition_AuthorizedToLoggedOut() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        viewModel.authState = AuthState.Authorized
        
        // When
        viewModel.command = "oktalogout"
        viewModel.onCommandSend()
        
        // Then
        assertEquals(AuthState.LoggedOut, viewModel.authState, "Should transition to LoggedOut state")
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.content.contains("logout") })
    }

    @Test
    fun testAuthStateTransition_AuthorizedToLoggedOutViaTokenRemoval() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        viewModel.authState = AuthState.Authorized
        
        // When
        viewModel.command = "removetoken"
        viewModel.onCommandSend()
        
        // Then
        assertEquals(AuthState.LoggedOut, viewModel.authState, "Should transition to LoggedOut state when token removed")
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.content.contains("Removing access token") })
    }

    @Test
    fun testAuthStateTransition_AuthorizedToLoggedOutViaRefreshTokenRemoval() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        viewModel.authState = AuthState.Authorized
        
        // When
        viewModel.command = "removeauthrefreshtoken"
        viewModel.onCommandSend()
        
        // Then
        assertEquals(AuthState.LoggedOut, viewModel.authState, "Should transition to LoggedOut state when refresh token removed")
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.content.contains("Removing auth refresh token") })
    }

    // MARK: - Okta Sign-In Tests

    @Test
    fun testOktaSignIn_basic() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        
        // When
        viewModel.command = "oktasignin"
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.content.contains("Okta sign-in initiated") })
        assertFalse(viewModel.commandWaiting)
    }

    @Test
    fun testOktaSignIn_withPkce() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        
        // When
        viewModel.command = "oktasigninwithpkce"
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.content.contains("Okta sign-in with PKCE initiated") })
        assertFalse(viewModel.commandWaiting)
    }

    // MARK: - Authorization Code Tests

    @Test
    fun testAuthorizeCommand_validAuthCode() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        val authCode = "VALID_AUTH_CODE_ABC123"
        
        // When
        viewModel.command = "authorize $authCode"
        viewModel.onCommandSend()
        
        // Then
        assertEquals(AuthState.Authorized, viewModel.authState)
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.content.contains("Processing authorization code: ${authCode.take(8)}") })
        assertTrue(messages.any { it.content.contains("Authorization successful") })
        assertFalse(viewModel.commandWaiting)
    }

    @Test
    fun testAuthorizeCommand_emptyAuthCode() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        
        // When
        viewModel.command = "authorize"
        viewModel.onCommandSend()
        
        // Then
        assertEquals(AuthState.NoAuth, viewModel.authState, "Auth state should remain NoAuth")
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.type == "Error" && it.content.contains("Authorization code is required") })
        assertFalse(viewModel.commandWaiting)
    }

    @Test
    fun testAuthorizeCommand_shortAuthCode() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        val shortAuthCode = "ABC"
        
        // When
        viewModel.command = "authorize $shortAuthCode"
        viewModel.onCommandSend()
        
        // Then
        assertEquals(AuthState.Authorized, viewModel.authState) // Still processes short codes
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.content.contains("Processing authorization code: $shortAuthCode") })
        assertFalse(viewModel.commandWaiting)
    }

    @Test
    fun testAuthorizeCommand_longAuthCode() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        val longAuthCode = "VERY_LONG_AUTH_CODE_WITH_MANY_CHARACTERS_123456789"
        
        // When
        viewModel.command = "authorize $longAuthCode"
        viewModel.onCommandSend()
        
        // Then
        assertEquals(AuthState.Authorized, viewModel.authState)
        val messages = viewModel.socketMessages.value
        // Should truncate display to first 8 characters
        assertTrue(messages.any { it.content.contains("Processing authorization code: VERY_LON") })
        assertFalse(viewModel.commandWaiting)
    }

    // MARK: - Authentication Status Commands

    @Test
    fun testWasAuthenticatedCommand_whenNotAuthenticated() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        assertEquals(AuthState.NoAuth, viewModel.authState)
        
        // When
        viewModel.command = "wasauthenticated"
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.content.contains("Authentication status") })
        assertTrue(messages.any { it.content.contains("NoAuth") || it.content.contains("not authenticated") })
        assertFalse(viewModel.commandWaiting)
    }

    @Test
    fun testWasAuthenticatedCommand_whenAuthenticated() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        viewModel.authState = AuthState.Authorized
        
        // When
        viewModel.command = "wasauthenticated"
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.content.contains("Authentication status") })
        assertTrue(messages.any { it.content.contains("Authorized") || it.content.contains("authenticated") })
        assertFalse(viewModel.commandWaiting)
    }

    @Test
    fun testWasAuthenticatedCommand_whenLoggedOut() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        viewModel.authState = AuthState.LoggedOut
        
        // When
        viewModel.command = "wasauthenticated"
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.content.contains("Authentication status") })
        assertTrue(messages.any { it.content.contains("LoggedOut") || it.content.contains("logged out") })
        assertFalse(viewModel.commandWaiting)
    }

    @Test
    fun testShouldAuthorizeCommand() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        
        // When
        viewModel.command = "shouldauthorize"
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.content.contains("Authorization check") })
        assertFalse(viewModel.commandWaiting)
    }

    // MARK: - Step-Up Authentication Tests

    @Test
    fun testStepUpCommand_whenNotAuthenticated() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        assertEquals(AuthState.NoAuth, viewModel.authState)
        
        // When
        viewModel.command = "stepup"
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.content.contains("Step-up authentication") })
        assertFalse(viewModel.commandWaiting)
    }

    @Test
    fun testStepUpCommand_whenAuthenticated() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        viewModel.authState = AuthState.Authorized
        
        // When
        viewModel.command = "stepup"
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.content.contains("Step-up authentication") })
        assertFalse(viewModel.commandWaiting)
    }

    // MARK: - Logout Tests

    @Test
    fun testOktaLogoutCommand_whenAuthenticated() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        viewModel.authState = AuthState.Authorized
        
        // When
        viewModel.command = "oktalogout"
        viewModel.onCommandSend()
        
        // Then
        assertEquals(AuthState.LoggedOut, viewModel.authState)
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.content.contains("Initiating logout from Okta session") })
        assertTrue(messages.any { it.content.contains("Successfully logged out") })
        assertFalse(viewModel.commandWaiting)
    }

    @Test
    fun testOktaLogoutCommand_whenNotAuthenticated() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        assertEquals(AuthState.NoAuth, viewModel.authState)
        
        // When
        viewModel.command = "oktalogout"
        viewModel.onCommandSend()
        
        // Then
        assertEquals(AuthState.LoggedOut, viewModel.authState) // Still transitions to LoggedOut
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.content.contains("logout") })
        assertFalse(viewModel.commandWaiting)
    }

    // MARK: - Token Management Tests

    @Test
    fun testRemoveTokenCommand_whenAuthenticated() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        viewModel.authState = AuthState.Authorized
        
        // When
        viewModel.command = "removetoken"
        viewModel.onCommandSend()
        
        // Then
        assertEquals(AuthState.LoggedOut, viewModel.authState)
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.content.contains("Removing access token") })
        assertTrue(messages.any { it.content.contains("Access token removed successfully") })
        assertFalse(viewModel.commandWaiting)
    }

    @Test
    fun testRemoveTokenCommand_whenNotAuthenticated() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        assertEquals(AuthState.NoAuth, viewModel.authState)
        
        // When
        viewModel.command = "removetoken"
        viewModel.onCommandSend()
        
        // Then
        assertEquals(AuthState.NoAuth, viewModel.authState) // Remains NoAuth when no token to remove
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.content.contains("Removing access token") })
        assertFalse(viewModel.commandWaiting)
    }

    @Test
    fun testRemoveRefreshTokenCommand_whenAuthenticated() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        viewModel.authState = AuthState.Authorized
        
        // When
        viewModel.command = "removeauthrefreshtoken"
        viewModel.onCommandSend()
        
        // Then
        assertEquals(AuthState.LoggedOut, viewModel.authState)
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.content.contains("Removing auth refresh token") })
        assertTrue(messages.any { it.content.contains("Auth refresh token removed successfully") })
        assertFalse(viewModel.commandWaiting)
    }

    @Test
    fun testRemoveRefreshTokenCommand_whenNotAuthenticated() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        assertEquals(AuthState.NoAuth, viewModel.authState)
        
        // When
        viewModel.command = "removeauthrefreshtoken"
        viewModel.onCommandSend()
        
        // Then
        assertEquals(AuthState.NoAuth, viewModel.authState) // Remains NoAuth when no token to remove
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.content.contains("Removing auth refresh token") })
        assertFalse(viewModel.commandWaiting)
    }

    // MARK: - Authentication Flow Integration Tests

    @Test
    fun testCompleteAuthenticationFlow_success() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        
        // When - Complete authentication flow
        
        // Step 1: Initiate Okta sign-in
        viewModel.command = "oktasignin"
        viewModel.onCommandSend()
        assertEquals(AuthState.NoAuth, viewModel.authState) // Still NoAuth until code received
        
        // Step 2: Provide authorization code
        viewModel.command = "authorize AUTH_CODE_FROM_OKTA_123"
        viewModel.onCommandSend()
        assertEquals(AuthState.Authorized, viewModel.authState) // Now authorized
        
        // Step 3: Check authentication status
        viewModel.command = "wasauthenticated"
        viewModel.onCommandSend()
        
        // Step 4: Connect with authentication
        viewModel.command = "connectauthenticated"
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.content.contains("Okta sign-in initiated") })
        assertTrue(messages.any { it.content.contains("Authorization successful") })
        assertTrue(messages.any { it.content.contains("Authentication status") })
        assertTrue(messages.any { it.content.contains("Connect authenticated") })
        assertEquals(AuthState.Authorized, viewModel.authState)
    }

    @Test
    fun testCompleteAuthenticationFlow_withLogout() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        
        // When - Authentication and logout flow
        
        // Step 1: Authorize
        viewModel.command = "authorize AUTH_CODE_123"
        viewModel.onCommandSend()
        assertEquals(AuthState.Authorized, viewModel.authState)
        
        // Step 2: Connect authenticated
        viewModel.command = "connectauthenticated"
        viewModel.onCommandSend()
        
        // Step 3: Logout
        viewModel.command = "oktalogout"
        viewModel.onCommandSend()
        assertEquals(AuthState.LoggedOut, viewModel.authState)
        
        // Step 4: Try to connect authenticated (should still work but with logged out state)
        viewModel.command = "connectauthenticated"
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.content.contains("Authorization successful") })
        assertTrue(messages.any { it.content.contains("Connect authenticated") })
        assertTrue(messages.any { it.content.contains("logout") })
        assertEquals(AuthState.LoggedOut, viewModel.authState)
    }

    @Test
    fun testAuthenticationFlow_withTokenRemoval() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        
        // When - Authentication and token removal flow
        
        // Step 1: Authorize
        viewModel.command = "authorize TOKEN_123"
        viewModel.onCommandSend()
        assertEquals(AuthState.Authorized, viewModel.authState)
        
        // Step 2: Remove access token
        viewModel.command = "removetoken"
        viewModel.onCommandSend()
        assertEquals(AuthState.LoggedOut, viewModel.authState)
        
        // Step 3: Try to authorize again
        viewModel.command = "authorize NEW_TOKEN_456"
        viewModel.onCommandSend()
        assertEquals(AuthState.Authorized, viewModel.authState)
        
        // Step 4: Remove refresh token
        viewModel.command = "removeauthrefreshtoken"
        viewModel.onCommandSend()
        assertEquals(AuthState.LoggedOut, viewModel.authState)
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.content.contains("Authorization successful") })
        assertTrue(messages.any { it.content.contains("Removing access token") })
        assertTrue(messages.any { it.content.contains("Removing auth refresh token") })
    }

    // Note: PlatformContext is an expect class and cannot be mocked in common tests
}