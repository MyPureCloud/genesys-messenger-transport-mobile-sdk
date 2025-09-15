package com.genesys.cloud.messenger.composeapp.util

import com.genesys.cloud.messenger.composeapp.model.TestBedError
import com.genesys.cloud.messenger.composeapp.model.Result
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class DeploymentValidatorTest {
    
    @Test
    fun `validateDeploymentId should accept valid deployment IDs`() {
        val validIds = listOf(
            "test-deployment-123",
            "deployment_456",
            "DEPLOYMENT789",
            "a1b2c3d4e5f6g7h8", // 16 chars
            "a".repeat(64) // 64 chars (max)
        )
        
        validIds.forEach { id ->
            val result = DeploymentValidator.validateDeploymentId(id)
            assertTrue(result is Result.Success, "Expected success for ID: $id")
            assertEquals(id, (result as Result.Success).data)
        }
    }
    
    @Test
    fun `validateDeploymentId should reject invalid deployment IDs`() {
        val invalidIds = listOf(
            "", // empty
            "   ", // blank
            "short", // too short (< 8 chars)
            "a".repeat(65), // too long (> 64 chars)
            "invalid@deployment", // invalid character
            "deployment with spaces",
            "deployment.with.dots"
        )
        
        invalidIds.forEach { id ->
            val result = DeploymentValidator.validateDeploymentId(id)
            assertTrue(result is Result.Error, "Expected error for ID: $id")
            assertTrue((result as Result.Error).error is TestBedError.DeploymentValidationError)
        }
    }
    
    @Test
    fun `validateDeploymentId should handle empty and blank strings`() {
        val emptyResult = DeploymentValidator.validateDeploymentId("")
        assertTrue(emptyResult is Result.Error)
        assertTrue((emptyResult as Result.Error).error is TestBedError.DeploymentValidationError.EmptyDeploymentIdError)
        
        val blankResult = DeploymentValidator.validateDeploymentId("   ")
        assertTrue(blankResult is Result.Error)
        assertTrue((blankResult as Result.Error).error is TestBedError.DeploymentValidationError.EmptyDeploymentIdError)
    }
    
    @Test
    fun `validateRegion should accept valid regions`() {
        val validRegions = listOf(
            "us-east-1",
            "eu-west-1",
            "ap-southeast-2",
            "ca-central-1",
            "useast1",
            "euwest1"
        )
        
        validRegions.forEach { region ->
            val result = DeploymentValidator.validateRegion(region)
            assertTrue(result is Result.Success, "Expected success for region: $region")
            assertEquals(region, (result as Result.Success).data)
        }
    }
    
    @Test
    fun `validateRegion should reject invalid regions when available regions provided`() {
        val availableRegions = listOf("us-east-1", "eu-west-1", "ap-southeast-1")
        
        val result = DeploymentValidator.validateRegion("invalid@region!", availableRegions)
        assertTrue(result is Result.Error)
        val error = (result as Result.Error).error
        assertTrue(error is TestBedError.DeploymentValidationError.InvalidRegionError)
        assertEquals("invalid@region!", (error as TestBedError.DeploymentValidationError.InvalidRegionError).region)
        assertEquals(availableRegions, error.availableRegions)
    }
    
    @Test
    fun `validateRegion should accept valid pattern even if not in available regions`() {
        val availableRegions = listOf("us-east-1", "eu-west-1")
        
        val result = DeploymentValidator.validateRegion("ap-southeast-1", availableRegions)
        assertTrue(result is Result.Success)
        assertEquals("ap-southeast-1", (result as Result.Success).data)
    }
    
    @Test
    fun `validateRegion should handle empty and blank strings`() {
        val emptyResult = DeploymentValidator.validateRegion("")
        assertTrue(emptyResult is Result.Error)
        assertTrue((emptyResult as Result.Error).error is TestBedError.DeploymentValidationError.EmptyRegionError)
        
        val blankResult = DeploymentValidator.validateRegion("   ")
        assertTrue(blankResult is Result.Error)
        assertTrue((blankResult as Result.Error).error is TestBedError.DeploymentValidationError.EmptyRegionError)
    }
    
    @Test
    fun `validateDeploymentConfig should validate both fields together`() {
        val result = DeploymentValidator.validateDeploymentConfig(
            deploymentId = "valid-deployment-123",
            region = "us-east-1"
        )
        
        assertTrue(result is Result.Success)
        val (deploymentId, region) = (result as Result.Success).data
        assertEquals("valid-deployment-123", deploymentId)
        assertEquals("us-east-1", region)
    }
    
    @Test
    fun `validateDeploymentConfig should fail if deployment ID is invalid`() {
        val result = DeploymentValidator.validateDeploymentConfig(
            deploymentId = "short", // too short
            region = "us-east-1"
        )
        
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is TestBedError.DeploymentValidationError.InvalidDeploymentIdError)
    }
    
    @Test
    fun `validateDeploymentConfig should fail if region is invalid`() {
        val result = DeploymentValidator.validateDeploymentConfig(
            deploymentId = "valid-deployment-123",
            region = "" // empty
        )
        
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is TestBedError.DeploymentValidationError.EmptyRegionError)
    }
    
    @Test
    fun `isValidDeploymentIdFormat should check format without business rules`() {
        assertTrue(DeploymentValidator.isValidDeploymentIdFormat("valid-deployment-123"))
        assertTrue(DeploymentValidator.isValidDeploymentIdFormat("deployment_456"))
        assertTrue(DeploymentValidator.isValidDeploymentIdFormat("DEPLOYMENT789"))
        
        assertFalse(DeploymentValidator.isValidDeploymentIdFormat(""))
        assertFalse(DeploymentValidator.isValidDeploymentIdFormat("short"))
        assertFalse(DeploymentValidator.isValidDeploymentIdFormat("invalid@deployment"))
    }
    
    @Test
    fun `isValidRegionFormat should check format without business rules`() {
        assertTrue(DeploymentValidator.isValidRegionFormat("us-east-1"))
        assertTrue(DeploymentValidator.isValidRegionFormat("eu-west-1"))
        assertTrue(DeploymentValidator.isValidRegionFormat("useast1"))
        assertTrue(DeploymentValidator.isValidRegionFormat("custom-region-123"))
        
        assertFalse(DeploymentValidator.isValidRegionFormat(""))
        assertFalse(DeploymentValidator.isValidRegionFormat("   "))
    }
    
    @Test
    fun `suggestSimilarRegions should provide helpful suggestions`() {
        val availableRegions = listOf("us-east-1", "us-west-1", "eu-west-1", "ap-southeast-1")
        
        val suggestions = DeploymentValidator.suggestSimilarRegions("us-east", availableRegions)
        assertTrue(suggestions.contains("us-east-1"))
        assertTrue(suggestions.contains("us-west-1"))
        
        val euSuggestions = DeploymentValidator.suggestSimilarRegions("eu", availableRegions)
        assertTrue(euSuggestions.contains("eu-west-1"))
    }
    
    @Test
    fun `suggestSimilarRegions should limit suggestions to 3`() {
        val availableRegions = listOf(
            "us-east-1", "us-east-2", "us-west-1", "us-west-2", 
            "eu-west-1", "eu-central-1", "ap-southeast-1"
        )
        
        val suggestions = DeploymentValidator.suggestSimilarRegions("us", availableRegions)
        assertTrue(suggestions.size <= 3)
    }
    
    @Test
    fun `normalizeDeploymentConfig should normalize and validate`() {
        val result = DeploymentValidator.normalizeDeploymentConfig(
            deploymentId = "  VALID-DEPLOYMENT-123  ",
            region = "  US-EAST-1  "
        )
        
        assertTrue(result is Result.Success)
        val (deploymentId, region) = (result as Result.Success).data
        assertEquals("valid-deployment-123", deploymentId)
        assertEquals("us-east-1", region)
    }
    
    @Test
    fun `getAvailableRegions should return common regions`() {
        val regions = DeploymentValidator.getAvailableRegions()
        
        assertTrue(regions.isNotEmpty())
        assertTrue(regions.contains("us-east-1"))
        assertTrue(regions.contains("eu-west-1"))
        assertTrue(regions.contains("ap-southeast-1"))
    }
}