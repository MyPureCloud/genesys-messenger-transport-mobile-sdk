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
        ApiHelper.shared.disconnectExistingConversations()
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

        messengerTester.startNewMessengerConnection()
        messengerTester.sendText(text: "Starting Attachment test.")

        // Use the public API to answer the new Messenger conversation.
        // Send a message from that agent and make sure we receive it.
        guard let conversationInfo = ApiHelper.shared.answerNewConversation() else {
            XCTFail("The message we sent may not have connected to an agent.")
            return
        }

        // Send an attachment from the transport SDK. Ensure it's received
        guard let image = UIImage(named: "image"), let data = image.pngData() as NSData? else {
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
        let newToken = UUID().uuidString
        DefaultTokenStore(storeKey: "com.genesys.cloud.messenger").store(token: newToken)
        print("New token: \(newToken)")

        // Initializing a new MessengerInteractorTester object.
        // Doing this because the global messenger object will still use the cached token for this test.
        let messengerHandler: MessengerInteractorTester?
        do {
            let deployment = try Deployment()
            messengerHandler = MessengerInteractorTester(deployment: deployment)
        } catch {
            XCTFail(error.localizedDescription)
            return
        }
        guard let messengerHandler = messengerHandler else {
            XCTFail("Failed to setup the Messenger tester.")
            return
        }

        // Should be able to answer the conversation immediately after starting the connection if AutoStart is enabled.
        messengerHandler.startNewMessengerConnection()
        guard let conversationInfo = ApiHelper.shared.answerNewConversation() else {
            XCTFail("The message we sent may not have connected to an agent.")
            return
        }

        // Disconnect the conversation for the agent and disconnect the session.
        ApiHelper.shared.sendConnectOrDisconnect(conversationInfo: conversationInfo, connecting: false, wrapup: true)
        messengerHandler.disconnectMessenger()
    }

    func testBotConversation() {
        let deployment = try! Deployment()
        let testController = MessengerInteractorTester(deployment: Deployment(deploymentId: TestConfig.shared.config?.botDeploymentId ?? "", domain: deployment.domain))

        testController.startNewMessengerConnection()
        delay(3, reason: "Allow time for the bot to start.")
        testController.receivedMessageExpectation = XCTestExpectation(description: "Wait for message to be received from the bot.")
        testController.sendText(text: "Yes") // Bot is configured to send a message if a "Yes" is sent.
        testController.waitForMessageReceiveExpectation()
        testController.verifyReceivedMessage(expectedMessage: "Ok! Here's another message.") // Bot is configured to send this text.

        // Disconnect the conversation for the bot and disconnect the session.
        testController.sendText(text: "No") // Bot is configured to disconnect if a "No" is sent.
        testController.disconnectMessenger()
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
            tester.startNewMessengerConnection()
            delay(3)
        }
        let result = XCTWaiter().wait(for: [testers[0].connectionClosed!], timeout: 30)
        XCTAssertEqual(result, .completed, "Did not receive a Connection Closed event.")

        // Cleanup. Also verifies that all of the other clients are still connected due to an error being thrown if we disconnect while not being connected.
        for tester in testers[1..<testers.count] {
            tester.disconnectMessenger()
        }
    }

    func testDisconnectAgent_ReadOnly() {
        // Setup the session. Send a message.
        guard let messengerTester = messengerTester else {
            XCTFail("Failed to setup the Messenger tester.")
            return
        }

        messengerTester.startNewMessengerConnection()
        messengerTester.sendText(text: "Testing from E2E test.")

        // Use the public API to answer the new Messenger conversation.
        guard let conversationInfo = ApiHelper.shared.answerNewConversation() else {
            XCTFail("The message we sent may not have connected to an agent.")
            return
        }

        // Disconnect the conversation for the agent and make sure the client state changes.
        messengerTester.disconnectedSession = XCTestExpectation(description: "Wait for the session to be disconnected.")
        messengerTester.readOnlyStateExpectation = XCTestExpectation(description: "Wait for the client state to be set to read only.")
        ApiHelper.shared.sendConnectOrDisconnect(conversationInfo: conversationInfo, connecting: false, wrapup: true)
        messengerTester.waitForAgentDisconnect()
        messengerTester.waitForReadOnlyState()

        // Disconnect the messenger. When we connect again, check to make sure the state is set to read only.
        messengerTester.disconnectMessenger()
        messengerTester.readOnlyStateExpectation = XCTestExpectation(description: "Wait for the client state to be set to read only.")
        messengerTester.startMessengerConnection() // With the deployment config setup this way, we'll be put into a read only state. Will automatically wait for readOnly state.
        messengerTester.disconnectMessenger()
    }

    func testDisconnectAgent_NotReadOnly() {
        let deployment = try! Deployment()
        let testController = MessengerInteractorTester(deployment: Deployment(deploymentId: TestConfig.shared.config?.agentDisconnectDeploymentId ?? "", domain: deployment.domain))

        testController.startNewMessengerConnection()
        testController.sendText(text: "Testing from E2E test.")

        // Use the public API to answer the new Messenger conversation.
        // Send a message from that agent and make sure we receive it.
        guard let conversationInfo = ApiHelper.shared.answerNewConversation() else {
            XCTFail("The message we sent may not have connected to an agent.")
            return
        }

        // Disconnect the conversation for the agent and make sure the client state changes.
        testController.disconnectedSession = XCTestExpectation(description: "Wait for the session to be disconnected.")
        ApiHelper.shared.sendConnectOrDisconnect(conversationInfo: conversationInfo, connecting: false, wrapup: true)
        testController.waitForAgentDisconnect()
        if testController.currentClientState is MessagingClientState.ReadOnly {
            XCTFail("The client should NOT be in a ReadOnly state.")
            return
        }

        // Connect again. The conversation should match
        testController.sendText(text: "Testing from E2E test.")
        guard let conversationInfo2 = ApiHelper.shared.answerNewConversation() else {
            XCTFail("The message we sent may not have connected to an agent.")
            return
        }
        XCTAssertEqual(conversationInfo.conversationId, conversationInfo2.conversationId, "The reconnect conversation may not have matched.")

        // Cleanup. Wait for the agent disconnect again, because why not.
        testController.disconnectedSession = XCTestExpectation(description: "Wait for the session to be disconnected.")
        ApiHelper.shared.sendConnectOrDisconnect(conversationInfo: conversationInfo2, connecting: false, wrapup: true)
        testController.waitForAgentDisconnect()
        testController.disconnectMessenger()
    }

    func testHistoryPull() {
        guard let messengerTester = messengerTester else {
            XCTFail("Failed to setup the Messenger tester.")
            return
        }

        messengerTester.startNewMessengerConnection()
        messengerTester.sendText(text: "Starting message history test.")

        // Use the public API to answer the new Messenger conversation.
        // Send a message from that agent and make sure we receive it.
        guard let conversationInfo = ApiHelper.shared.answerNewConversation() else {
            XCTFail("The message we sent may not have connected to an agent.")
            return
        }

        // Disconnect the conversation for the agent and make sure the client state changes.
        messengerTester.disconnectedSession = XCTestExpectation(description: "Wait for the session to be disconnected.")
        messengerTester.readOnlyStateExpectation = XCTestExpectation(description: "Wait for the client state to be set to read only.")
        ApiHelper.shared.sendConnectOrDisconnect(conversationInfo: conversationInfo, connecting: false, wrapup: true)
        messengerTester.waitForAgentDisconnect()
        messengerTester.waitForReadOnlyState()

        // Pull messages from the backend.
        let messages = messengerTester.pullHistory()

        // Create a list of Events to check.
        var checkList: [String: (check: Bool, test: (Event) -> Bool)] = [:]
        checkList["ConversationAutostart"] = (check: false, test: { event in
            return event is Event.ConversationAutostart
        })
        checkList["ConversationDisconnect"] = (check: false, test: { event in
            return event is Event.ConversationDisconnect
        })

        // Go through the list of received messages. Ensure that every expected event was pulled.
        for message in messages {
            if let event = message.events.first {
                for testInfo in checkList where !testInfo.value.check {
                    if testInfo.value.test(event) {
                        checkList[testInfo.key]?.check = true
                        break
                    }
                }
            }
        }

        let missingEvents = checkList.filter { !$0.value.check }
        XCTAssertTrue(missingEvents.isEmpty, "The following events were not found in the history: \(missingEvents.keys)")

        // Disconnect the conversation for the agent and disconnect the session.
        messengerTester.disconnectMessenger()
    }

    func testMessageAttributes() {
        // Setup the session. Send a message.
        guard let messengerTester = messengerTester else {
            XCTFail("Failed to setup the Messenger tester.")
            return
        }

        messengerTester.startNewMessengerConnection()
        messengerTester.sendTextWithAttribute(text: "Testing with a specific name.", attributes: ["name": "Jane Doe"])
        guard let conversationInfo = ApiHelper.shared.answerNewConversation() else {
            XCTFail("The message we sent may not have connected to an agent.")
            return
        }

        // Send a message with a new name via custom attributes.
        messengerTester.sendTextWithAttribute(text: "Testing with a new name!", attributes: ["name": "John Doe"])

        // Cleanup.
        ApiHelper.shared.sendConnectOrDisconnect(conversationInfo: conversationInfo, connecting: false, wrapup: true)
        messengerTester.disconnectMessenger()
    }

    func testSendAndReceiveMessage() {
        // Setup the session. Send a message.
        guard let messengerTester = messengerTester else {
            XCTFail("Failed to setup the Messenger tester.")
            return
        }

        messengerTester.startNewMessengerConnection()
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

        messengerTester.startNewMessengerConnection()
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

    func testUnknownAgent() {
        let deployment = try! Deployment()
        let testController = MessengerInteractorTester(deployment: Deployment(deploymentId: TestConfig.shared.config?.humanizeDisableDeploymentId ?? "", domain: deployment.domain))
        testController.humanizeEnabled = false

        testController.startNewMessengerConnection()
        testController.sendText(text: "Testing from E2E test.")

        // Use the public API to answer the new Messenger conversation.
        // Send a message from that agent and make sure we receive it.
        guard let conversationInfo = ApiHelper.shared.answerNewConversation() else {
            XCTFail("The message we sent may not have connected to an agent.")
            return
        }
        let receivedMessageText = "Test message sent via API request!"
        testController.testExpectation = XCTestExpectation(description: "Wait for message to be received from the UI agent.")
        ApiHelper.shared.sendOutboundSmsMessage(conversationId: conversationInfo.conversationId, communicationId: conversationInfo.communicationId, message: receivedMessageText)
        testController.waitForTestExpectation()
        testController.verifyReceivedMessage(expectedMessage: receivedMessageText)

        // Disconnect the conversation for the agent and disconnect the session.
        ApiHelper.shared.sendConnectOrDisconnect(conversationInfo: conversationInfo, connecting: false, wrapup: true)
        testController.disconnectMessenger()
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
