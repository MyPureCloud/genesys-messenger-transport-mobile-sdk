package com.genesys.cloud.messenger.composeapp.util

import com.genesys.cloud.messenger.composeapp.model.TestBedError
import com.genesys.cloud.messenger.composeapp.model.Result

/**
 * Validator for deployment configuration settings.
 * Provides validation for deployment ID and region settings.
 */
object DeploymentValidator {
    
    // Valid deployment ID pattern: alphanumeric, hyphens, underscores, 8-64 characters
    private val DEPLOYMENT_ID_PATTERN = Regex("^[a-zA-Z0-9_-]{8,64}$")
    
    // Valid region patterns
    private val REGION_PATTERNS = listOf(
        Regex("^[a-z]+-[a-z]+-\\d+$"), // AWS style: us-east-1, eu-west-1
        Regex("^[a-z]+\\d+$"), // Simple style: useast1, euwest1
        Regex("^[a-zA-Z0-9_-]{2,32}$") // Generic pattern
    )
    
    // Common valid regions
    private val COMMON_REGIONS = listOf(
        "us-east-1", "us-west-1", "us-west-2",
        "eu-west-1", "eu-central-1", "eu-north-1",
        "ap-southeast-1", "ap-southeast-2", "ap-northeast-1",
        "ca-central-1", "sa-east-1",
        "useast1", "uswest1", "uswest2",
        "euwest1", "eucentral1", "eunorth1",
        "apsoutheast1", "apsoutheast2", "apnortheast1",
        "cacentral1", "saeast1"
    )
    
    /**
     * Validate deployment ID
     */
    fun validateDeploymentId(deploymentId: String): Result<String> {
        return when {
            deploymentId.isBlank() -> Result.Error(
                TestBedError.DeploymentValidationError.EmptyDeploymentIdError()
            )
            
            !DEPLOYMENT_ID_PATTERN.matches(deploymentId) -> Result.Error(
                TestBedError.DeploymentValidationError.InvalidDeploymentIdError(deploymentId)
            )
            
            else -> Result.Success(deploymentId.trim())
        }
    }
    
    /**
     * Validate region
     */
    fun validateRegion(region: String, availableRegions: List<String> = COMMON_REGIONS): Result<String> {
        val trimmedRegion = region.trim()
        
        return when {
            trimmedRegion.isBlank() -> Result.Error(
                TestBedError.DeploymentValidationError.EmptyRegionError()
            )
            
            // Check if region is in the available regions list
            availableRegions.isNotEmpty() && !availableRegions.contains(trimmedRegion) -> {
                // If not in list, check if it matches valid patterns
                val matchesPattern = REGION_PATTERNS.any { it.matches(trimmedRegion) }
                if (!matchesPattern) {
                    Result.Error(
                        TestBedError.DeploymentValidationError.InvalidRegionError(
                            region = trimmedRegion,
                            availableRegions = availableRegions
                        )
                    )
                } else {
                    Result.Success(trimmedRegion)
                }
            }
            
            // If no available regions provided, just check patterns
            availableRegions.isEmpty() && !REGION_PATTERNS.any { it.matches(trimmedRegion) } -> 
                Result.Error(
                    TestBedError.DeploymentValidationError.InvalidRegionError(
                        region = trimmedRegion,
                        availableRegions = COMMON_REGIONS
                    )
                )
            
            else -> Result.Success(trimmedRegion)
        }
    }
    
    /**
     * Validate both deployment ID and region together
     */
    fun validateDeploymentConfig(
        deploymentId: String, 
        region: String, 
        availableRegions: List<String> = COMMON_REGIONS
    ): Result<Pair<String, String>> {
        val deploymentIdResult = validateDeploymentId(deploymentId)
        val regionResult = validateRegion(region, availableRegions)
        
        return when {
            deploymentIdResult is Result.Error -> deploymentIdResult
            regionResult is Result.Error -> regionResult
            else -> {
                val validDeploymentId = (deploymentIdResult as Result.Success).data
                val validRegion = (regionResult as Result.Success).data
                Result.Success(validDeploymentId to validRegion)
            }
        }
    }
    
    /**
     * Get available regions list
     */
    fun getAvailableRegions(): List<String> = COMMON_REGIONS
    
    /**
     * Check if a deployment ID format is valid (without checking business rules)
     */
    fun isValidDeploymentIdFormat(deploymentId: String): Boolean {
        return deploymentId.isNotBlank() && DEPLOYMENT_ID_PATTERN.matches(deploymentId.trim())
    }
    
    /**
     * Check if a region format is valid (without checking against available regions)
     */
    fun isValidRegionFormat(region: String): Boolean {
        return region.isNotBlank() && REGION_PATTERNS.any { it.matches(region.trim()) }
    }
    
    /**
     * Suggest similar regions if validation fails
     */
    fun suggestSimilarRegions(region: String, availableRegions: List<String> = COMMON_REGIONS): List<String> {
        val normalizedInput = region.lowercase().trim()
        
        return availableRegions.filter { availableRegion ->
            val normalizedAvailable = availableRegion.lowercase()
            
            // Check for partial matches
            normalizedInput.contains(normalizedAvailable.take(2)) || // First 2 chars
            normalizedAvailable.contains(normalizedInput.take(2)) || // First 2 chars reverse
            levenshteinDistance(normalizedInput, normalizedAvailable) <= 2 // Edit distance
        }.take(3) // Limit to 3 suggestions
    }
    
    /**
     * Calculate Levenshtein distance for string similarity
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length
        
        val dp = Array(len1 + 1) { IntArray(len2 + 1) }
        
        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j
        
        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        
        return dp[len1][len2]
    }
    
    /**
     * Validate and normalize deployment configuration
     */
    fun normalizeDeploymentConfig(deploymentId: String, region: String): Result<Pair<String, String>> {
        val normalizedDeploymentId = deploymentId.trim().lowercase()
        val normalizedRegion = region.trim().lowercase()
        
        return validateDeploymentConfig(normalizedDeploymentId, normalizedRegion)
    }
}