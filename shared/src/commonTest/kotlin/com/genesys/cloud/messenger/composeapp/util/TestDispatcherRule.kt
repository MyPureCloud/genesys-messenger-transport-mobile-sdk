package com.genesys.cloud.messenger.composeapp.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * Test utility to set up test dispatchers for coroutine testing
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TestDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) {
    
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }
    
    fun tearDown() {
        Dispatchers.resetMain()
    }
}