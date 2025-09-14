package com.genesys.cloud.messenger.composeapp.validation

import com.genesys.cloud.messenger.composeapp.model.AppError
import com.genesys.cloud.messenger.composeapp.model.Result

/**
 * Input validation utilities for user interactions.
 * 
 * This object provides comprehensive validation rules for different types of user input
 * commonly used in messaging applications. All validation methods return a Result<T>
 * type for type-safe error handling.
 * 
 * Features:
 * - Type-safe validation with Result wrapper
 * - Comprehensive error messages with AppError types
 * - Cross-platform compatibility
 * - Security-focused validation (XSS prevention)
 * - Configurable validation rules
 * 
 * Validation Types:
 * - Chat messages: Length, content, and security validation
 * - Display names: Format and length validation
 * - Email addresses: Format and length validation
 * - Phone numbers: Format and length validation
 * - Language codes: Availability validation
 * - Generic text fields: Customizable validation
 * 
 * Security Considerations:
 * - Prevents XSS attacks by checking for script tags
 * - Validates against potentially harmful patterns
 * - Sanitizes input while preserving user intent
 * 
 * Performance Optimizations:
 * - Efficient regex patterns
 * - Early validation exit for common cases
 * - Minimal string operations
 * - Reusable validation logic
 * 
 * Usage Example:
 * ```kotlin
 * val result = InputValidator.validateChatMessage(userInput)
 * when (result) {
 *     is Result.Success -> handleValidMessage(result.data)
 *     is Result.Error -> showError(result.error)
 * }
 * ```
 */
object InputValidator {
    
    /**
     * Validates a chat message input
     */
    fun validateChatMessage(message: String): Result<String> {
        val trimmedMessage = message.trim()
        
        return when {
            trimmedMessage.isEmpty() -> Result.Error(
                AppError.ValidationError.EmptyFieldError("Message")
            )
            trimmedMessage.length > MAX_MESSAGE_LENGTH -> Result.Error(
                AppError.ValidationError.TooLongError(
                    fieldName = "Message",
                    maxLength = MAX_MESSAGE_LENGTH
                )
            )
            containsOnlyWhitespace(trimmedMessage) -> Result.Error(
                AppError.ValidationError.EmptyFieldError("Message")
            )
            containsInvalidCharacters(trimmedMessage) -> Result.Error(
                AppError.ValidationError.InvalidCharactersError("Message")
            )
            else -> Result.Success(trimmedMessage)
        }
    }
    
    /**
     * Validates language code selection
     */
    fun validateLanguageCode(languageCode: String, availableCodes: List<String>): Result<String> {
        return when {
            languageCode.isEmpty() -> Result.Error(
                AppError.ValidationError.EmptyFieldError("Language")
            )
            !availableCodes.contains(languageCode) -> Result.Error(
                AppError.ValidationError.InvalidFormatError(
                    fieldName = "Language",
                    message = "Selected language is not supported"
                )
            )
            else -> Result.Success(languageCode)
        }
    }
    
    /**
     * Validates user display name or username
     */
    fun validateDisplayName(name: String): Result<String> {
        val trimmedName = name.trim()
        
        return when {
            trimmedName.isEmpty() -> Result.Error(
                AppError.ValidationError.EmptyFieldError("Display name")
            )
            trimmedName.length < MIN_DISPLAY_NAME_LENGTH -> Result.Error(
                AppError.ValidationError.TooShortError(
                    fieldName = "Display name",
                    minLength = MIN_DISPLAY_NAME_LENGTH
                )
            )
            trimmedName.length > MAX_DISPLAY_NAME_LENGTH -> Result.Error(
                AppError.ValidationError.TooLongError(
                    fieldName = "Display name",
                    maxLength = MAX_DISPLAY_NAME_LENGTH
                )
            )
            !isValidDisplayNameFormat(trimmedName) -> Result.Error(
                AppError.ValidationError.InvalidFormatError(
                    fieldName = "Display name",
                    message = "Display name can only contain letters, numbers, spaces, and basic punctuation"
                )
            )
            else -> Result.Success(trimmedName)
        }
    }
    
    /**
     * Validates email format (if needed for user identification)
     */
    fun validateEmail(email: String): Result<String> {
        val trimmedEmail = email.trim().lowercase()
        
        return when {
            trimmedEmail.isEmpty() -> Result.Error(
                AppError.ValidationError.EmptyFieldError("Email")
            )
            !isValidEmailFormat(trimmedEmail) -> Result.Error(
                AppError.ValidationError.InvalidFormatError(
                    fieldName = "Email",
                    message = "Please enter a valid email address"
                )
            )
            trimmedEmail.length > MAX_EMAIL_LENGTH -> Result.Error(
                AppError.ValidationError.TooLongError(
                    fieldName = "Email",
                    maxLength = MAX_EMAIL_LENGTH
                )
            )
            else -> Result.Success(trimmedEmail)
        }
    }
    
