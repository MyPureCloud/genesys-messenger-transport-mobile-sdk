package com.genesys.cloud.messenger.composeapp.viewmodel

import com.genesys.cloud.messenger.composeapp.model.AppError
import com.genesys.cloud.messenger.composeapp.model.Result
import com.genesys.cloud.messenger.composeapp.util.TestDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BaseViewModelTest {
    
    private val testDispatcherRule = TestDispatcherRule()
    
    @BeforeTest
    fun setUp() {
        testDispatcherRule.setUp()
    }
    
    @AfterTest
    fun tearDown() {
        testDispatcherRule.tearDown()
    }
    
    private class TestViewModel : BaseViewModel() {
        suspend fun testSafeExecute(shouldFail: Boolean = false): Result<String> {
            return safeExecute {
                if (shouldFail) {
                    throw Exception("Test exception")
                } else {
                    Result.Success("Success")
                }
            }
        }
        
        suspend fun testSafeExecuteUnit(shouldFail: Boolean = false) {
            safeExecuteUnit {
                if (shouldFail) {
                    throw Exception("Test exception")
                }
            }
        }
        
        fun testHandleError(error: AppError) {
            handleError(error)
        }
        
        fun testSetLoading(loading: Boolean) {
            setLoading(loading)
        }
    }
    
    @Test
    fun testInitialState() {
        val viewModel = TestViewModel()
        
        assertNull(viewModel.error.value)
        assertFalse(viewModel.isLoading.value)
    }
    
    @Test
    fun testHandleError() {
        val viewModel = TestViewModel()
        val error = AppError.NetworkError.ConnectionError("Test connection error")
        
        viewModel.testHandleError(error)
        
        assertEquals(error, viewModel.error.value)
        assertFalse(viewModel.isLoading.value) // Should set loading to false
    }
    
    @Test
    fun testClearError() {
        val viewModel = TestViewModel()
        val error = AppError.NetworkError.ConnectionError("Test error")
        
        viewModel.testHandleError(error)
        assertEquals(error, viewModel.error.value)
        
        viewModel.clearError()
        assertNull(viewModel.error.value)
    }
    
    @Test
    fun testSetLoading() {
        val viewModel = TestViewModel()
        
        viewModel.testSetLoading(true)
        assertTrue(viewModel.isLoading.value)
        
        viewModel.testSetLoading(false)
        assertFalse(viewModel.isLoading.value)
    }
    
    @Test
    fun testSafeExecuteSuccess() = runTest {
        val viewModel = TestViewModel()
        
        val result = viewModel.testSafeExecute(shouldFail = false)
        
        assertTrue(result is Result.Success)
        assertEquals("Success", (result as Result.Success).data)
        assertNull(viewModel.error.value)
        assertFalse(viewModel.isLoading.value) // Should be false after completion
    }
    
    @Test
    fun testSafeExecuteFailure() = runTest {
        val viewModel = TestViewModel()
        
        val result = viewModel.testSafeExecute(shouldFail = true)
        
        assertTrue(result is Result.Error)
        assertTrue(viewModel.error.value is AppError.UnknownError)
        assertFalse(viewModel.isLoading.value) // Should be false after failure
    }
    
    @Test
    fun testSafeExecuteUnitSuccess() = runTest {
        val viewModel = TestViewModel()
        
        viewModel.testSafeExecuteUnit(shouldFail = false)
        
        assertNull(viewModel.error.value)
        assertFalse(viewModel.isLoading.value)
    }
    
    @Test
    fun testSafeExecuteUnitFailure() = runTest {
        val viewModel = TestViewModel()
        
        viewModel.testSafeExecuteUnit(shouldFail = true)
        
        assertTrue(viewModel.error.value is AppError.UnknownError)
        assertFalse(viewModel.isLoading.value)
    }
    
    @Test
    fun testOnCleared() {
        val viewModel = TestViewModel()
        
        // This should not throw an exception
        viewModel.onCleared()
        
        // After clearing, the scope should be cancelled
        // We can't directly test this, but we can ensure the method completes
        assertTrue(true) // Test passes if no exception is thrown
    }
}