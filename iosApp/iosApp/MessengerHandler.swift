//
//  MessengerHandler.swift
//  iosApp
//
//  Created by Morehouse, Matthew on 6/2/22.
//  Copyright © 2022 orgName. All rights reserved.
//

import Foundation
import MessengerTransport
import UIKit

class MessengerHandler {

    let client: MessagingClient
    private let deployment: Deployment
    let config: Configuration
    var outputLabel: UILabel? = nil

    init(deployment: Deployment) {
        self.deployment = deployment
        self.config = Configuration(deploymentId: deployment.deploymentId!,
                                    domain: deployment.domain!,
                                    tokenStoreKey: "com.genesys.cloud.messenger",
                                    logging: true)
        self.client = MobileMessenger().createMessagingClient(configuration: self.config)
    }

    func setOutputLabel(outputLabel: UILabel) {
        self.outputLabel = outputLabel
    }

    func setupSocketListeners() {
        client.stateListener = { [weak self] state in
            switch state {
            case _ as MessagingClientState.Connecting:
                print("connecting state")
                self?.outputLabel?.text = "<connecting>"
            case _ as MessagingClientState.Connected:
                print("connected")
                self?.outputLabel?.text = "<connected>"
            case let configured as MessagingClientState.Configured:
                print("Socket <configured>. connected: <\(configured.connected.description)> , newSession: <\(configured.newSession?.description ?? "nill")>")
                self?.outputLabel?.text = "Socket <configured>. connected: <\(configured.connected.description)> , newSession: <\(configured.newSession?.description ?? "nill")>"
            case let closing as MessagingClientState.Closing:
                print("Socket <closing>. reason: <\(closing.reason.description)> , code: <\(closing.code.description)>")
                self?.outputLabel?.text = "Socket <closing>. reason: <\(closing.reason.description)> , code: <\(closing.code.description))>"
            case let closed as MessagingClientState.Closed:
                print("Socket <closed>. reason: <\(closed.reason.description)> , code: <\(closed.code.description)>")
                self?.outputLabel?.text = "Socket <closed>. reason: <\(closed.reason.description)> , code: <\(closed.code.description)>"
            case let error as MessagingClientState.Error:
                print("Socket <error>. code: <\(error.code.description)> , message: <\(error.message ?? "No message")>")
                self?.outputLabel?.text = "Socket <error>. code: <\(error.code.description)> , message: <\(error.message ?? "No message")>"
            default:
                print("Unexpected stateListener state: \(state)")
            }
        }
    }

    func setupMessageListener() {
        client.messageListener = { [weak self] event in
            switch event {
            case let messageInserted as MessageEvent.MessageInserted:
                self?.outputLabel?.text = "Message Inserted: <\(messageInserted.message.description)>"
            case let messageUpdated as MessageEvent.MessageUpdated:
                self?.outputLabel?.text = "Message Updated: <\(messageUpdated.message.description)>"
            case let attachmentUpdated as MessageEvent.AttachmentUpdated:
                self?.outputLabel?.text = "Attachment Updated: <\(attachmentUpdated.attachment.description)>"
            case let history as MessageEvent.HistoryFetched:
                self?.outputLabel?.text = "start of conversation: <\(history.startOfConversation.description)>, messages: <\(history.messages.description)> "
            default:
                print("Unexpected messageListener event: \(event)")
            }
        }
    }

    func configureSession() {
        do {
            try client.configureSession()
        } catch {
            print("Failed to configure session. \(error.localizedDescription)")
            outputLabel?.text = "<\(error.localizedDescription)>"
        }
    }

    func connect() {
        do {
            try client.connect()
        } catch {
            print(error)
            outputLabel?.text = "<\(error.localizedDescription)>"
        }
    }

    func disconnect() {
        do {
            try client.disconnect()
        } catch {
            print(error)
            outputLabel?.text = "<\(error.localizedDescription)>"
        }
    }

    func sendMessage(text: String) {
        do {
            try client.sendMessage(text: text.trimmingCharacters(in: .whitespaces))
        } catch {
            print(error)
            outputLabel?.text = "<\(error.localizedDescription)>"
        }
    }

    func fetchNextPage() {
        client.fetchNextPage() {_, error in
            if let error = error {
                self.outputLabel?.text = "<\(error.localizedDescription)>"
                return
            }
        }
    }

    func sendHealthCheck() {
        do {
            try client.sendHealthCheck()
        } catch {
            print(error)
            outputLabel?.text = "<\(error.localizedDescription)>"
        }
    }

    func attachImage(kotlinByteArray: KotlinByteArray) {
        do {
            try client.attach(byteArray: kotlinByteArray, fileName: "image.png", uploadProgress: { progress in
                print("Attachment upload progress: \(progress)")
            })
        } catch {
            print(error)
            outputLabel?.text = "<\(error.localizedDescription)>"
        }
    }

    func detachImage(attachId: String) {
        do {
            try client.detach(attachmentId: attachId)
        } catch {
            print(error)
            outputLabel?.text = "<\(error.localizedDescription)>"
        }
    }

    func fetchDeployment() {
        MobileMessenger().fetchDeploymentConfig(domain: deployment.domain!, deploymentId: deployment.deploymentId!, logging: true,
            completionHandler: { deploymentConfig, error in
            if let error = error {
                self.outputLabel?.text = "<\(error.localizedDescription)>"
                return
            }
            self.outputLabel?.text = "<\(deploymentConfig?.description() ?? "Unknown deployment config")>"
        })
    }

    func clearConversation() {
        client.invalidateConversationCache()
    }
}