    /**
     * Validates phone number format (if needed for user identification)
     */
    fun validatePhoneNumber(phoneNumber: String): Result<String> {
        val cleanedNumber = phoneNumber.replace(Regex("[\\s\\-\\(\\)\\+]"), "")
        
        return when {
            phoneNumber.trim().isEmpty() -> Result.Error(
                AppError.ValidationError.EmptyFieldError("Phone number")
            )
            !isValidPhoneNumberFormat(cleanedNumber) -> Result.Error(
                AppError.ValidationError.InvalidFormatError(
                    fieldName = "Phone number",
                    message = "Please enter a valid phone number"
                )
            )
            cleanedNumber.length < MIN_PHONE_LENGTH -> Result.Error(
                AppError.ValidationError.TooShortError(
                    fieldName = "Phone number",
                    minLength = MIN_PHONE_LENGTH
                )
            )
            cleanedNumber.length > MAX_PHONE_LENGTH -> Result.Error(
                AppError.ValidationError.TooLongError(
                    fieldName = "Phone number",
                    maxLength = MAX_PHONE_LENGTH
                )
            )
            else -> Result.Success(phoneNumber.trim())
        }
    }
    
    /**
     * Validates a generic text field with custom rules
     */
    fun validateTextField(
        value: String,
        fieldName: String,
        minLength: Int = 0,
        maxLength: Int = Int.MAX_VALUE,
        allowEmpty: Boolean = false,
        customValidator: ((String) -> Boolean)? = null
    ): Result<String> {
        val trimmedValue = value.trim()
        
        return when {
            !allowEmpty && trimmedValue.isEmpty() -> Result.Error(
                AppError.ValidationError.EmptyFieldError(fieldName)
            )
            trimmedValue.length < minLength -> Result.Error(
                AppError.ValidationError.TooShortError(
                    fieldName = fieldName,
                    minLength = minLength
                )
            )
            trimmedValue.length > maxLength -> Result.Error(
                AppError.ValidationError.TooLongError(
                    fieldName = fieldName,
                    maxLength = maxLength
                )
            )
            customValidator != null && !customValidator(trimmedValue) -> Result.Error(
                AppError.ValidationError.InvalidFormatError(
                    fieldName = fieldName,
                    message = "$fieldName has invalid format"
                )
            )
            else -> Result.Success(if (allowEmpty && trimmedValue.isEmpty()) value else trimmedValue)
        }
    }
    
    // Private helper functions
    
    private fun containsOnlyWhitespace(text: String): Boolean {
        return text.all { it.isWhitespace() }
    }
    
    private fun containsInvalidCharacters(text: String): Boolean {
        // Check for potentially harmful characters or patterns
        val invalidPatterns = listOf(
            "<script",
            "javascript:",
            "data:text/html",
            "vbscript:",
            "onload=",
            "onerror="
        )
        
        val lowerText = text.lowercase()
        return invalidPatterns.any { pattern -> lowerText.contains(pattern) }
    }
    
    private fun isValidDisplayNameFormat(name: String): Boolean {
        // Allow letters, numbers, spaces, and basic punctuation
        val validPattern = Regex("^[a-zA-Z0-9\\s\\-_\\.,']+$")
        return validPattern.matches(name)
    }
    
    private fun isValidEmailFormat(email: String): Boolean {
        // Basic email validation pattern
        val emailPattern = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
        return emailPattern.matches(email)
    }
    
    private fun isValidPhoneNumberFormat(phoneNumber: String): Boolean {
        // Allow only digits, should be reasonable length
        return phoneNumber.all { it.isDigit() }
    }
    
    // Constants
    private const val MAX_MESSAGE_LENGTH = 4000
    private const val MIN_DISPLAY_NAME_LENGTH = 2
    private const val MAX_DISPLAY_NAME_LENGTH = 50
    private const val MAX_EMAIL_LENGTH = 254
    private const val MIN_PHONE_LENGTH = 7
    private const val MAX_PHONE_LENGTH = 15
}

/**
 * Extension function to validate multiple fields and collect all errors
 */
fun validateFields(vararg validations: () -> Result<*>): List<AppError> {
    return validations.mapNotNull { validation ->
        when (val result = validation()) {
            is Result.Error -> result.error
            is Result.Success -> null
        }
    }
}

/**
 * Data class to hold validation state for a field
 */
data class FieldValidationState(
    val value: String = "",
    val error: AppError? = null,
    val isValid: Boolean = error == null
) {
    fun withValue(newValue: String): FieldValidationState {
        return copy(value = newValue, error = null, isValid = true)
    }
    
    fun withError(newError: AppError): FieldValidationState {
        return copy(error = newError, isValid = false)
    }
    
    fun withValidation(validation: Result<String>): FieldValidationState {
        return when (validation) {
            is Result.Success -> copy(value = validation.data, error = null, isValid = true)
            is Result.Error -> copy(error = validation.error, isValid = false)
        }
    }
}