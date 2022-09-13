//
//  MessengerHandler.swift
//  iosApp
//
//  Created by Morehouse, Matthew on 6/2/22.
//  Copyright © 2022 Genesys. All rights reserved.
//

import Foundation
import MessengerTransport
import UIKit

class MessengerHandler {

    private let deployment: Deployment
    private let config: Configuration
    let messengerTransport: MessengerTransport
    let client: MessagingClient
    
    public var onStateChange: ((StateChange) -> Void)?
    public var onMessageEvent: ((MessageEvent) -> Void)?
    public var onEvent: ((Event) -> Void)?
    
    init(deployment: Deployment, reconnectTimeout: Int64 = 60 * 5) {
        self.deployment = deployment
        self.config = Configuration(deploymentId: deployment.deploymentId!,
                                    domain: deployment.domain!,
                                    logging: true,
                                    reconnectionTimeoutInSeconds: reconnectTimeout)
        self.messengerTransport = MessengerTransport(configuration: self.config)
        self.client = self.messengerTransport.createMessagingClient()
        
        client.stateChangedListener = { [weak self] stateChange in
            print("State Change: \(stateChange)")
            self?.onStateChange?(stateChange)
        }
        client.messageListener = { [weak self] message in
            print("Message Event: \(message)")
            self?.onMessageEvent?(message)
        }
        client.eventListener = { [weak self] event in
            print("Event: \(event)")
            self?.onEvent?(event)
        }
    }

    func connect() throws {
        do {
            try client.connect()
        } catch {
            print("connect() failed. \(error.localizedDescription)")
            throw error
        }
    }
    
    func configureSession() throws {
        do {
            try client.configureSession()
        } catch {
            print("configureSession() failed. \(error.localizedDescription)")
            throw error
        }
    }
    
    func connect(shouldConfigure: Bool) throws {
        do {
            try client.connect(shouldConfigure: shouldConfigure)
        } catch {
            print("connectWithConfigure() failed. \(error.localizedDescription)")
            throw error
        }
    }

    func disconnect() throws {
        do {
            try client.disconnect()
        } catch {
            print("disconnect() failed. \(error.localizedDescription)")
            throw error
        }
    }

    func sendMessage(text: String, customAttributes: [String: String] = [:]) throws {
        do {
            try client.sendMessage(text: text.trimmingCharacters(in: .whitespaces), customAttributes: customAttributes)
        } catch {
            print("sendMessage(text:) failed. \(error.localizedDescription)")
            throw error
        }
    }

    func fetchNextPage(completion: ((Error?) -> Void)? = nil) {
        client.fetchNextPage() { _, error in
            completion?(error)
        }
    }

    func sendHealthCheck() throws {
        do {
            try client.sendHealthCheck()
        } catch {
            print("sendHealthCheck() failed. \(error.localizedDescription)")
            throw error
        }
    }

    func attachImage(kotlinByteArray: KotlinByteArray) throws {
        do {
            try client.attach(byteArray: kotlinByteArray, fileName: "image.png", uploadProgress: { progress in
                print("Attachment upload progress: \(progress)")
            })
        } catch {
            print("attachImage(kotlinByteArray:) failed. \(error.localizedDescription)")
            throw error
        }
    }

    func detachImage(attachId: String) throws {
        do {
            try client.detach(attachmentId: attachId)
        } catch {
            print("detachImage(attachId:) failed. \(error.localizedDescription)")
            throw error
        }
    }

    func fetchDeployment(completion: @escaping (DeploymentConfig?, Error?) -> Void) {
        messengerTransport.fetchDeploymentConfig(completionHandler: completion)
    }

    func clearConversation() {
        client.invalidateConversationCache()
    }

    func indicateTyping() throws {
        do {
            try client.indicateTyping()
        } catch {
            print("indicateTyping() failed. \(error.localizedDescription)")
            throw error
        }
    }
}
