//
//  iosAppTests.swift
//  iosAppTests
//
//  Created by Morehouse, Matthew on 6/1/22.
//

import XCTest
import MessengerTransport
@testable import iosApp

class iosAppTests: XCTestCase {

    var messengerTester: MessengerInteractorTester?

    override class func setUp() {
        super.setUp()
        ApiHelper.shared.disconnectExistingConversations()
    }

    override func setUp() {
        super.setUp()
        continueAfterFailure = false
        if messengerTester == nil {
            do {
                let deployment = try Deployment()
                messengerTester = MessengerInteractorTester(deployment: deployment)
            } catch {
                XCTFail("Failed to initialize MessengerInteractorTester: \(error.localizedDescription)")
            }
        }
    }

    override func tearDown() {
        super.tearDown()
    }

    func testAttachments() {
        // Setup the session. Send a message to start the conversation.
        // Setup the session. Send a message.
        guard let messengerTester = messengerTester else {
            XCTFail("Failed to setup the Messenger tester.")
            return
        }

        messengerTester.startMessengerConnection()
        messengerTester.sendText(text: "Testing from E2E test.")

        // Use the public API to answer the new Messenger conversation.
        // Send a message from that agent and make sure we receive it.
        guard let conversationInfo = ApiHelper.shared.answerNewConversation() else {
            XCTFail("The message we sent may not have connected to an agent.")
            return
        }

        // Send an attachment from the transport SDK. Ensure it's received
        guard let image = TestConfig.shared.pullTestPng(), let data = image.pngData() as NSData? else {
            XCTFail("Failed to pull the test image.")
            return
        }
        let swiftByteArray: [UInt8] = data.toByteArray()
        let intArray : [Int8] = swiftByteArray
            .map { Int8(bitPattern: $0) }
        let kotlinByteArray: KotlinByteArray = KotlinByteArray.init(size: Int32(swiftByteArray.count))
        for (index, element) in intArray.enumerated() {
            kotlinByteArray.set(index: Int32(index), value: element)
        }
        messengerTester.attemptImageAttach(kotlinByteArray: kotlinByteArray)
        messengerTester.sendUploadedImage()

        // Disconnect the conversation for the agent and disconnect the session.
        ApiHelper.shared.sendConnectOrDisconnect(conversationInfo: conversationInfo, connecting: false, wrapup: true)
        messengerTester.disconnectMessenger()
    }

    // Test will always fail if the deployment configuration doesn't have AutoStart enabled. See Deployment Configuration options in Admin.
    func testAutoStart() {
        guard let deploymentConfig = messengerTester?.pullDeploymentConfig() else {
            XCTFail("Failed to pull the deployment configuration.")
            return
        }
        XCTAssertTrue(deploymentConfig.messenger.apps.conversations.autoStart.enabled, "AutoStart was not enabled for this deployment config.")

        // Save a new token.
        DefaultTokenStore(storeKey: "com.genesys.cloud.messenger").store(token: UUID().uuidString)

        guard let messengerTester = messengerTester else {
            XCTFail("Failed to setup the Messenger tester.")
            return
        }

        // Should be able to answer the conversation immediately after starting the connection if AutoStart is enabled.
        messengerTester.startMessengerConnection()
        guard let conversationInfo = ApiHelper.shared.answerNewConversation() else {
            XCTFail("The message we sent may not have connected to an agent.")
            return
        }

        // Disconnect the conversation for the agent and disconnect the session.
        ApiHelper.shared.sendConnectOrDisconnect(conversationInfo: conversationInfo, connecting: false, wrapup: true)
        messengerTester.disconnectMessenger()
    }

    func testConnectionClosed() {
        // Pull the deployment for use later.
        var deployment: Deployment?
        do {
            deployment = try Deployment()
        } catch {
            XCTFail("\(error.localizedDescription)")
        }
        guard let deployment = deployment else {
            XCTFail("Failed to pull the deployment config.")
            return
        }

        // Create 4 new MessengerInteractorTesters. We'll use these to trigger a connection closed event.
        // Right now, there's a max number of 3 open sesssions that use the same token.
        // We'll open 4 and confirm that an error occurs in the fourth attempt on the oldest client.
        var testers = [MessengerInteractorTester]()
        for _ in 1...4 {
            testers.append(MessengerInteractorTester(deployment: deployment))
        }
        testers[0].connectionClosed = XCTestExpectation(description: "Wait for the ConnectionClosedEvent")
        for tester in testers {
            tester.startMessengerConnection()
            delay(3)
        }
        let result = XCTWaiter().wait(for: [testers[0].connectionClosed!], timeout: 30)
        XCTAssertEqual(result, .completed, "Did not receive a Connection Closed event.")

        // Cleanup. Also verifies that all of the other clients are still connected due to an error being thrown if we disconnect while not being connected.
        for tester in testers[1..<testers.count] {
            tester.disconnectMessenger()
        }
    }

