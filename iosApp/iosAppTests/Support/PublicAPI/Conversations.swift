//
//  Conversations.swift
//  MessengerUITests
//
//  Created by Morehouse, Matthew on 6/3/21.
//  Copyright © 2021 Genesys. All rights reserved.
//

import XCTest

extension ApiHelper {

    public func answerNewConversation(timeout: Int = 60) -> ConversationInfo? {
        let agentId = TestConfig.shared.config?.agentId ?? ""
        changePresence(presenceName: "On Queue", userID: agentId)
        guard let conversation = waitForConversation(timeout: timeout) else {
            print("Did not receive a new conversation to answer.")
            return nil
        }
        sendConnectOrDisconnect(conversationInfo: conversation, connecting: true, wrapup: false)
        changePresence(presenceName: "Available", userID: agentId)
        return getConversation(conversationId: conversation.conversationId)
    }

    public func disconnectExistingConversations() {
        let agentId = TestConfig.shared.config?.agentId ?? ""
        changePresence(presenceName: "Available", userID: agentId)
        guard let conversationList = getSmsConversations()?.value(forKey: "entities") as? [JsonDictionary] else {
            XCTFail("Failed to get the existing conversation list.")
            return
        }
        for conversation in conversationList {
            sendConnectOrDisconnect(conversationInfo: ConversationInfo(json: conversation), connecting: false, wrapup: true)
        }

        // Answer and disconnect any potential conversations in the queue but not yet routed to the agent.
        if let conversationInfo = answerNewConversation(timeout: 2) {
            sendConnectOrDisconnect(conversationInfo: conversationInfo, connecting: false, wrapup: true)
        }
    }

    private func getSmsConversations() -> JsonDictionary? {
        return publicAPICall(httpMethod: "GET", httpURL: "/api/v2/conversations")
    }

    private func getConversation(conversationId: String) -> ConversationInfo? {
        if let conversationJson = publicAPICall(httpMethod: "GET", httpURL: "/api/v2/conversations/\(conversationId)") {
            return ConversationInfo(json: conversationJson)
        }
        return nil
    }

    public func waitForConversation(timeout: Int = 60) -> ConversationInfo? {
        var entities: [JsonDictionary] = []
        // 1 minute wait maximum.
        // Waiting for the first conversation that matches this criteria:
        //   - An agent is on the conversation.
        //   - That agent is alerting.
        Wait.queryUntilTrue(queryInfo: "Waiting for a conversation to be available.", attempts: timeout, canFailTest: false) {
            entities = (getSmsConversations()?.value(forKey: "entities") as? [JsonDictionary] ?? []).filter {
                let agentParticipant = ($0.value(forKey: "participants") as? [JsonDictionary] ?? []).filter {
                    return $0.value(forKey: "purpose") as? String == "agent"
                }.first
                return (agentParticipant?.value(forKey: "messages") as? [JsonDictionary])?.first?.value(forKey: "state") as? String == "alerting"
            }
            return !entities.isEmpty
        }
        let conversations = entities.map {
            return ConversationInfo(json: $0)
        }.filter {
            return $0.isAvailable
        }
        return conversations.first
    }

    public func sendOutboundSmsMessage(conversationId: String, communicationId: String, message: String = "Test from agent.") {
        let payload: [String: Any] = ["textBody": message]
        _ = publicAPICall(httpMethod: "POST", httpURL: "/api/v2/conversations/messages/\(conversationId)/communications/\(communicationId)/messages", jsonBody: payload)
    }

    public func sendTypingIndicator(conversationId: String, communicationId: String) {
        let conversationEventTyping: [String: Any] = ["type": "On"]
        let payload: [String: Any] = ["typing": conversationEventTyping]
        _ = publicAPICall(httpMethod: "POST", httpURL: "/api/v2/conversations/messages/\(conversationId)/communications/\(communicationId)/typing", jsonBody: payload)
    }

    public func getCommunicationId(conversationId: String) -> String? {
        guard let results = publicAPICall(httpMethod: "GET", httpURL: "/api/v2/conversations/\(conversationId)"), let participants = results.value(forKey: "participants") as? [JsonDictionary] else {
            print("There was an issue getting the conversation information.")
            return nil
        }
        for participant in participants where participant.value(forKey: "purpose") as? String == "agent" {
            let messages = participant.value(forKey: "messages") as? [JsonDictionary] ?? []
            if let communicationId = messages[0].value(forKey: "id") as? String {
                return communicationId
            } else {
                print(participant)
            }
        }
        return nil
    }

