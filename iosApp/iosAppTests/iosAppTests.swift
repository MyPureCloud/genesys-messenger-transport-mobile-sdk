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
                contentController?.setupSocketListeners()
                contentController?.setupMessageListener()
            } catch {
                XCTFail("Failed to initialize Content Controller: \(error.localizedDescription)")
            }
        }
    }

    override func tearDown() {
        super.tearDown()
    }

    func testSendAndReceiveMessage() {
        // Setup the session. Send a message.
        guard let contentController = contentController else {
            XCTFail("Faild to setup the content controller.")
            return
        }

        contentController.connect()
        contentController.configureSession()
        contentController.sendMessage(text: "Testing from E2E test.")

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
        contentController.disconnect()
    }

}

class TestContentController: MessengerHandler {

    var testExpectation: XCTestExpectation? = nil
    var receivedMessageText: String? = nil

    override func connect() {
        testExpectation = XCTestExpectation(description: "Wait for Connection.")
        super.connect()
        waitForExpectation()
    }

    override func configureSession() {
        testExpectation = XCTestExpectation(description: "Wait for Configuration.")
        super.configureSession()
        waitForExpectation()
    }

    override func disconnect() {
        testExpectation = XCTestExpectation(description: "Wait for Disconnect.")
        super.disconnect()
        waitForExpectation()
    }


    override func sendMessage(text: String) {
        testExpectation = XCTestExpectation(description: "Wait for message to send.")
        super.sendMessage(text: text)
        waitForExpectation()
        verifyReceivedMessage(expectedMessage: text)
    }

    func waitForExpectation() {
        let result = XCTWaiter().wait(for: [testExpectation!], timeout: 30)
        XCTAssertEqual(result, .completed, "Expectation never fullfilled: \(testExpectation?.description ?? "No description.")")
    }

    func verifyReceivedMessage(expectedMessage: String) {
        print("Checking the received message...")
        XCTAssertEqual(expectedMessage, receivedMessageText, "The received message: '\(receivedMessageText ?? "")' didn't match what was expected: '\(expectedMessage)'.")
        receivedMessageText = nil
    }

    override func setupSocketListeners() {
        client.stateListener = { [weak self] state in
            switch state {
            case _ as MessagingClientState.Connecting:
                print("connecting state")
            case _ as MessagingClientState.Connected:
                print("connected")
                self?.testExpectation?.fulfill()
            case let configured as MessagingClientState.Configured:
                print("Socket <configured>. connected: <\(configured.connected.description)> , newSession: <\(configured.newSession?.description ?? "nill")>")
                self?.testExpectation?.fulfill()
            case let closing as MessagingClientState.Closing:
                print("Socket <closing>. reason: <\(closing.reason.description)> , code: <\(closing.code.description)>")
            case let closed as MessagingClientState.Closed:
                print("Socket <closed>. reason: <\(closed.reason.description)> , code: <\(closed.code.description)>")
                self?.testExpectation?.fulfill()
            case let error as MessagingClientState.Error:
                XCTFail("Socket <error>. code: <\(error.code.description)> , message: <\(error.message ?? "No message")>")
            default:
                print("Unexpected stateListener state: \(state)")
            }
        }
    }

    override func setupMessageListener() {
        client.messageListener = { [ weak self ] event in
            switch event {
            case let messageInserted as MessageEvent.MessageInserted:
                print("Message Inserted: <\(messageInserted.message.description)>")
                self?.receivedMessageText = messageInserted.message.text
                self?.testExpectation?.fulfill()
            case let messageUpdated as MessageEvent.MessageUpdated:
                print("Message Updated: <\(messageUpdated.message.description)>")
                self?.testExpectation?.fulfill()
            case let attachmentUpdated as MessageEvent.AttachmentUpdated:
                print("Attachment Updated: <\(attachmentUpdated.attachment.description)>")
                self?.testExpectation?.fulfill()
            case let history as MessageEvent.HistoryFetched:
                print("start of conversation: <\(history.startOfConversation.description)>, messages: <\(history.messages.description)>")
                self?.testExpectation?.fulfill()
            default:
                print("Unexpected messageListener event: \(event)")
            }
        }
    }
}
