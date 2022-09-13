//
//  TestContentController.swift
//  iosAppTests
//
//  Created by Morehouse, Matthew on 9/8/22.
//  Copyright © 2022 orgName. All rights reserved.
//

import XCTest
import MessengerTransport
@testable import iosApp

class TestContentController: MessengerHandler {

    var testExpectation: XCTestExpectation? = nil
    var errorExpectation: XCTestExpectation? = nil
    var receivedMessageText: String? = nil
    var receivedDownloadUrl: String? = nil

    override init(deployment: Deployment, reconnectTimeout: Int64 = 60 * 5) {
        super.init(deployment: deployment, reconnectTimeout: reconnectTimeout)

        client.stateChangedListener = { [weak self] stateChange in
            print("State Event. New state: \(stateChange.newState), old state: \(stateChange.oldState)")
            let newState = stateChange.newState
            switch newState {
            case _ as MessagingClientState.Configured:
                self?.testExpectation?.fulfill()
            case _ as MessagingClientState.Closed:
                self?.testExpectation?.fulfill()
            case let error as MessagingClientState.Error:
                print("Socket <error>. code: <\(error.code.description)> , message: <\(error.message ?? "No message")>")
                self?.errorExpectation?.fulfill()
            default:
                break
            }
            self?.onStateChange?(stateChange)
        }

        client.messageListener = { [weak self] message in
            switch message {
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
                print("Unexpected messageListener event: \(message)")
            }
            self?.onMessageEvent?(message)
        }

        client.eventListener = { [weak self] event in
            switch event {
            case let typing as Event.AgentTyping:
                print("Agent is typing: \(typing)")
                self?.testExpectation?.fulfill()
            default:
                print("Other event. \(event)")
            }
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

    func sendTextWithAttribute(text: String, attributes: [String: String], file: StaticString = #file, line: UInt = #line) {
        do {
            try sendMessage(text: text, customAttributes: attributes)
        } catch {
            XCTFail("Failed to send the message \(text) with the attributes: \(attributes)\n\(error.localizedDescription)", file: file, line: line)
        }
    }

    override func sendMessage(text: String, customAttributes: [String: String] = [:]) throws {
        testExpectation = XCTestExpectation(description: "Wait for message to send.")
        try super.sendMessage(text: text, customAttributes: customAttributes)
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

    override func indicateTyping() throws {
        // No need to wait for an expectation. Just need to make sure the request doesn't fail.
        do {
            try super.indicateTyping()
        } catch {
            XCTFail(error.localizedDescription)
        }
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
