import XCTest

let app = XCUIApplication()

class appNameUITests: XCTestCase {

    override func setUp() {
        continueAfterFailure = false
        app.activate()
    }

    override func tearDown() {
        // If the test fails, print off the UI element tree and terminate the app.
        if (testRun?.totalFailureCount ?? 1) >= 1 {
            print(app.debugDescription)
            app.terminate()
        }
    }

    func testAllCommands() {
        enterCommand(command: "configure", expectedResponse: "<WebMessaging client is not connected.>")
        enterCommand(command: "connect", expectedResponse: "<connected>")
        enterCommand(command: "healthCheck", expectedResponse: "<WebMessaging client is not configured.>")
        enterCommand(command: "send Test Message", expectedResponse: "<WebMessaging client is not configured.>")
        enterCommand(command: "bye", expectedResponse: "Socket <closed>. reason: <disconnected> , code: <1000>")
    }

    func enterCommand(command: String, expectedResponse: String) {
        let textField = app.textFields["Text-Field"]
        textField.tap()
        sleep(2)
        textField.typeText("\(command)\n")
        let result = app.staticTexts["\(expectedResponse)"].waitForExistence(timeout: 60)
        XCTAssertTrue(result, "The expected response never showed up.")
    }
}
