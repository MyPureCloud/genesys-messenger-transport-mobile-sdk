//
//  MessengerInteractorTester.swift
//  iosAppTests
//
//  Created by Morehouse, Matthew on 9/8/22.
//  Copyright © 2022 orgName. All rights reserved.
//

import XCTest
import MessengerTransport
import Combine
@testable import iosApp

class MessengerInteractorTester {

    let messenger: MessengerInteractor
    var testExpectation: XCTestExpectation?
    var errorExpectation: XCTestExpectation?
    var connectionClosed: XCTestExpectation?
    var receivedMessageText: String? = nil
    var receivedDownloadUrl: String? = nil
    
    private var cancellables = Set<AnyCancellable>()

    init(deployment: Deployment, reconnectTimeout: Int64 = 60 * 5) {
        messenger = MessengerInteractor(deployment: deployment, reconnectTimeout: reconnectTimeout)

        messenger.stateChangeSubject
            .sink { [weak self] stateChange in
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
            }
            .store(in: &cancellables)
        
        messenger.messageEventSubject
            .sink { [weak self] message in
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
            }
            .store(in: &cancellables)

        messenger.eventSubject
            .sink { [weak self] event in
                switch event {
                case let typing as Event.AgentTyping:
                    print("Agent is typing: \(typing)")
                    self?.testExpectation?.fulfill()
                case let closedEvent as Event.ConnectionClosed:
                    print("Connection was closed: \(closedEvent)")
                    self?.connectionClosed?.fulfill()
                default:
                    print("Other event. \(event)")
                }
            }
            .store(in: &cancellables)
    }

    func pullDeploymentConfig() -> DeploymentConfig? {
        var deploymentConfig: DeploymentConfig?
        let expectation = XCTestExpectation(description: "Wait for deployment config.")
        
        messenger.fetchDeployment { config, error in
            if let error = error {
                XCTFail(error.localizedDescription)
            }
            deploymentConfig = config
            expectation.fulfill()
        }
        waitForExpectation(expectation, timeout: 30.0)
        return deploymentConfig
    }

    func startMessengerConnection(file: StaticString = #file, line: UInt = #line) {
        do {
            testExpectation = XCTestExpectation(description: "Wait for Configuration.")
            try messenger.connect()
            waitForTestExpectation()
        } catch {
            XCTFail("Possible issue with connecting to the backend: \(error.localizedDescription)", file: file, line: line)
        }
    }
    
    func startMessengerConnectionWithErrorExpectation(_ errorExpectation: XCTestExpectation, file: StaticString = #file, line: UInt = #line) {
        do {
            self.errorExpectation = errorExpectation
            try messenger.connect()
            waitForErrorExpectation()
        } catch {
            XCTFail("Connect threw other than the expected error: \(error.localizedDescription)", file: file, line: line)
        }
    }

    func disconnectMessenger(file: StaticString = #file, line: UInt = #line) {
        do {
            testExpectation = XCTestExpectation(description: "Wait for Disconnect.")
            try messenger.disconnect()
            waitForTestExpectation()
        } catch {
            XCTFail("Failed to disconnect the session.\n\(error.localizedDescription)", file: file, line: line)
        }
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

    private func sendMessage(text: String, customAttributes: [String: String] = [:]) throws {
        testExpectation = XCTestExpectation(description: "Wait for message to send.")
        try messenger.sendMessage(text: text, customAttributes: customAttributes)
        waitForTestExpectation()
        verifyReceivedMessage(expectedMessage: text)
    }

    func attemptImageAttach(kotlinByteArray: KotlinByteArray, file: StaticString = #file, line: UInt = #line) {
        do {
            try attachImage(kotlinByteArray: kotlinByteArray)
        } catch {
            XCTFail("Failed to attach image.\n\(error.localizedDescription)", file: file, line: line)
        }
    }

    private func attachImage(kotlinByteArray: KotlinByteArray) throws {
        testExpectation = XCTestExpectation(description: "Wait for image to attach successfully.")
        try messenger.attachImage(kotlinByteArray: kotlinByteArray)
        waitForTestExpectation()
    }

    func sendUploadedImage(file: StaticString = #file, line: UInt = #line) {
        testExpectation = XCTestExpectation(description: "Wait for the uploaded image url to send.")
        guard let receivedDownloadUrl = receivedDownloadUrl else {
            XCTFail("There was no download URL received.")
            return
        }
        do {
            try messenger.sendMessage(text: receivedDownloadUrl)
        } catch {
            XCTFail("Failed to upload an image.\n\(error.localizedDescription)", file: file, line: line)
        }
        waitForTestExpectation()
        verifyReceivedMessage(expectedMessage: receivedDownloadUrl)
    }

    func indicateTyping() {
        // No need to wait for an expectation. Just need to make sure the request doesn't fail.
        do {
            try messenger.indicateTyping()
        } catch {
            XCTFail(error.localizedDescription)
        }
    }

    func waitForTestExpectation(timeout: Double = 60.0) {
        guard let expectation = testExpectation else {
            XCTFail("No expectation to wait for.")
            return
        }
        waitForExpectation(expectation, timeout: timeout)
    }
    
    func waitForErrorExpectation(timeout: Double = 60.0) {
        guard let expectation = errorExpectation else {
            XCTFail("No expectation to wait for.")
            return
        }
        waitForExpectation(expectation, timeout: timeout)
    }
    
    private func waitForExpectation(_ expectation: XCTestExpectation, timeout: Double = 60.0) {
        let result = XCTWaiter().wait(for: [expectation], timeout: timeout)
        XCTAssertEqual(result, .completed, "Test expectation never fullfilled: \(expectation.description)")
    }

    func verifyReceivedMessage(expectedMessage: String) {
        print("Checking the received message...")
        XCTAssertEqual(expectedMessage, receivedMessageText, "The received message: '\(receivedMessageText ?? "")' didn't match what was expected: '\(expectedMessage)'.")
        receivedMessageText = nil
    }

}