    /// Sends the appropriate public API request to answer/disconnect from an ACD conversation.
    public func sendConnectOrDisconnect(conversationInfo: ConversationInfo, connecting: Bool, wrapup: Bool = true) {
        print("Sending request to \(connecting ? "Connect to" : "Disconnect from") a conversation.")
        let statusPayload: [String: String] = connecting ? ["state": "CONNECTED"] : ["state": "DISCONNECTED"]
        let wrapupCodeId = getDefaultWrapupCode(conversationId: conversationInfo.conversationId, participantId: conversationInfo.agentParticipantId)
        let wrapupCodePayload: [String: Any] = [
            "code": wrapupCodeId, /// This is the ID of an existing wrapup code in the Supervisor org.
            "notes": ""
        ]

        /// Send requests to disconnect the conversation and send the wrapup code.
        _ = sendStatusToParticipant(conversationId: conversationInfo.conversationId, participantId: conversationInfo.agentParticipantId, statusPayload: statusPayload)
        if connecting {
            waitForParticipantToConnect(conversationId: conversationInfo.conversationId)
        } else if wrapup {
            Wait.delay(2)
            print("Sending wrapup code.")
            sendWrapupCode(conversationInfo: conversationInfo, wrapupPayload: wrapupCodePayload)
        }
    }

    public func sendStatusToParticipant(conversationId: String, participantId: String, statusPayload: [String: Any]) -> [String: Any]? {
        return publicAPICall(httpMethod: "PATCH", httpURL: "/api/v2/conversations/messages/\(conversationId)/participants/\(participantId)", jsonBody: statusPayload)
    }

    private func waitForParticipantToConnect(conversationId: String) {
        Wait.queryUntilTrue {
            guard let conversationInfo = getConversation(conversationId: conversationId) else {
                return false
            }
            return (conversationInfo.agentParticipant?.value(forKey: "messages") as? [JsonDictionary])?.first?.value(forKey: "state") as? String == "connected"
        }
    }

    public func getDefaultWrapupCode(conversationId: String, participantId: String) -> String {
        return publicAPICall_Array(httpMethod: "GET", httpURL: "/api/v2/conversations/messages/\(conversationId)/participants/\(participantId)/wrapupcodes")?.first?.value(forKey: "id") as? String ?? ""
    }

    public func sendWrapupCode(conversationInfo: ConversationInfo, wrapupPayload: JsonDictionary) {
        _ = publicAPICall(httpMethod: "POST", httpURL: "/api/v2/conversations/messages/\(conversationInfo.conversationId)/participants/\(conversationInfo.agentParticipantId)/communications/\(conversationInfo.communicationId)/wrapup", jsonBody: wrapupPayload)
    }

    public func sendOutboundSmsImage(conversationId: String, communicationId: String) {
        guard let mediaId = uploadFileForMessages(conversationId: conversationId, communicationId: communicationId) else {
            print("Failed to upload the image.")
            return
        }
        let json: [String: Any] = [
            "direction": "outbound",
            "mediaIds": [mediaId]
        ]
        _ = publicAPICall(httpMethod: "POST", httpURL: "/api/v2/conversations/messages/\(conversationId)/communications/\(communicationId)/messages", jsonBody: json)
    }

    private func uploadFileForMessages(conversationId: String, communicationId: String) -> String? {
        // Prepare backend for a new file. Get the Upload URL.
        let result = publicAPICall(httpMethod: "POST", httpURL: "/api/v2/conversations/messages/\(conversationId)/communications/\(communicationId)/messages/media")
        guard let mediaId = result?.value(forKey: "id") as? String else {
            print("Failed to get the upload Url for messages.")
            return nil
        }

        // Send the file.
        guard let imageData = UIImage(named: "image")?.pngData() else {
            XCTFail("Failed to get Image information.")
            return nil
        }
        _ = publicAPICall(httpMethod: "POST", url: "https://apps.inindca.com/uploads/v4/services/messaging/\(mediaId)", imageData: imageData)

        // Wait for the file upload to finish.
        for _ in 0...10 {
            let check = publicAPICall(httpMethod: "GET", httpURL: "/api/v2/conversations/messages/\(conversationId)/communications/\(communicationId)/messages/media/\(mediaId)")
            print(check ?? "N/A")
            if check?.value(forKey: "status") as? String == "valid" {
                break
            }
            sleep(1) // Small delay.
        }

        return mediaId
    }
}
