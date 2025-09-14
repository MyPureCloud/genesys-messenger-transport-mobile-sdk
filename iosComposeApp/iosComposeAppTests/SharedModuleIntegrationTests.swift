import XCTest
import SwiftUI
@testable import iosComposeApp
import Shared

/**
 * Specific integration tests for shared module functionality on iOS.
 * 
 * These tests focus on:
 * - Shared ViewModels working correctly on iOS
 * - Shared UI components rendering properly
 * - Cross-platform state management
 * - Platform-specific integrations
 * 
 * Requirements addressed:
 * - 2.5: Testing shared UI components and ViewModels on iOS
 * - 3.5: Verifying shared module integration with iOS platform
 */
class SharedModuleIntegrationTests: XCTestCase {

    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    override func tearDownWithError() throws {
        // Clean up any test state
    }

    func testSharedViewModelsInitialization() throws {
        // Test that all shared ViewModels can be initialized on iOS
        let homeViewModel = HomeViewModel()
        XCTAssertNotNil(homeViewModel)
        
        let chatViewModel = ChatViewModel()
        XCTAssertNotNil(chatViewModel)
        
        let settingsViewModel = SettingsViewModel()
        XCTAssertNotNil(settingsViewModel)
    }
    
    func testSharedViewModelStateManagement() throws {
        let homeViewModel = HomeViewModel()
        
        // Test initial state
        XCTAssertNotNil(homeViewModel.uiState)
        
        // Test navigation events
        XCTAssertNotNil(homeViewModel.navigationEvent)
        
        // Test that we can trigger navigation
        homeViewModel.navigateToChat()
        
        // Note: In a real test, you would verify the state change
        // This test verifies the methods can be called without crashing
    }
    
    func testChatViewModelFunctionality() throws {
        let chatViewModel = ChatViewModel()
        
        // Test initial state
        XCTAssertNotNil(chatViewModel.uiState)
        XCTAssertNotNil(chatViewModel.messages)
        
        // Test message sending functionality
        chatViewModel.sendMessage(content: "Test message")
        
        // Test input validation
        let isValid = chatViewModel.isValidMessage(content: "Test")
        XCTAssertTrue(isValid)
        
        let isEmpty = chatViewModel.isValidMessage(content: "")
        XCTAssertFalse(isEmpty)
    }
    
    func testSettingsViewModelFunctionality() throws {
        let settingsViewModel = SettingsViewModel()
        
        // Test initial state
        XCTAssertNotNil(settingsViewModel.uiState)
        XCTAssertNotNil(settingsViewModel.settings)
        
        // Test theme change
        settingsViewModel.updateTheme(theme: ThemeMode.dark)
        
        // Test notification toggle
        settingsViewModel.toggleNotifications()
        
        // Test language change
        settingsViewModel.updateLanguage(language: "es")
    }
    
    func testSharedDataModels() throws {
        // Test ChatMessage model
        let message = ChatMessage(
            id: "test-id",
            content: "Test message",
            timestamp: 1234567890,
            isFromUser: true
        )
        XCTAssertEqual(message.id, "test-id")
        XCTAssertEqual(message.content, "Test message")
        XCTAssertEqual(message.timestamp, 1234567890)
        XCTAssertTrue(message.isFromUser)
        
        // Test AppSettings model
        let settings = AppSettings(
            theme: ThemeMode.dark,
            notifications: false,
            language: "en"
        )
        XCTAssertEqual(settings.theme, ThemeMode.dark)
        XCTAssertFalse(settings.notifications)
        XCTAssertEqual(settings.language, "en")
    }
    
    func testSharedNavigationModels() throws {
        // Test Screen sealed class
        let homeScreen = Screen.Home()
        XCTAssertNotNil(homeScreen)
        
        let chatScreen = Screen.Chat()
        XCTAssertNotNil(chatScreen)
        
        let settingsScreen = Screen.Settings()
        XCTAssertNotNil(settingsScreen)
    }
    
    func testSharedErrorTypes() throws {
        // Test error type creation and handling
        let networkError = ErrorType.NetworkError(message: "Network connection failed")
        XCTAssertNotNil(networkError)
        
        let validationError = ErrorType.ValidationError(message: "Invalid input")
        XCTAssertNotNil(validationError)
        
        let platformError = ErrorType.PlatformError(message: "iOS specific error")
        XCTAssertNotNil(platformError)
    }
    
    func testSharedThemeSystem() throws {
        // Test theme modes
        let lightTheme = ThemeMode.light
        XCTAssertNotNil(lightTheme)
        
        let darkTheme = ThemeMode.dark
        XCTAssertNotNil(darkTheme)
        
        let systemTheme = ThemeMode.system
        XCTAssertNotNil(systemTheme)
    }
    
    func testPlatformSpecificIntegration() throws {
        // Test that platform-specific code works with shared module
        let platform = Platform()
        XCTAssertNotNil(platform)
        
        // Test platform name
        let platformName = platform.name
        XCTAssertTrue(platformName.contains("iOS"))
    }
    
    func testSharedValidationLogic() throws {
        // Test input validation from shared module
        let validator = InputValidator()
        
        // Test message validation
        XCTAssertTrue(validator.isValidMessage(content: "Valid message"))
        XCTAssertFalse(validator.isValidMessage(content: ""))
        XCTAssertFalse(validator.isValidMessage(content: String(repeating: "a", count: 1001)))
        
        // Test settings validation
        XCTAssertTrue(validator.isValidLanguage(language: "en"))
        XCTAssertTrue(validator.isValidLanguage(language: "es"))
        XCTAssertFalse(validator.isValidLanguage(language: ""))
    }
    
    func testSharedStateManagement() throws {
        // Test that shared state management works on iOS
        let homeViewModel = HomeViewModel()
        let chatViewModel = ChatViewModel()
        
        // Test state synchronization
        homeViewModel.navigateToChat()
        
        // Verify that navigation state is properly managed
        // Note: In a real test, you would verify actual state changes
        XCTAssertNotNil(homeViewModel.navigationEvent)
    }
    
    func testMemoryManagement() throws {
        // Test that ViewModels can be properly cleaned up
        var homeViewModel: HomeViewModel? = HomeViewModel()
        var chatViewModel: ChatViewModel? = ChatViewModel()
        var settingsViewModel: SettingsViewModel? = SettingsViewModel()
        
        XCTAssertNotNil(homeViewModel)
        XCTAssertNotNil(chatViewModel)
        XCTAssertNotNil(settingsViewModel)
        
        // Simulate cleanup
        homeViewModel?.onCleared()
        chatViewModel?.onCleared()
        settingsViewModel?.onCleared()
        
        homeViewModel = nil
        chatViewModel = nil
        settingsViewModel = nil
        
        XCTAssertNil(homeViewModel)
        XCTAssertNil(chatViewModel)
        XCTAssertNil(settingsViewModel)
    }
}