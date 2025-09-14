import XCTest

/**
 * UI tests for the iOS Compose Multiplatform app.
 * 
 * These tests verify:
 * - App launches and displays correctly
 * - User interactions work as expected
 * - Navigation flows work end-to-end
 * - Shared UI components are accessible and functional
 * 
 * Requirements addressed:
 * - 2.5: End-to-end UI testing of iOS app
 * - 3.5: Testing shared module UI integration with iOS
 */
final class iosComposeAppUITests: XCTestCase {

    var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launch()
    }

    override func tearDownWithError() throws {
        app = nil
    }

    func testAppLaunchesSuccessfully() throws {
        // Test that the app launches and displays the main screen
        XCTAssertTrue(app.staticTexts["Welcome to Messenger"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.buttons["Start Chat"].exists)
        XCTAssertTrue(app.buttons["Settings"].exists)
    }
    
    func testNavigationToChat() throws {
        // Test navigation to chat screen
        let startChatButton = app.buttons["Start Chat"]
        XCTAssertTrue(startChatButton.waitForExistence(timeout: 5))
        startChatButton.tap()
        
        // Verify chat screen is displayed
        XCTAssertTrue(app.staticTexts["Chat"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.textFields["Message input field"].exists)
        XCTAssertTrue(app.buttons["Send message"].exists)
    }
    
    func testNavigationToSettings() throws {
        // Test navigation to settings screen
        let settingsButton = app.buttons["Settings"]
        XCTAssertTrue(settingsButton.waitForExistence(timeout: 5))
        settingsButton.tap()
        
        // Verify settings screen is displayed
        XCTAssertTrue(app.staticTexts["Settings"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["Theme"].exists)
        XCTAssertTrue(app.staticTexts["Notifications"].exists)
        XCTAssertTrue(app.staticTexts["Language"].exists)
    }
    
    func testBackNavigation() throws {
        // Navigate to chat screen
        app.buttons["Start Chat"].tap()
        XCTAssertTrue(app.staticTexts["Chat"].waitForExistence(timeout: 5))
        
        // Navigate back
        let backButton = app.buttons["Navigate back"]
        if backButton.exists {
            backButton.tap()
        } else {
            // Alternative: swipe back gesture
            app.swipeRight()
        }
        
        // Verify we're back on home screen
        XCTAssertTrue(app.staticTexts["Welcome to Messenger"].waitForExistence(timeout: 5))
    }
    
    func testChatInteraction() throws {
        // Navigate to chat screen
        app.buttons["Start Chat"].tap()
        XCTAssertTrue(app.staticTexts["Chat"].waitForExistence(timeout: 5))
        
        // Test message input interaction
        let messageField = app.textFields["Message input field"]
        XCTAssertTrue(messageField.exists)
        messageField.tap()
        
        // Test send button
        let sendButton = app.buttons["Send message"]
        XCTAssertTrue(sendButton.exists)
        
        // Note: In a real test, you would type text and send messages
        // This test verifies the UI elements are accessible
    }
    
    func testSettingsInteraction() throws {
        // Navigate to settings screen
        app.buttons["Settings"].tap()
        XCTAssertTrue(app.staticTexts["Settings"].waitForExistence(timeout: 5))
        
        // Test theme setting interaction
        let themeOption = app.staticTexts["Theme"]
        XCTAssertTrue(themeOption.exists)
        themeOption.tap()
        
        // Test notifications toggle
        let notificationsOption = app.staticTexts["Notifications"]
        XCTAssertTrue(notificationsOption.exists)
        
        // Test language setting
        let languageOption = app.staticTexts["Language"]
        XCTAssertTrue(languageOption.exists)
    }
    
    func testFullNavigationFlow() throws {
        // Test complete navigation flow
        
        // Start at home
        XCTAssertTrue(app.staticTexts["Welcome to Messenger"].waitForExistence(timeout: 5))
        
        // Go to chat
        app.buttons["Start Chat"].tap()
        XCTAssertTrue(app.staticTexts["Chat"].waitForExistence(timeout: 5))
        
        // Go back to home
        if app.buttons["Navigate back"].exists {
            app.buttons["Navigate back"].tap()
        } else {
            app.swipeRight()
        }
        XCTAssertTrue(app.staticTexts["Welcome to Messenger"].waitForExistence(timeout: 5))
        
        // Go to settings
        app.buttons["Settings"].tap()
        XCTAssertTrue(app.staticTexts["Settings"].waitForExistence(timeout: 5))
        
        // Go back to home
        if app.buttons["Navigate back"].exists {
            app.buttons["Navigate back"].tap()
        } else {
            app.swipeRight()
        }
        XCTAssertTrue(app.staticTexts["Welcome to Messenger"].waitForExistence(timeout: 5))
    }
    
    func testSharedUIComponentsAccessibility() throws {
        // Test that shared UI components are accessible on iOS
        
        // Home screen components
        XCTAssertTrue(app.staticTexts["Welcome to Messenger"].exists)
        XCTAssertTrue(app.buttons["Start Chat"].exists)
        XCTAssertTrue(app.buttons["Settings"].exists)
        
        // Chat screen components
        app.buttons["Start Chat"].tap()
        XCTAssertTrue(app.staticTexts["Chat"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.textFields["Message input field"].exists)
        XCTAssertTrue(app.buttons["Send message"].exists)
        
        // Navigate back
        if app.buttons["Navigate back"].exists {
            app.buttons["Navigate back"].tap()
        } else {
            app.swipeRight()
        }
        
        // Settings screen components
        app.buttons["Settings"].tap()
        XCTAssertTrue(app.staticTexts["Settings"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["Theme"].exists)
        XCTAssertTrue(app.staticTexts["Notifications"].exists)
        XCTAssertTrue(app.staticTexts["Language"].exists)
    }
    
    func testAppStateManagement() throws {
        // Test that app state is maintained during navigation
        
        // Navigate to chat
        app.buttons["Start Chat"].tap()
        XCTAssertTrue(app.staticTexts["Chat"].waitForExistence(timeout: 5))
        
        // Interact with message field
        let messageField = app.textFields["Message input field"]
        messageField.tap()
        
        // Navigate away and back
        if app.buttons["Navigate back"].exists {
            app.buttons["Navigate back"].tap()
        } else {
            app.swipeRight()
        }
        XCTAssertTrue(app.staticTexts["Welcome to Messenger"].waitForExistence(timeout: 5))
        
        // Return to chat
        app.buttons["Start Chat"].tap()
        XCTAssertTrue(app.staticTexts["Chat"].waitForExistence(timeout: 5))
        
        // Verify chat screen is still functional
        XCTAssertTrue(app.textFields["Message input field"].exists)
        XCTAssertTrue(app.buttons["Send message"].exists)
    }
    
    func testErrorHandlingUI() throws {
        // Test that error states are displayed correctly
        // Note: This would require triggering error conditions
        // For now, we verify the app doesn't crash during normal operation
        
        app.buttons["Start Chat"].tap()
        XCTAssertTrue(app.staticTexts["Chat"].waitForExistence(timeout: 5))
        
        // Test that the app handles empty message input gracefully
        let sendButton = app.buttons["Send message"]
        sendButton.tap()
        
        // App should still be functional
        XCTAssertTrue(app.textFields["Message input field"].exists)
        XCTAssertTrue(app.buttons["Send message"].exists)
    }
    
    func testLaunchPerformance() throws {
        if #available(macOS 10.15, iOS 13.0, tvOS 13.0, watchOS 7.0, *) {
            // This measures how long it takes to launch your application.
            measure(metrics: [XCTApplicationLaunchMetric()]) {
                XCUIApplication().launch()
            }
        }
    }
}