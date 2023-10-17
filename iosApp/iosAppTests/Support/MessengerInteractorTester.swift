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
    var receivedMessageExpectation: XCTestExpectation?
    var readOnlyStateExpectation: XCTestExpectation?
    var connectedExpectation: XCTestExpectation?
    var errorExpectation: XCTestExpectation?
    var disconnectedSession: XCTestExpectation?
    var connectionClosed: XCTestExpectation?
    var closedStateChange: XCTestExpectation?
    var conversationCleared: XCTestExpectation?
    var authExpectation: XCTestExpectation?
    var receivedMessageText: String? = nil
    var receivedDownloadUrl: String? = nil
    var humanizeEnabled: Bool = true
    var currentClientState: MessagingClientState?
    var authState: AuthState = .noAuth

    private var historyExpectation: XCTestExpectation?
    private var historyMessages: [Message] = []
    
    private var cancellables = Set<AnyCancellable>()

    init(deployment: Deployment, reconnectTimeout: Int64 = 60 * 5) {
        messenger = MessengerInteractor(deployment: deployment, reconnectTimeout: reconnectTimeout)

        messenger.stateChangeSubject
            .sink { [weak self] stateChange in
                print("State Event. New state: \(stateChange.newState), old state: \(stateChange.oldState)")
                self?.currentClientState = stateChange.newState
                let newState = stateChange.newState
                switch newState {
                case _ as MessagingClientState.Configured:
                    self?.testExpectation?.fulfill()
                case _ as MessagingClientState.Connected:
                    self?.connectedExpectation?.fulfill()
                case _ as MessagingClientState.ReadOnly:
                    self?.readOnlyStateExpectation?.fulfill()
                case _ as MessagingClientState.Closed:
                    self?.testExpectation?.fulfill()
                    self?.closedStateChange?.fulfill()
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

                    // Handling for received messages.
                    if messageInserted.message.direction.name == "Outbound" {
                        print("Verifying that the message from the agent/bot has an expected name and an imageUrl attached.")

                        // Different checks applied depending on the originating entity.
                        // Expecting the bot to not have a name or avatar. If the deployment config is updated, this may need to change.
                        switch messageInserted.message.from.originatingEntity {
                        case .human:
                            let expectedName = (self?.humanizeEnabled ?? true) ? TestConfig.shared.config?.agentName : nil
                            let expectedUrl = (self?.humanizeEnabled ?? true) ? TestConfig.shared.config?.expectedAvatarUrl : nil
                            print("Expecting humanize: \(self?.humanizeEnabled ?? true)")
                            print("Expected Name: \(expectedName ?? "N/A")")
                            print("Expected avatar url: \(expectedUrl ?? "N/A")")
                            XCTAssertEqual(messageInserted.message.from.name, expectedName, "The agent name was not what was expected.")
                            XCTAssertEqual(messageInserted.message.from.imageUrl, expectedUrl, "The agent avatar url not what was expected.")
                        case .bot:
                            let expectedName = TestConfig.shared.config?.botName ?? ""
                            XCTAssertEqual(messageInserted.message.from.name, expectedName, "The bot name was not what was expected.")
                            XCTAssertNil(messageInserted.message.from.imageUrl, "The bot image was not what was expected.")
                        default:
                            XCTFail("Unexpected orginating entity: \(messageInserted.message.from.originatingEntity)")
                        }

                        // Specific expectation to make sure a RECEIVED message is handled.
                        // The normal TestExpectation will just check that ANY messageInserted event is handled.
                        self?.receivedMessageExpectation?.fulfill()
                    }
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
                    } else if let attachmentError = attachmentUpdated.attachment.state as? Attachment.StateError {
                        print("Attachment Error: \(attachmentError.description())")
                        self?.errorExpectation?.fulfill()
                    }
                case let history as MessageEvent.HistoryFetched:
                    print("HistoryEvent: <\(history.startOfConversation.description)>, messages:")
                    for message in history.messages {
                        print(message.description())
                    }
                    self?.historyMessages = history.messages
                    self?.historyExpectation?.fulfill()
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
                case let disconnectedEvent as Event.ConversationDisconnect:
                    print("Conversation was disconnected by the agent. \(disconnectedEvent.description)")
                    self?.disconnectedSession?.fulfill()
                case let authorized as Event.Authorized:
                    print("Auth event: \(authorized.description)")
                    self?.authState = AuthState.authorized
                    self?.authExpectation?.fulfill()
                case let loggedOut as Event.Logout:
                    print("Auth event: \(loggedOut.description)")
                    self?.authState = AuthState.loggedOut
                    self?.authExpectation?.fulfill()
                case let cleared as Event.ConversationCleared:
                    print("Conversation cleared event: \(cleared.description)")
                    self?.conversationCleared?.fulfill()
                case let error as Event.Error:
                    print("Error Event: \(error.description())")
                    self?.errorExpectation?.fulfill()
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
                print(error.localizedDescription)
            }
            deploymentConfig = config
            expectation.fulfill()
        }
        waitForExpectation(expectation, timeout: 30.0)
        return deploymentConfig
    }

    // Attempts to return to a previous session.
    // If readOnly is enabled, this will bring the user to the read only state.
    func startMessengerConnection(file: StaticString = #file, line: UInt = #line) {
        do {
            readOnlyStateExpectation = XCTestExpectation(description: "Wait for Configuration.")
            try messenger.connect()
            waitForExpectation(readOnlyStateExpectation!)
        } catch {
            XCTFail("Possible issue with connecting to the backend: \(error.localizedDescription)", file: file, line: line)
        }
    }

    // Starts a new messaging session.
    // Should be used if ReadOnly is enabled and the user wants to start a new chat.
    func startNewMessengerConnection(authorized: Bool = false, file: StaticString = #file, line: UInt = #line) {
        do {
            testExpectation = XCTestExpectation(description: "Wait for Configuration.")
            if currentClientState is MessagingClientState.ReadOnly {
                try messenger.newChat()
            } else {
                connectedExpectation = XCTestExpectation(description: "Check for the connected state.")
                if authorized {
                    try messenger.connectAuthenticated()
                } else {
                    try messenger.connect()
                }
                let connectedCheck = XCTWaiter().wait(for: [connectedExpectation!], timeout: 15) == .completed // If we're connecting to a new conversation, we will be connected first. Waiting for the configured check will give the back end enough time to send the ReadOnly state to the user.
                let configuredCheck = XCTWaiter().wait(for: [testExpectation!], timeout: 15) == .completed
                XCTAssertTrue(configuredCheck || connectedCheck, "Did not successfully connect to messenger.")

                // If after we connect we end up in the ReadOnly state, we should run messenger.newChat() and wait again.
                if currentClientState is MessagingClientState.ReadOnly {
                    testExpectation = XCTestExpectation(description: "Wait for Configuration.")
                    try messenger.newChat()
                    waitForTestExpectation()
                }
            }
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

    func authorize(config: Config, authCode: String, shouldFail: Bool = false) {
        authExpectation = XCTestExpectation(description: "Wait for authorization to finish.")
        errorExpectation = XCTestExpectation(description: "Wait for authorization to fail.")
        messenger.authorize(authCode: authCode, redirectUri: config.redirectUri, codeVerifier: config.oktaCodeVerifier)
        if shouldFail {
            let result = XCTWaiter().wait(for: [errorExpectation!], timeout: 60)
            XCTAssertEqual(result, .completed, "The test did not receive an error as expected.")
        } else {
            let result = XCTWaiter().wait(for: [authExpectation!], timeout: 60)
            XCTAssertEqual(result, .completed, "The test may not have authorized correctly.")
        }
    }

    func authLogout() {
        authExpectation = XCTestExpectation(description: "Wait for authentication to log out.")
        do {
            try messenger.oktaLogout()
        } catch {
            XCTFail("Failed to logout from okta.")
        }
        let result = XCTWaiter().wait(for: [authExpectation!], timeout: 60)
        XCTAssertEqual(result, .completed, "The test may not have logged out correctly.")
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

    func attemptImageAttach(attachmentName: String, kotlinByteArray: KotlinByteArray, shouldSucceed: Bool = true, file: StaticString = #file, line: UInt = #line) {
        do {
            try attachImage(attachmentName: attachmentName, kotlinByteArray: kotlinByteArray, shouldSucceed: shouldSucceed)
        } catch {
            XCTFail("Failed to attach image.\n\(error.localizedDescription)", file: file, line: line)
        }
    }

    private func attachImage(attachmentName: String, kotlinByteArray: KotlinByteArray, shouldSucceed: Bool = true) throws {
        if shouldSucceed {
            testExpectation = XCTestExpectation(description: "Wait for image to attach successfully.")
        } else {
            errorExpectation = XCTestExpectation(description: "Wait for the image to fail to attach.")
        }
        do {
            try messenger.attachImage(kotlinByteArray: kotlinByteArray, fileName: attachmentName)
        } catch {
            if shouldSucceed {
                throw error
            }
        }
        if shouldSucceed {
            waitForTestExpectation()
        } else {
            waitForErrorExpectation()
        }
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

    func pullHistory() -> [Message] {
        historyExpectation = XCTestExpectation(description: "Wait for history results.")
        messenger.fetchNextPage { error in
            if let error = error {
                XCTFail(error.localizedDescription)
            }
        }
        let result = XCTWaiter().wait(for: [historyExpectation!], timeout: 30)
        XCTAssertEqual(result, .completed, "Did not successfully pull the message history.")
        return historyMessages
    }

    func waitForTestExpectation(timeout: Double = 60.0) {
        guard let expectation = testExpectation else {
            XCTFail("No expectation to wait for.")
            return
        }
        waitForExpectation(expectation, timeout: timeout)
    }

    func waitForMessageReceiveExpectation(timeout: Double = 60.0) {
        guard let receivedMessageExpectation = receivedMessageExpectation else {
            XCTFail("No received message expectation to wait for.")
            return
        }
        waitForExpectation(receivedMessageExpectation, timeout: timeout)
    }

    func waitForErrorExpectation(timeout: Double = 60.0) {
        guard let expectation = errorExpectation else {
            XCTFail("No expectation to wait for.")
            return
        }
        waitForExpectation(expectation, timeout: timeout)
    }

    // When the agent disconnects the session,
    func waitForAgentDisconnect() {
        guard let disconnectedSession = disconnectedSession else {
            XCTFail("The disconnect session expectation was never initialized.")
            return
        }
        waitForExpectation(disconnectedSession)
    }

    func waitForReadOnlyState() {
        guard let readOnlyStateExpectation = readOnlyStateExpectation else {
            XCTFail("The read only state expectation was never initialized.")
            return
        }
        waitForExpectation(readOnlyStateExpectation)
    }
    
    private func waitForExpectation(_ expectation: XCTestExpectation, timeout: Double = 60.0) {
        let result = XCTWaiter().wait(for: [expectation], timeout: timeout)
        XCTAssertEqual(result, .completed, "Test expectation never fullfilled: \(expectation.description)")
    }

    func verifyReceivedMessage(expectedMessage: String) {
        print("Checking the received message.\nExpecting: '\(expectedMessage)'")
        XCTAssertEqual(expectedMessage, receivedMessageText, "The received message: '\(receivedMessageText ?? "")' didn't match what was expected: '\(expectedMessage)'.")
        receivedMessageText = nil
    }

    func clearConversation() {
        connectionClosed = XCTestExpectation(description: "Wait for connection to be closed.")
        closedStateChange = XCTestExpectation(description: "Wait for the connection state to be closed.")
        conversationCleared = XCTestExpectation(description: "Wait for the conversation cleared event to be received.")
        do {
            try messenger.clearConversation()
        } catch {
            XCTFail(error.localizedDescription)
        }
        let result = XCTWaiter().wait(for: [connectionClosed!, closedStateChange!, conversationCleared!], timeout: 30)
        XCTAssertTrue(result == .completed, "The Clear Conversation command may have had an error, or the expected state changes didn't happen.")
    }

}

public enum AuthState {
    case noAuth
    case authCodeReceived(authCode: String)
    case authorized
    case loggedOut
    case error(errorCode: ErrorCode, message: String?, correctiveAction: CorrectiveAction)
}
