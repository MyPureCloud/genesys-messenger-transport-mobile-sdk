package com.genesys.cloud.messenger.composeapp.viewmodel

import com.genesys.cloud.messenger.composeapp.model.Screen
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
class HomeViewModelTest {
    
    private val testDispatcherRule = TestDispatcherRule()
    
    @BeforeTest
    fun setUp() {
        testDispatcherRule.setUp()
    }
    
    @AfterTest
    fun tearDown() {
        testDispatcherRule.tearDown()
    }
    
    @Test
    fun testInitialState() {
        val viewModel = HomeViewModel()
        
        assertNull(viewModel.navigationEvent.value)
        assertNull(viewModel.error.value)
        assertFalse(viewModel.isLoading.value)
    }
    
    @Test
    fun testNavigateToInteraction() = runTest {
        val viewModel = HomeViewModel()
        
        viewModel.navigateToInteraction()
        
        val navigationEvent = viewModel.navigationEvent.value
        assertTrue(navigationEvent is NavigationEvent.NavigateToScreen)
        assertEquals(Screen.Interaction, (navigationEvent as NavigationEvent.NavigateToScreen).screen)
        
        // Should not have any errors
        assertNull(viewModel.error.value)
    }
    
    @Test
    fun testNavigateToSettings() = runTest {
        val viewModel = HomeViewModel()
        
        viewModel.navigateToSettings()
        
        val navigationEvent = viewModel.navigationEvent.value
        assertTrue(navigationEvent is NavigationEvent.NavigateToScreen)
        assertEquals(Screen.Settings, (navigationEvent as NavigationEvent.NavigateToScreen).screen)
        
        // Should not have any errors
        assertNull(viewModel.error.value)
    }
    
    @Test
    fun testClearNavigationEvent() = runTest {
        val viewModel = HomeViewModel()
        
        // First navigate somewhere
        viewModel.navigateToInteraction()
        assertTrue(viewModel.navigationEvent.value is NavigationEvent.NavigateToScreen)
        
        // Then clear the event
        viewModel.clearNavigationEvent()
        assertNull(viewModel.navigationEvent.value)
    }
    
    @Test
    fun testMultipleNavigationEvents() = runTest {
        val viewModel = HomeViewModel()
        
        // Navigate to interaction
        viewModel.navigateToInteraction()
        val interactionEvent = viewModel.navigationEvent.value
        assertTrue(interactionEvent is NavigationEvent.NavigateToScreen)
        assertEquals(Screen.Interaction, (interactionEvent as NavigationEvent.NavigateToScreen).screen)
        
        // Navigate to settings (should replace previous event)
        viewModel.navigateToSettings()
        val settingsEvent = viewModel.navigationEvent.value
        assertTrue(settingsEvent is NavigationEvent.NavigateToScreen)
        assertEquals(Screen.Settings, (settingsEvent as NavigationEvent.NavigateToScreen).screen)
    }
    
    @Test
    fun testNavigationEventTypes() {
        // Test the NavigationEvent sealed class
        val interactionEvent = NavigationEvent.NavigateToScreen(Screen.Interaction)
        val settingsEvent = NavigationEvent.NavigateToScreen(Screen.Settings)
        val homeEvent = NavigationEvent.NavigateToScreen(Screen.Home)
        
        assertTrue(interactionEvent is NavigationEvent.NavigateToScreen)
        assertTrue(settingsEvent is NavigationEvent.NavigateToScreen)
        assertTrue(homeEvent is NavigationEvent.NavigateToScreen)
        
        assertEquals(Screen.Interaction, interactionEvent.screen)
        assertEquals(Screen.Settings, settingsEvent.screen)
        assertEquals(Screen.Home, homeEvent.screen)
    }
}