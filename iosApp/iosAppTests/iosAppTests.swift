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

}

class TestContentController: MessengerHandler {

    var testExpectation: XCTestExpectation? = nil
    var receivedMessageText: String? = nil
    var receivedDownloadUrl: String? = nil
    
    override init(deployment: Deployment) {
        super.init(deployment: deployment)
        
        client.onStateChanged = { [weak self] stateChange in
            print("State Event. New state: \(stateChange.newState), old state: \(stateChange.oldState)")
            let newState = stateChange.newState
            switch newState {
            case _ as MessagingClientState.Configured:
                self?.testExpectation?.fulfill()
            case _ as MessagingClientState.Closed:
                self?.testExpectation?.fulfill()
            case let error as MessagingClientState.Error:
                XCTFail("Socket <error>. code: <\(error.code.description)> , message: <\(error.message ?? "No message")>")
            default:
                break
            }
            self?.onStateChange?(stateChange)
        }
        
        client.messageListener = { [weak self] event in
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
                // Only finish the wait when the attachment has finished uploading.
                if let uploadedAttachment = attachmentUpdated.attachment.state as? Attachment.StateUploaded {
                    self?.receivedDownloadUrl = uploadedAttachment.downloadUrl
                    self?.testExpectation?.fulfill()
                }
            case let history as MessageEvent.HistoryFetched:
                print("start of conversation: <\(history.startOfConversation.description)>, messages: <\(history.messages.description)>")
                self?.testExpectation?.fulfill()
            default:
                print("Unexpected messageListener event: \(event)")
            }
            self?.onMessageEvent?(event)
        }
        
    }

    func startMessengerConnection(file: StaticString = #file, line: UInt = #line) {
        do {
            try connect(shouldConfigure: true)
        } catch {
            XCTFail("Possible issue with connecting to the backend: \(error.localizedDescription)", file: file, line: line)
        }
    }

    override func connect(shouldConfigure: Bool) throws {
        testExpectation = XCTestExpectation(description: "Wait for Configuration.")
        try super.connect(shouldConfigure: shouldConfigure)
        waitForExpectation()
    }

    func disconnectMessenger(file: StaticString = #file, line: UInt = #line) {
        do {
            try disconnect()
        } catch {
            XCTFail("Failed to disconnect the session.\n\(error.localizedDescription)", file: file, line: line)
        }
    }

    override func disconnect() throws {
        testExpectation = XCTestExpectation(description: "Wait for Disconnect.")
        try super.disconnect()
        waitForExpectation()
    }

    func sendText(text: String, file: StaticString = #file, line: UInt = #line) {
        do {
            try sendMessage(text: text)
        } catch {
            XCTFail("Failed to send the message '\(text)'\n\(error.localizedDescription)", file: file, line: line)
        }
    }

    override func sendMessage(text: String, customAttributes: [String: String] = [:]) throws {
        testExpectation = XCTestExpectation(description: "Wait for message to send.")
        try super.sendMessage(text: text)
        waitForExpectation()
        verifyReceivedMessage(expectedMessage: text)
    }

    func attemptImageAttach(kotlinByteArray: KotlinByteArray, file: StaticString = #file, line: UInt = #line) {
        do {
            try attachImage(kotlinByteArray: kotlinByteArray)
        } catch {
            XCTFail("Failed to attach image.\n\(error.localizedDescription)", file: file, line: line)
        }
    }

    override func attachImage(kotlinByteArray: KotlinByteArray) throws {
        testExpectation = XCTestExpectation(description: "Wait for image to attach successfully.")
        try super.attachImage(kotlinByteArray: kotlinByteArray)
        waitForExpectation()
    }

    func sendUploadedImage(file: StaticString = #file, line: UInt = #line) {
        testExpectation = XCTestExpectation(description: "Wait for the uploaded image url to send.")
        guard let receivedDownloadUrl = receivedDownloadUrl else {
            XCTFail("There was no download URL received.")
            return
        }
        do {
            try super.sendMessage(text: receivedDownloadUrl)
        } catch {
            XCTFail("Failed to upload an image.\n\(error.localizedDescription)", file: file, line: line)
        }
        waitForExpectation()
        verifyReceivedMessage(expectedMessage: receivedDownloadUrl)
    }

    func waitForExpectation() {
        let result = XCTWaiter().wait(for: [testExpectation!], timeout: 60)
        XCTAssertEqual(result, .completed, "Expectation never fullfilled: \(testExpectation?.description ?? "No description.")")
    }

    func verifyReceivedMessage(expectedMessage: String) {
        print("Checking the received message...")
        XCTAssertEqual(expectedMessage, receivedMessageText, "The received message: '\(receivedMessageText ?? "")' didn't match what was expected: '\(expectedMessage)'.")
        receivedMessageText = nil
    }

}
