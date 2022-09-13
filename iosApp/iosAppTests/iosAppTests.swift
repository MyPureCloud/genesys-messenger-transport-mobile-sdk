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

    var contentController: TestContentController?

    override func setUp() {
        super.setUp()
        if contentController == nil {
            do {
                let deployment = try Deployment()
                contentController = TestContentController(deployment: deployment)
            } catch {
                XCTFail("Failed to initialize Content Controller: \(error.localizedDescription)")
            }
        }
    }

    override func tearDown() {
        super.tearDown()
    }

    func testAttachments() {
        // Setup the session. Send a message to start the conversation.
        // Setup the session. Send a message.
        guard let contentController = contentController else {
            XCTFail("Failed to setup the content controller.")
            return
        }

        contentController.startMessengerConnection()
        contentController.sendText(text: "Testing from E2E test.")

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
        contentController.attemptImageAttach(kotlinByteArray: kotlinByteArray)
        contentController.sendUploadedImage()

        // Disconnect the conversation for the agent and disconnect the session.
        ApiHelper.shared.sendConnectOrDisconnect(conversationInfo: conversationInfo, connecting: false, wrapup: true)
        contentController.disconnectMessenger()
    }

    func testMessageAttributes() {
        // Setup the session. Send a message.
        guard let contentController = contentController else {
            XCTFail("Failed to setup the content controller.")
            return
        }

        contentController.startMessengerConnection()
        contentController.sendTextWithAttribute(text: "Testing with a specific name.", attributes: ["name": "Jane Doe"])
        guard let conversationInfo = ApiHelper.shared.answerNewConversation() else {
            XCTFail("The message we sent may not have connected to an agent.")
            return
        }

        // Send a message with a new name via custom attributes.
        contentController.sendTextWithAttribute(text: "Testing with a new name!", attributes: ["name": "John Doe"])

        // Cleanup.
        ApiHelper.shared.sendConnectOrDisconnect(conversationInfo: conversationInfo, connecting: false, wrapup: true)
    }

    func testSendAndReceiveMessage() {
        // Setup the session. Send a message.
        guard let contentController = contentController else {
            XCTFail("Failed to setup the content controller.")
            return
        }

        contentController.startMessengerConnection()
        contentController.sendText(text: "Testing from E2E test.")

        // Use the public API to answer the new Messenger conversation.
        // Send a message from that agent and make sure we receive it.
        guard let conversationInfo = ApiHelper.shared.answerNewConversation() else {
            XCTFail("The message we sent may not have connected to an agent.")
            return
        }
        let receivedMessageText = "Test message sent via API request!"
        contentController.testExpectation = XCTestExpectation(description: "Wait for message to be received from the UI agent.")
        ApiHelper.shared.sendOutboundSmsMessage(conversationId: conversationInfo.conversationId, communicationId: conversationInfo.communicationId, message: receivedMessageText)
        contentController.waitForExpectation()
        contentController.verifyReceivedMessage(expectedMessage: receivedMessageText)

        // Disconnect the conversation for the agent and disconnect the session.
        ApiHelper.shared.sendConnectOrDisconnect(conversationInfo: conversationInfo, connecting: false, wrapup: true)
        contentController.disconnectMessenger()
    }

    func testTypingIndicators() {
        // Setup the session. Send a message.
        guard let contentController = contentController else {
            XCTFail("Failed to setup the content controller.")
            return

        }

        contentController.startMessengerConnection()
        contentController.sendText(text: "Testing from E2E test.")

        // Use the public API to answer the new Messenger conversation.
        // Send a message from that agent and make sure we receive it.
        guard let conversationInfo = ApiHelper.shared.answerNewConversation() else {
            XCTFail("The message we sent may not have connected to an agent.")
            return
        }

        // Send a typing indicator. Ensure that we don't receive an error.
        do {
            try contentController.indicateTyping()
        } catch {
            XCTFail(error.localizedDescription)
        }

        // Receive a typing indicator from the agent.
        contentController.testExpectation = XCTestExpectation(description: "Wait for a typing indicator from the agent.")
        ApiHelper.shared.sendTypingIndicator(conversationId: conversationInfo.conversationId, communicationId: conversationInfo.communicationId)
        contentController.waitForExpectation()

        // Disconnect the conversation for the agent and disconnect the session.
        ApiHelper.shared.sendConnectOrDisconnect(conversationInfo: conversationInfo, connecting: false, wrapup: true)
        contentController.disconnectMessenger()
    }

}