    func testMessageAttributes() {
        // Setup the session. Send a message.
        guard let messengerTester = messengerTester else {
            XCTFail("Failed to setup the Messenger tester.")
            return
        }

        messengerTester.startMessengerConnection()
        messengerTester.sendTextWithAttribute(text: "Testing with a specific name.", attributes: ["name": "Jane Doe"])
        guard let conversationInfo = ApiHelper.shared.answerNewConversation() else {
            XCTFail("The message we sent may not have connected to an agent.")
            return
        }

        // Send a message with a new name via custom attributes.
        messengerTester.sendTextWithAttribute(text: "Testing with a new name!", attributes: ["name": "John Doe"])

        // Cleanup.
        ApiHelper.shared.sendConnectOrDisconnect(conversationInfo: conversationInfo, connecting: false, wrapup: true)
    }

    func testSendAndReceiveMessage() {
        // Setup the session. Send a message.
        guard let messengerTester = messengerTester else {
            XCTFail("Failed to setup the Messenger tester.")
            return
        }

        messengerTester.startMessengerConnection()
        messengerTester.sendText(text: "Testing from E2E test.")

        // Use the public API to answer the new Messenger conversation.
        // Send a message from that agent and make sure we receive it.
        guard let conversationInfo = ApiHelper.shared.answerNewConversation() else {
            XCTFail("The message we sent may not have connected to an agent.")
            return
        }
        let receivedMessageText = "Test message sent via API request!"
        messengerTester.testExpectation = XCTestExpectation(description: "Wait for message to be received from the UI agent.")
        ApiHelper.shared.sendOutboundSmsMessage(conversationId: conversationInfo.conversationId, communicationId: conversationInfo.communicationId, message: receivedMessageText)
        messengerTester.waitForTestExpectation()
        messengerTester.verifyReceivedMessage(expectedMessage: receivedMessageText)

        // Disconnect the conversation for the agent and disconnect the session.
        ApiHelper.shared.sendConnectOrDisconnect(conversationInfo: conversationInfo, connecting: false, wrapup: true)
        messengerTester.disconnectMessenger()
    }

    func testTypingIndicators() {
        // Setup the session. Send a message.
        guard let messengerTester = messengerTester else {
            XCTFail("Failed to setup the Messenger tester.")
            return

        }

        messengerTester.startMessengerConnection()
        messengerTester.sendText(text: "Testing from E2E test.")

        // Use the public API to answer the new Messenger conversation.
        // Send a message from that agent and make sure we receive it.
        guard let conversationInfo = ApiHelper.shared.answerNewConversation() else {
            XCTFail("The message we sent may not have connected to an agent.")
            return
        }

        // Send a typing indicator.
        messengerTester.indicateTyping()

        // Receive a typing indicator from the agent.
        messengerTester.testExpectation = XCTestExpectation(description: "Wait for a typing indicator from the agent.")
        ApiHelper.shared.sendTypingIndicator(conversationId: conversationInfo.conversationId, communicationId: conversationInfo.communicationId)
        messengerTester.waitForTestExpectation()

        // Disconnect the conversation for the agent and disconnect the session.
        ApiHelper.shared.sendConnectOrDisconnect(conversationInfo: conversationInfo, connecting: false, wrapup: true)
        messengerTester.disconnectMessenger()
    }
    
    func testAccessDenied() {
        let deployment = try! Deployment()
        let tester = MessengerInteractorTester(deployment: Deployment(deploymentId: "InvalidDeploymentId", domain: deployment.domain))
                 
        tester.startMessengerConnectionWithErrorExpectation(XCTestExpectation(description: "Expecting access denied."))
        
        if let state = tester.messenger.messagingClient.currentState as? MessagingClientState.Error {
            XCTAssertTrue(state.code is ErrorCode.WebsocketAccessDenied)
        } else {
            XCTFail("Expected Error state, but instead state is: \(tester.messenger.messagingClient.currentState)")
        }
    }

}

private func delay(_ seconds: Double = 1.0, reason: String? = nil) {
    if let reason = reason {
        print("Reason for delay is: \(reason)")
    }
    let expectation = XCTestExpectation(description: "Test delay for: \(reason ?? "")")
    DispatchQueue.main.asyncAfter(deadline: .now() + seconds) {
        expectation.fulfill()
    }
    _ = XCTWaiter.wait(for: [expectation], timeout: seconds + 5)
}
