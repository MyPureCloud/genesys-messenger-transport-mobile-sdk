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
    
    // Editing the name of this test to ensure that this test runs first (alphabetically).
    // The auth token has a limited amount of time that it can be used.
    // Additionally, until we can pull a new auth code from the test, we will only be using this test for checks involved with an active auth session.
    func testAAA_Auth_Pass_With_MultiUser() {
        guard let config = TestConfig.shared.config else {
            XCTFail("Failed to pull the test config.")
            return
        }
        let deployment = try! Deployment()
        var testController = MessengerInteractorTester(deployment: Deployment(deploymentId: config.authDeploymentId, domain: deployment.domain))
        testController.authorize(config: config, authCode: config.authCode)
        testController.startNewMessengerConnection(authorized: true)
        testController.sendText(text: "Testing from E2E test.")
        
        // Use the public API to answer the new Messenger conversation.
        // Verify that we can send and receive new messages.
        guard let conversationInfo1 = ApiHelper.shared.answerNewConversation() else {
            XCTFail("The message we sent may not have connected to an agent.")
            return
        }
        let receivedMessageText = "Test message sent via API request!"
        testController.testExpectation = XCTestExpectation(description: "Wait for message to be received from the UI agent.")
        ApiHelper.shared.sendOutboundSmsMessage(conversationId: conversationInfo1.conversationId, communicationId: conversationInfo1.communicationId, message: receivedMessageText)
        testController.waitForTestExpectation()
        testController.verifyReceivedMessage(expectedMessage: receivedMessageText)
        
        // Disconnect. Ensure that auth logs out correctly.
        testController.authLogout()
        
        // Temporary fix for MTSDK-222.
        let newToken = UUID().uuidString
        testController.messenger.tokenVault.store(key: "token", value: newToken)
        print("New token: \(newToken)")
        testController = MessengerInteractorTester(deployment: Deployment(deploymentId: config.authDeploymentId, domain: deployment.domain))
        
        // With the same test controller, authenticate with a different user's auth code.
        // Ensure that we can answer a new conversation.
        testController.authorize(config: config, authCode: config.authCode2)
        testController.startNewMessengerConnection(authorized: true)
        testController.sendText(text: "Testing from E2E test.")
        
        // Use the public API to answer the new Messenger conversation.
        // Disconnect afterwards. Ensure that auth logs out correctly.
        guard let conversationInfo2 = ApiHelper.shared.answerNewConversation() else {
            XCTFail("The message we sent may not have connected to an agent.")
            return
        }
        
        XCTAssertNotEqual(conversationInfo1.conversationId, conversationInfo2.conversationId, "For some reason, the test controller started the same conversation again.")
        testController.authLogout()
        ApiHelper.shared.sendConnectOrDisconnect(conversationInfo: conversationInfo1, connecting: false, wrapup: true)
        ApiHelper.shared.sendConnectOrDisconnect(conversationInfo: conversationInfo2, connecting: false, wrapup: true)
    }
    
    func testAttachments() {
        // Setup the session. Send a message to start the conversation.
        // Setup the session. Send a message.
        guard let messengerTester = messengerTester else {
            XCTFail("Failed to setup the Messenger tester.")
            return
        }
        messengerTester.attachmentId = nil // Ensure that we have no attachment ID already set.
        
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
        messengerTester.attemptImageAttach(attachmentName: "AttachmentTest.png", kotlinByteArray: kotlinByteArray)
        XCTAssertTrue(messengerTester.attachmentId != nil, "We did not have an attachment ID available.")
        messengerTester.refreshAttachment(attachmentId: messengerTester.attachmentId)
        messengerTester.sendUploadedImage()
        
        // Disconnect the conversation for the agent and disconnect the session.
        ApiHelper.shared.sendConnectOrDisconnect(conversationInfo: conversationInfo, connecting: false, wrapup: true)
        messengerTester.disconnectMessenger()
    }
    
    func testAttachments_ContentProfile() {
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
        
        // Pull the deployment config and ensure that we have an allowedMedia list.
        guard let deploymentConfig = messengerTester.pullDeploymentConfig() else {
            ApiHelper.shared.sendConnectOrDisconnect(conversationInfo: conversationInfo, connecting: false, wrapup: true)
            messengerTester.disconnectMessenger()
            XCTFail("Failed to pull the deployment config.")
            return
        }
        XCTAssertTrue(!deploymentConfig.messenger.fileUpload.modes.isEmpty, "The number of allowed media is missing or we were not able to pull the list.")
        
        // Attempt to attach a file that is not allowed by the Content Profile.
        // application/json should not be allowed right now.
        guard let byteArray = TestConfig.shared.pullConfigDataAsKotlinByteArray() else {
            XCTFail("Failed to convert the test config into a byte array.")
            return
        }
        messengerTester.attemptImageAttach(attachmentName: "shouldFail.json", kotlinByteArray: byteArray, shouldSucceed: false)
        
        // Disconnect the conversation for the agent and disconnect the session.
        ApiHelper.shared.sendConnectOrDisconnect(conversationInfo: conversationInfo, connecting: false, wrapup: true)
        messengerTester.disconnectMessenger()
    }
    
    func testAuthFail() {
        guard let config = TestConfig.shared.config else {
            XCTFail("Failed to pull the test config.")
            return
        }
        let deployment = try! Deployment()
        let testController = MessengerInteractorTester(deployment: Deployment(deploymentId: config.authDeploymentId, domain: deployment.domain))
        testController.authorize(config: config, authCode: "", shouldFail: true)
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
        messengerTester?.messenger.tokenVault.store(key: "token", value: newToken)
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
        delay(10, reason: "Allow time for the bot to start.")
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
    
    func testConversationClear() {
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
        
        // Test case 1: After sending Conversation Clear from client. Ensure we receive Event.ConversationCleared and Event.ConnectionClosed from Transport.
        // Client state should also be set to Closed.
        messengerTester.clearConversation()
        XCTAssertEqual(messengerTester.currentClientState, .Closed(code: 1000, reason: "The user has closed the connection."), "The client state did not close for the expected reason.")
        
        // Test case 2: While disconnected after a cleared conversation. Start a new conversation. Ensure that this conversation is considered a new session.
        messengerTester.startNewMessengerConnection()
        if let configuredState = messengerTester.currentClientState as? MessagingClientState.Configured {
            XCTAssertTrue(configuredState.newSession, "The configured session is not considered a new session.")
        } else {
            XCTFail("Unable to confirm details about the configured state \(messengerTester.currentClientState?.description ?? "N/A").")
        }
        guard let conversationInfo2 = ApiHelper.shared.answerNewConversation() else {
            XCTFail("The message we sent may not have connected to an agent.")
            return
        }
        XCTAssertNotEqual(conversationInfo.conversationId, conversationInfo2.conversationId, "The new conversationID is the same as the previous conversation's. This conversation should be new after the previous one was cleared.")
        
        // Disconnect agent from all conversations.
        ApiHelper.shared.sendConnectOrDisconnect(conversationInfo: conversationInfo, connecting: false, wrapup: true)
        ApiHelper.shared.sendConnectOrDisconnect(conversationInfo: conversationInfo2, connecting: false, wrapup: true)
        messengerTester.disconnectMessenger()
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
        
        // Use the public API to answer the new Messenger conversation.
        // Send a message from that agent and make sure we receive it.
        guard let conversationInfo = ApiHelper.shared.answerNewConversation() else {
            XCTFail("The message we sent may not have connected to an agent.")
            return
        }
        
        // Testing for https://inindca.atlassian.net/browse/MTSDK-180
        // Will disconnect and reconnect to the same conversation multiple times before checking for history.
        // This helps simulate backgrounding and returning to the UI app.
        for _ in 1...10 {
            messengerTester.disconnectMessenger()
            delay(1)
            messengerTester.startNewMessengerConnection()
            delay(1)
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
    
    // The Quick Reply bot has the following structure:
    //   First segment: Two replies possible: "Carousel", "Done".
    //     - Selecting Carousel goes to another part of the flow where a Carousel Reply option is given.
    //     - Selecting Done will end the flow.
    //   Second segment: One reply possible: "Done".
    //     - Selecting "Done" will end the flow.
    func testQuickReply() {
        // Continuing after failure to ensure that we finish the bot flow.
        continueAfterFailure = true
        
        // Create a new instance of the MessengerInteractorTester.
        // This will be used to handle sending/receiving messages through the SDK.
        // Connect to the quick reply bot.
        let deployment = try! Deployment()
        let testController = MessengerInteractorTester(deployment: Deployment(deploymentId: TestConfig.shared.config?.quickReplyBot ?? "", domain: deployment.domain))
        testController.quickReplyExpectation = XCTestExpectation(description: "Waiting for quick replies to show up.")
        testController.startNewMessengerConnection()
        
        // Wait for quick replies to show up. Verify the expected options are shown.
        testController.waitForQuickReplies()
        let repliesFirstSegment = testController.getQuickReplyOptions()
        XCTAssertEqual(repliesFirstSegment.count, 2, "An unexpected number of replies were received.")
        XCTAssertTrue(repliesFirstSegment.contains("Carousel"), "The \"Carousel\" quick reply option was not received.")
        XCTAssertTrue(repliesFirstSegment.contains("Done"), "The \"Done\" quick reply option was not received.")
        
        // Create a test expectation to ensure the test waits for an incoming message from the bot.
        // Send the quick reply to go to the Carousel digital menu.
        testController.receivedMessageExpectation = XCTestExpectation(description: "Wait for a message to be received from the bot.")
        testController.sendQuickReply(reply: "Carousel")
        
        // After sending the quick reply, wait for the expected digital menu message to be received.
        // Getting to this point ensures that we do not error out after getting to a Carousel digital menu.
        // Send a "Done" message to end the bot flow.
        testController.waitForMessageReceiveExpectation()
        let receivedMessageText = "Welcome to the Carousel menu. Reply \"Done\" to end the flow."
        testController.verifyReceivedMessage(expectedMessage: receivedMessageText)
        testController.sendText(text: "Done")
        
        // Disconnect the test messenger.
        testController.disconnectMessenger()
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
    
    func testStepUpAuthentication() {
        guard let config = TestConfig.shared.config else {
            XCTFail("Failed to pull the test config.")
            return
        }
        
        let deployment = try! Deployment()
        let testController1 = MessengerInteractorTester(deployment: Deployment(deploymentId: config.authDeploymentId, domain: deployment.domain))
        
        // Save a new token.
        let newToken = UUID().uuidString
        testController1.messenger.tokenVault.store(key: "token", value: newToken)
        print("New token: \(newToken)")
        
        // Step 1: Start as Guest: Initiate a session as a guest or anonymous user.
        testController1.startNewMessengerConnection(authorized: false)
        testController1.sendText(text: "Unauthenticated message.")
        
        // Use the public API to answer the new Messenger conversation.
        guard let conversationInfo = ApiHelper.shared.answerNewConversation() else {
            XCTFail("The message we sent may not have connected to an agent.")
            return
        }
        
        let guestMessageReceived = "Guest session message received!"
        testController1.testExpectation = XCTestExpectation(description: "Wait for message to be received in guest session.")
        ApiHelper.shared.sendOutboundSmsMessage(conversationId: conversationInfo.conversationId, communicationId: conversationInfo.communicationId, message: guestMessageReceived)
        testController1.waitForTestExpectation()
        testController1.verifyReceivedMessage(expectedMessage: guestMessageReceived)
        
        // Step 2: Step-Up Authentication
        testController1.authorize(config: config, authCode: config.authCode3)
        testController1.stepUpToAuthenticatedSession()
        testController1.sendText(text: "Authenticated session message after step-up.")
        
        let authMessageReceived = "Authenticated session message after step-up."
        testController1.testExpectation = XCTestExpectation(description: "Wait for message to be received in authenticated session.")
        ApiHelper.shared.sendOutboundSmsMessage(conversationId: conversationInfo.conversationId, communicationId: conversationInfo.communicationId, message: authMessageReceived)
        testController1.verifyReceivedMessage(expectedMessage: authMessageReceived)
        
        // Step 3: Verify Events and Session Transition
        testController1.verifyEventSignedIn()
        testController1.verifyEventExistingAuthSessionCleared()
        
        // Step 4: Close the Conversation
        ApiHelper.shared.sendConnectOrDisconnect(conversationInfo: conversationInfo, connecting: false, wrapup: true)
        
        // Step 5: Start a new session and do the authentication without any message exchange.
        let testController2 = MessengerInteractorTester(deployment: Deployment(deploymentId: config.authDeploymentId, domain: deployment.domain))
        testController2.authorize(config: config, authCode: config.authCode4)
        testController2.startNewMessengerConnection(authorized: true)
        
        // Step 7: Close the new session conversation
        ApiHelper.shared.sendConnectOrDisconnect(conversationInfo: conversationInfo, connecting: false, wrapup: true)
    }
    
    func testAuthenticatedSessionsSync() {
        guard let config = TestConfig.shared.config else {
            XCTFail("Failed to pull the test config.")
            return
        }
        
        let deployment = try! Deployment()
        let testController1 = MessengerInteractorTester(deployment: Deployment(deploymentId: config.authDeploymentId, domain: deployment.domain))
        let testController2 = MessengerInteractorTester(deployment: Deployment(deploymentId: config.authDeploymentId, domain: deployment.domain))
        
        // Start first authenticated session
        testController1.authorize(config: config, authCode: config.authCode5)
        testController1.startNewMessengerConnection(authorized: true)
        
        // Start second authenticated session
        testController2.authorize(config: config, authCode: config.authCode6)
        testController2.startNewMessengerConnection(authorized: true)
        
        // Exchange messages
        testController1.sendText(text: "Message from session 1")
        testController2.sendText(text: "Message from session 2")
        
        // Verify messages synced
        testController1.verifyReceivedMessage(expectedMessage: "Message from session 1")
        testController2.verifyReceivedMessage(expectedMessage: "Message from session 2")
        
        // Close conversations
        testController1.disconnectMessenger()
        testController2.disconnectMessenger()
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
