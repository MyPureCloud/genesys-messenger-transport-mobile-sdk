package com.genesys.cloud.messenger.composeapp.viewmodel

import com.genesys.cloud.messenger.composeapp.model.AuthState
import com.genesys.cloud.messenger.composeapp.model.MessagingClientState
import com.genesys.cloud.messenger.composeapp.model.PlatformContext
import com.genesys.cloud.messenger.composeapp.model.SavedAttachment
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Comprehensive tests for deployment configuration handling in TestBedViewModel.
 * Tests deployment settings validation, updates, and reinitialization behavior.
 * 
 * Requirements: 3.1, 3.4
 */
class TestBedViewModelDeploymentTest {

    // Note: PlatformContext is an expect class and cannot be mocked in common tests

    // MARK: - Initial Deployment Settings Tests

    @Test
    fun testInitialDeploymentSettings() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        
        // Then
        assertEquals("", viewModel.deploymentId, "Initial deployment ID should be empty")
        assertEquals("", viewModel.region, "Initial region should be empty")
    }

    @Test
    fun testDeploymentIdChange() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        val newDeploymentId = "test-deployment-123"
        
        // When
        viewModel.onDeploymentIdChanged(newDeploymentId)
        
        // Then
        assertEquals(newDeploymentId, viewModel.deploymentId, "Deployment ID should be updated")
    }

    @Test
    fun testRegionChange() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        val newRegion = "us-west-2"
        
        // When
        viewModel.onRegionChanged(newRegion)
        
        // Then
        assertEquals(newRegion, viewModel.region, "Region should be updated")
    }

    @Test
    fun testMultipleDeploymentChanges() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        
        // When
        viewModel.onDeploymentIdChanged("deployment-1")
        viewModel.onRegionChanged("us-east-1")
        
        viewModel.onDeploymentIdChanged("deployment-2")
        viewModel.onRegionChanged("eu-west-1")
        
        // Then
        assertEquals("deployment-2", viewModel.deploymentId, "Should have latest deployment ID")
        assertEquals("eu-west-1", viewModel.region, "Should have latest region")
    }

    // MARK: - Deployment Settings Update Tests

    @Test
    fun testUpdateDeploymentSettings_validSettings() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        val deploymentId = "valid-deployment-456"
        val region = "us-east-1"
        
        // When
        viewModel.updateDeploymentSettings(deploymentId, region)
        
        // Then
        assertEquals(deploymentId, viewModel.deploymentId, "Deployment ID should be updated")
        assertEquals(region, viewModel.region, "Region should be updated")
        
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.content.contains("Validating deployment settings") }, "Should have validation message")
        assertTrue(messages.any { it.type == "Success" && it.content.contains("validated successfully") }, "Should have success message")
    }

    @Test
    fun testUpdateDeploymentSettings_emptyDeploymentId() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        val deploymentId = ""
        val region = "us-east-1"
        
        // When
        viewModel.updateDeploymentSettings(deploymentId, region)
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.content.contains("Validating deployment settings") }, "Should have validation message")
        // Note: Actual validation behavior depends on DeploymentValidator implementation
    }

    @Test
    fun testUpdateDeploymentSettings_emptyRegion() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        val deploymentId = "test-deployment"
        val region = ""
        
        // When
        viewModel.updateDeploymentSettings(deploymentId, region)
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.content.contains("Validating deployment settings") }, "Should have validation message")
    }

    @Test
    fun testUpdateDeploymentSettings_bothEmpty() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        val deploymentId = ""
        val region = ""
        
        // When
        viewModel.updateDeploymentSettings(deploymentId, region)
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.content.contains("Validating deployment settings") }, "Should have validation message")
    }

    @Test
    fun testUpdateDeploymentSettings_invalidRegion() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        val deploymentId = "test-deployment"
        val region = "invalid-region-xyz"
        
        // When
        viewModel.updateDeploymentSettings(deploymentId, region)
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.content.contains("Validating deployment settings") }, "Should have validation message")
        // Note: Specific validation error handling depends on DeploymentValidator
    }

    // MARK: - Reinitialization Tests

    // Note: Reinitialization tests that require PlatformContext are skipped in common tests
    // These would be implemented in platform-specific test modules

    @Test
    fun testUpdateDeploymentSettings_notInitialized_noReinitialization() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        viewModel.deploymentId = "original-deployment"
        viewModel.region = "us-east-1"
        
        // Don't initialize the view model
        assertFalse(viewModel.isInitialized, "Should not be initialized")
        
        // When - Update settings
        viewModel.updateDeploymentSettings("new-deployment", "us-west-2")
        
        // Then
        assertEquals("new-deployment", viewModel.deploymentId, "Deployment ID should be updated")
        assertEquals("us-west-2", viewModel.region, "Region should be updated")
        
        val messages = viewModel.socketMessages.value
        assertFalse(messages.any { it.content.contains("reinitialization required") }, "Should not indicate reinitialization needed when not initialized")
    }

    // MARK: - Deployment Command Tests

    @Test
    fun testDeploymentCommand_showsCurrentConfiguration() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        viewModel.deploymentId = "display-deployment-789"
        viewModel.region = "eu-central-1"
        
        // When
        viewModel.command = "deployment"
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.type == "Command Result" && it.content.contains("Deployment Configuration") }, "Should show deployment configuration")
        
        // Should contain deployment details in the raw message
        val deploymentMessage = messages.find { it.content.contains("Deployment Configuration") }
        assertTrue(deploymentMessage?.rawMessage?.contains("display-deployment-789") == true, "Should contain deployment ID")
        assertTrue(deploymentMessage?.rawMessage?.contains("eu-central-1") == true, "Should contain region")
        
        assertFalse(viewModel.commandWaiting, "Command waiting should be false")
    }

    @Test
    fun testDeploymentCommand_emptyConfiguration() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        // Leave deployment settings empty
        
        // When
        viewModel.command = "deployment"
        viewModel.onCommandSend()
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.type == "Command Result" && it.content.contains("Deployment Configuration") }, "Should show deployment configuration")
        
        assertFalse(viewModel.commandWaiting, "Command waiting should be false")
    }

    // MARK: - Default Values Tests

    // Note: Initialization tests that require PlatformContext are skipped in common tests
    // These would be implemented in platform-specific test modules

    // MARK: - Integration Tests

    // Note: Integration tests that require PlatformContext are skipped in common tests
    // These would be implemented in platform-specific test modules

    @Test
    fun testDeploymentValidation_withSuggestions() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        
        // When - Update with potentially invalid region
        viewModel.updateDeploymentSettings("test-deployment", "us-east-invalid")
        
        // Then
        val messages = viewModel.socketMessages.value
        assertTrue(messages.any { it.content.contains("Validating deployment settings") }, "Should validate settings")
        
        // Note: Specific suggestion behavior depends on DeploymentValidator implementation
        // The test verifies that validation is triggered
    }

    @Test
    fun testMultipleDeploymentUpdates() = runTest {
        // Given
        val viewModel = TestBedViewModel()
        
        // When - Multiple rapid updates
        viewModel.updateDeploymentSettings("deployment-1", "us-east-1")
        viewModel.updateDeploymentSettings("deployment-2", "us-west-1")
        viewModel.updateDeploymentSettings("deployment-3", "eu-west-1")
        
        // Then
        assertEquals("deployment-3", viewModel.deploymentId, "Should have final deployment ID")
        assertEquals("eu-west-1", viewModel.region, "Should have final region")
        
        val messages = viewModel.socketMessages.value
        // Should have validation messages for each update
        val validationMessages = messages.filter { it.content.contains("Validating deployment settings") }
        assertEquals(3, validationMessages.size, "Should have validation message for each update")
    }
}