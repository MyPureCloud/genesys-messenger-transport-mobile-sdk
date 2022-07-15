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
    let client: MessagingClient
    
    public var onStateChange: ((StateChange) -> Void)?
    public var onMessageEvent: ((MessageEvent) -> Void)?
    
    init(deployment: Deployment) {
        self.deployment = deployment
        self.config = Configuration(deploymentId: deployment.deploymentId!,
                                    domain: deployment.domain!,
                                    tokenStoreKey: "com.genesys.cloud.messenger",
                                    logging: true)
        self.client = MobileMessenger().createMessagingClient(configuration: self.config)
        
        client.onStateChanged = { [weak self] stateChange in
            print("State Change: \(stateChange)")
            self?.onStateChange?(stateChange)
        }
        client.messageListener = { [weak self] event in
            print("Message Event: \(event)")
            self?.onMessageEvent?(event)
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
        client.fetchNextPage() {_, error in
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
        MobileMessenger().fetchDeploymentConfig(
            domain: deployment.domain!,
            deploymentId: deployment.deploymentId!,
            logging: true,
            completionHandler: completion)
    }

    func clearConversation() {
        client.invalidateConversationCache()
    }
}
