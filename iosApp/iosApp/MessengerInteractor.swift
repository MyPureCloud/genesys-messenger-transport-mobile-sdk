//
//  MessengerInteractor.swift
//  iosApp
//
//  Created by Morehouse, Matthew on 6/2/22.
//  Copyright © 2022 Genesys. All rights reserved.
//

import Foundation
import MessengerTransport
import Combine

final class MessengerInteractor {

    let configuration: Configuration
    let messengerTransport: MessengerTransport
    let messagingClient: MessagingClient
    
    let stateChangeSubject = PassthroughSubject<StateChange, Never>()
    let messageEventSubject = PassthroughSubject<MessageEvent, Never>()
    let eventSubject = PassthroughSubject<Event, Never>()
        
    init(deployment: Deployment, reconnectTimeout: Int64 = 60 * 5) {
        self.configuration = Configuration(deploymentId: deployment.deploymentId,
                                           domain: deployment.domain,
                                           logging: true,
                                           reconnectionTimeoutInSeconds: reconnectTimeout)
        self.messengerTransport = MessengerTransport(configuration: self.configuration)
        self.messagingClient = self.messengerTransport.createMessagingClient()
        
        messagingClient.stateChangedListener = { [weak self] stateChange in
            print("State Change: \(stateChange)")
            self?.stateChangeSubject.send(stateChange)
        }
        messagingClient.messageListener = { [weak self] message in
            print("Message Event: \(message)")
            self?.messageEventSubject.send(message)
        }
        messagingClient.eventListener = { [weak self] event in
            print("Event: \(event)")
            self?.eventSubject.send(event)
        }

        print("MessengerInteractor transport: \(messengerTransport.name) \(messengerTransport.version)")
    }

    func connect() throws {
        do {
            try messagingClient.connect()
        } catch {
            print("connect() failed. \(error.localizedDescription)")
            throw error
        }
    }
    
    func newChat() throws {
        do {
            try messagingClient.startNewChat()
        } catch {
            print("startNewChat() failed. \(error.localizedDescription)")
            throw error
        }
    }

    func disconnect() throws {
        do {
            try messagingClient.disconnect()
        } catch {
            print("disconnect() failed. \(error.localizedDescription)")
            throw error
        }
    }

    func sendMessage(text: String, customAttributes: [String: String] = [:]) throws {
        do {
            try messagingClient.sendMessage(text: text.trimmingCharacters(in: .whitespaces), customAttributes: customAttributes)
        } catch {
            print("sendMessage(text:) failed. \(error.localizedDescription)")
            throw error
        }
    }

    func fetchNextPage(completion: ((Error?) -> Void)? = nil) {
        messagingClient.fetchNextPage() { error in
            completion?(error)
        }
    }

    func sendHealthCheck() throws {
        do {
            try messagingClient.sendHealthCheck()
        } catch {
            print("sendHealthCheck() failed. \(error.localizedDescription)")
            throw error
        }
    }

    func attachImage(kotlinByteArray: KotlinByteArray, fileName: String = "image.png") throws {
        do {
            try messagingClient.attach(byteArray: kotlinByteArray, fileName: fileName, uploadProgress: { progress in
                print("Attachment upload progress: \(progress)")
            })
        } catch {
            print("attachImage(kotlinByteArray:) failed. \(error.localizedDescription)")
            throw error
        }
    }

    func detachImage(attachId: String) throws {
        do {
            try messagingClient.detach(attachmentId: attachId)
        } catch {
            print("detachImage(attachId:) failed. \(error.localizedDescription)")
            throw error
        }
    }

    func fetchDeployment(completion: @escaping (DeploymentConfig?, Error?) -> Void) {
        messengerTransport.fetchDeploymentConfig(completionHandler: completion)
    }

    func clearConversation() {
        messagingClient.invalidateConversationCache()
    }

    func indicateTyping() throws {
        do {
            try messagingClient.indicateTyping()
        } catch {
            print("indicateTyping() failed. \(error.localizedDescription)")
            throw error
        }
    }
}
