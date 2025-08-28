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
    let messengerTransport: MessengerTransportSDK
    let messagingClient: MessagingClient
    let tokenVault: DefaultVault
    
    let stateChangeSubject = PassthroughSubject<StateChange, Never>()
    let messageEventSubject = PassthroughSubject<MessageEvent, Never>()
    let eventSubject = PassthroughSubject<Event, Never>()
        
    init(deployment: Deployment, reconnectTimeout: Int64 = 60 * 5) {
        print("Messenger Transport sdkVersion: \(MessengerTransportSDK.companion.sdkVersion)")
        
        self.configuration = Configuration(deploymentId: deployment.deploymentId,
                                           domain: deployment.domain,
                                           logging: true,
                                           reconnectionTimeoutInSeconds: reconnectTimeout,
                                           autoRefreshTokenWhenExpired: true,
                                           encryptedVault: true)
        self.tokenVault = DefaultVault(keys: Vault.Keys(vaultKey: "com.genesys.cloud.messenger", tokenKey: "token", authRefreshTokenKey: "auth_refresh_token", wasAuthenticated: "wasAuthenticated"))
        self.messengerTransport = MessengerTransportSDK(configuration: self.configuration, vault: self.tokenVault)
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
    }

    func getFileAttachmentProfile() -> FileAttachmentProfile? {
        return messagingClient.fileAttachmentProfile
    }

    func authorize(authCode: String, redirectUri: String, codeVerifier: String?) {
        messagingClient.authorize(authCode: authCode, redirectUri: redirectUri, codeVerifier: codeVerifier)
    }

    func connect() throws {
        do {
            try messagingClient.connect()
        } catch {
            print("connect() failed. \(error.localizedDescription)")
            throw error
        }
    }
    
    func connectAuthenticated() throws {
        do {
            try messagingClient.connectAuthenticatedSession()
        } catch {
            print("connectAuthenticated() failed. \(error.localizedDescription)")
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
    
    func oktaLogout() throws {
        do {
            try messagingClient.logoutFromAuthenticatedSession()
        } catch {
            print("logoutFromAuthenticatedSession() failed. \(error.localizedDescription)")
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
    
    func sendQuickReply(buttonResponse: ButtonResponse) throws {
        do {
            try messagingClient.sendQuickReply(buttonResponse: buttonResponse)
        } catch {
            print("sendQuickReply(buttonResponse:) failed. \(error.localizedDescription)")
            throw error
        }
    }

    func sendCardReply(buttonResponse: ButtonResponse) throws {
        do {
            try messagingClient.sendCardReply(postbackResponse: buttonResponse)
        } catch {
            print("sendCardReply(buttonResponse:) failed. \(error.localizedDescription)")
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

    func attachImage(kotlinByteArray: KotlinByteArray, fileName: String) throws {
        do {
            try messagingClient.attach(byteArray: kotlinByteArray, fileName: fileName, uploadProgress: { progress in
                print("Attachment upload progress: \(progress)")
            })
        } catch {
            print("attachImage(kotlinByteArray:) failed. \(error.localizedDescription)")
            throw error
        }
    }    
    
    func refreshAttachmentUrl(attachId: String) throws {
        do {
            try messagingClient.refreshAttachmentUrl(attachmentId: attachId)
        } catch {
            print("refreshAttachmentUrl(attachId:) failed. \(error.localizedDescription)")
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
    
    func invalidateConversationCache() {
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
    
    func clearConversation() throws {
        do {
            try messagingClient.clearConversation()
        } catch {
            print("clearConversation() failed. \(error.localizedDescription)")
            throw error
        }
    }
    
    func addCustomAttributes(customAttributes: [String: String] = [:]) -> Bool {
         return messagingClient.customAttributesStore.add(customAttributes: customAttributes)
    }
    
    func removeToken() {
        let tokenKey = messengerTransport.vault.keys.tokenKey
        messengerTransport.vault.remove(key: tokenKey)
    }
    
    func removeAuthRefreshToken() {
        let authRefreshTokenKey = messengerTransport.vault.keys.authRefreshTokenKey
        messengerTransport.vault.remove(key: authRefreshTokenKey)
    }

    func stepUp() throws {
        do {
            try messagingClient.stepUpToAuthenticatedSession()
        } catch {
            print("stepUp() failed. \(error.localizedDescription)")
        }
    }
    
    func wasAuthenticated() -> Bool {
        return messagingClient.wasAuthenticated
    }
    
    func shouldAuthorize(completion: @escaping (Bool) -> Void) {
        messagingClient.shouldAuthorize { shouldAuth in
            completion(shouldAuth as! Bool)
        }
    }
}
