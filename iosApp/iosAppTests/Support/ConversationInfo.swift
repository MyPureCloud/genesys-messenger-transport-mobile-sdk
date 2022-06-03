//
//  ConversationInfo.swift
//  MessengerUITests
//
//  Created by Morehouse, Matthew on 6/3/21.
//  Copyright © 2021 Genesys. All rights reserved.
//

import Foundation

public struct ConversationInfo {
    let conversationId: String
    let communicationId: String // CommunicationId for the agent.
    let agentParticipantId: String
    let participants: [JsonDictionary]
    let agentParticipant: JsonDictionary?

    // If any participant is not disconnected, then this is a live conversation.
    var isAvailable: Bool {
        for participant in participants {
            if participant.value(forKey: "state") as? String != "disconnected" {
                return true
            }
        }
        return false
    }

    init(json: JsonDictionary) {
        self.conversationId = json.value(forKey: "id") as? String ?? ""
        self.participants = json.value(forKey: "participants") as? [JsonDictionary] ?? []
        var agentParticipant: JsonDictionary?
        for participant in self.participants where participant.value(forKey: "purpose") as? String == "agent" && participant.value(forKey: "userId") as? String == TestConfig.shared.config?.agentId {
            let messages = participant.value(forKey: "messages") as? [JsonDictionary] ?? []
            for message in messages where message.value(forKey: "state") as? String != "disconnected" {
                agentParticipant = participant
                break
            }
            if agentParticipant != nil {
                break
            }
        }
        self.agentParticipant = agentParticipant
        self.communicationId = (self.agentParticipant?.value(forKey: "messages") as? [JsonDictionary])?.first?.value(forKey: "id") as? String ?? ""
        self.agentParticipantId = self.agentParticipant?.value(forKey: "id") as? String ?? ""

    }
}
