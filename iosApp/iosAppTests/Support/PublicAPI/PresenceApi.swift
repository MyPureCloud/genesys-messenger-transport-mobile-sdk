//
//  PresenceApi.swift
//  MessengerUITests
//
//  Created by Morehouse, Matthew on 6/3/21.
//  Copyright © 2021 Genesys. All rights reserved.
//

import XCTest

extension ApiHelper {

    // This function gets the list of presences used for the "changePresence" function.
    // Returns: Returns the list of presences in the form of: [presence name : presence ID]
    private func getPresenceList() -> [String: String] {
        let defaults = UserDefaults.standard
        var presences = defaults.object(forKey: "ios-MMSDK-TESTING-presenceList") as? [String: String]
        if presences == nil || presences?.isEmpty ?? false {
            presences = retrieveAllPresences()
            defaults.set(presences, forKey: "ios-MMSDK-TESTING-presenceList")
        }

        return presences ?? [:]
    }

    private func getPresenceId(presenceName: String) -> String {
        let presenceList = getPresenceList()
        for presence in presenceList where presence.key == presenceName {
            return presence.value
        }
        return ""
    }

    private func retrieveAllPresences() -> [String: String] {
        let result = publicAPICall(httpMethod: "GET", httpURL: "/api/v2/presencedefinitions?pageSize=100")
        let entities = result?.value(forKey: "entities") as? [JsonDictionary] ?? []
        var presenceArray: [String: String] = [:]
        for entity in entities {
            let presenceName = entity.value(forKeyPath: "languageLabels.en_US") as? String ?? ""
            let presenceId = entity.value(forKey: "id") as? String ?? ""
            presenceArray.updateValue(presenceId, forKey: presenceName)
        }
        return presenceArray
    }

    /// This function can be used to change an agent's presence to what is specified.
    ///
    /// - Parameters:
    ///   - presenceName: The name of the presence. EX: Available, Offline, Away.
    ///   - userID: The user ID of the user whos presence you want to change.
    ///   - message: "THe message attached to the presence.
    public func changePresence(presenceName: String, userID: String, message: String = "") {
        var presenceID = ""
        let presenceList = getPresenceList()
        for (key, value) in presenceList where key == presenceName {
            presenceID = value
        }
        if presenceID == "" {
            print("The presence name '\(presenceName) is not available in this org.")
        }

        // Make the json payload.
        let presenceDefinition: [String: Any] = ["id": presenceID]

        let payloadDictionary: [String: Any] =
            [
                "primary": true,
                "presenceDefinition": presenceDefinition,
                "message": message
        ]

        // Sending the HTTP Request
        _ = publicAPICall(httpMethod: "PATCH", httpURL: "/api/v2/users/\(userID)/presences/PURECLOUD", jsonBody: payloadDictionary)

        // Querying the endpoint to make sure the change took effect.
        for _ in 0..<30 {
            if let queryResult = getUserPresence(userId: userID) {
                if queryResult.presenceId == presenceID {
                    break
                }
            }
        }
    }

    // Gets the specified user's presence and their status message.
    public func getUserPresence(userId: String) -> (presenceName: String, statusMessage: String, presenceId: String)? {
        let response = publicAPICall(httpMethod: "GET", httpURL: "/api/v2/users/\(userId)/presences/PURECLOUD")
        guard let presence = response?.value(forKeyPath: "presenceDefinition.systemPresence") as? String, let presenceId = response?.value(forKeyPath: "presenceDefinition.id") as? String else {
            print("There was an issue with getting presence information. ")
            return nil
        }
        if let message = response?.value(forKey: "message") as? String { // The message section may not be set so this could be nil.
            return (presence, message, presenceId)
        } else {
            return (presence, "", presenceId)
        }
    }
}
