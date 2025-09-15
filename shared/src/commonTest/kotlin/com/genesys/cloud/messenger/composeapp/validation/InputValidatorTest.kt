package com.genesys.cloud.messenger.composeapp.validation

import com.genesys.cloud.messenger.composeapp.model.AppError
import com.genesys.cloud.messenger.composeapp.model.Result
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InputValidatorTest {
    
    @Test
    fun testValidateDisplayNameValid() {
        val result = InputValidator.validateDisplayName("John Doe")
        
        assertTrue(result is Result.Success)
        assertEquals("John Doe", (result as Result.Success).data)
    }
    
    @Test
    fun testValidateDisplayNameEmpty() {
        val result = InputValidator.validateDisplayName("")
        
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.ValidationError.EmptyFieldError)
    }
    
    @Test
    fun testValidateDisplayNameTooShort() {
        val result = InputValidator.validateDisplayName("A")
        
        assertTrue(result is Result.Error)
        val error = (result as Result.Error).error
        assertTrue(error is AppError.ValidationError.TooShortError)
        assertEquals(2, (error as AppError.ValidationError.TooShortError).minLength)
    }
    
    @Test
    fun testValidateDisplayNameTooLong() {
        val longName = "a".repeat(51)
        val result = InputValidator.validateDisplayName(longName)
        
        assertTrue(result is Result.Error)
        val error = (result as Result.Error).error
        assertTrue(error is AppError.ValidationError.TooLongError)
        assertEquals(50, (error as AppError.ValidationError.TooLongError).maxLength)
    }
    
    @Test
    fun testValidateDisplayNameInvalidFormat() {
        val result = InputValidator.validateDisplayName("John@Doe#")
        
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.ValidationError.InvalidFormatError)
    }
    
    @Test
    fun testValidateEmailValid() {
        val result = InputValidator.validateEmail("test@example.com")
        
        assertTrue(result is Result.Success)
        assertEquals("test@example.com", (result as Result.Success).data)
    }
    
    @Test
    fun testValidateEmailEmpty() {
        val result = InputValidator.validateEmail("")
        
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.ValidationError.EmptyFieldError)
    }
    
    @Test
    fun testValidateEmailInvalidFormat() {
        val result = InputValidator.validateEmail("invalid-email")
        
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.ValidationError.InvalidFormatError)
    }
    
    @Test
    fun testValidateEmailTooLong() {
        val longEmail = "a".repeat(250) + "@example.com"
        val result = InputValidator.validateEmail(longEmail)
        
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.ValidationError.TooLongError)
    }
    
    @Test
    fun testValidatePhoneNumberValid() {
        val result = InputValidator.validatePhoneNumber("+1 (555) 123-4567")
        
        assertTrue(result is Result.Success)
        assertEquals("+1 (555) 123-4567", (result as Result.Success).data)
    }
    
    @Test
    fun testValidatePhoneNumberEmpty() {
        val result = InputValidator.validatePhoneNumber("")
        
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.ValidationError.EmptyFieldError)
    }
    
    @Test
    fun testValidatePhoneNumberTooShort() {
        val result = InputValidator.validatePhoneNumber("123")
        
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.ValidationError.TooShortError)
    }
    
    @Test
    fun testValidatePhoneNumberTooLong() {
        val result = InputValidator.validatePhoneNumber("1234567890123456")
        
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.ValidationError.TooLongError)
    }
    
    @Test
    fun testValidatePhoneNumberInvalidFormat() {
        val result = InputValidator.validatePhoneNumber("abc-def-ghij")
        
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.ValidationError.InvalidFormatError)
    }
    
    @Test
    fun testValidateTextFieldDefault() {
        val result = InputValidator.validateTextField("Test value", "Test Field")
        
        assertTrue(result is Result.Success)
        assertEquals("Test value", (result as Result.Success).data)
    }
    
    @Test
    fun testValidateTextFieldCustomMinLength() {
        val result = InputValidator.validateTextField("Hi", "Test Field", minLength = 5)
        
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.ValidationError.TooShortError)
    }
    
    @Test
    fun testValidateTextFieldCustomMaxLength() {
        val result = InputValidator.validateTextField("This is too long", "Test Field", maxLength = 10)
        
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.ValidationError.TooLongError)
    }
    
    @Test
    fun testValidateTextFieldAllowEmpty() {
        val result = InputValidator.validateTextField("", "Test Field", allowEmpty = true)
        
        assertTrue(result is Result.Success)
        assertEquals("", (result as Result.Success).data)
    }
    
    @Test
    fun testValidateTextFieldCustomValidator() {
        val customValidator = { value: String -> value.startsWith("prefix_") }
        val result = InputValidator.validateTextField("invalid", "Test Field", customValidator = customValidator)
        
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.ValidationError.InvalidFormatError)
    }
    
    @Test
    fun testValidateFieldsMultiple() {
        val errors = validateFields(
            { InputValidator.validateDeploymentId("") },
            { InputValidator.validateEmail("invalid") },
            { InputValidator.validateDisplayName("Valid Name") }
        )
        
        assertEquals(2, errors.size)
        assertTrue(errors[0] is AppError.ValidationError.EmptyFieldError)
        assertTrue(errors[1] is AppError.ValidationError.InvalidFormatError)
    }
    
    @Test
    fun testValidateFieldsAllValid() {
        val errors = validateFields(
            { InputValidator.validateDeploymentId("12345678-1234-1234-1234-123456789abc") },
            { InputValidator.validateEmail("test@example.com") },
            { InputValidator.validateDisplayName("Valid Name") }
        )
        
        assertTrue(errors.isEmpty())
    }
    
    @Test
    fun testFieldValidationStateDefaults() {
        val state = FieldValidationState()
        
        assertEquals("", state.value)
        assertNull(state.error)
        assertTrue(state.isValid)
    }
    
    @Test
    fun testFieldValidationStateWithValue() {
        val state = FieldValidationState().withValue("test")
        
        assertEquals("test", state.value)
        assertNull(state.error)
        assertTrue(state.isValid)
    }
    
    @Test
    fun testFieldValidationStateWithError() {
        val error = AppError.ValidationError.EmptyFieldError("Test")
        val state = FieldValidationState().withError(error)
        
        assertEquals(error, state.error)
        assertFalse(state.isValid)
    }
    
    @Test
    fun testFieldValidationStateWithValidationSuccess() {
        val validation = Result.Success("validated value")
        val state = FieldValidationState().withValidation(validation)
        
        assertEquals("validated value", state.value)
        assertNull(state.error)
        assertTrue(state.isValid)
    }
    
    @Test
    fun testFieldValidationStateWithValidationError() {
        val error = AppError.ValidationError.EmptyFieldError("Test")
        val validation = Result.Error(error)
        val state = FieldValidationState().withValidation(validation)
        
        assertEquals(error, state.error)
        assertFalse(state.isValid)
    }
}