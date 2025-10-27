// ===================================================================================================
// Copyright © 2025 GenesysCloud(Genesys).
// GenesysCloud SDK.
// All rights reserved.
// ===================================================================================================

import Foundation

extension UserDefaults {
    private enum Keys {
        static let pushNotificationsRegisteredDeployments = "pushNotificationsRegisteredDeployments"
    }
    
    class func isRegisterForPushNotifications(deploymentId: String) -> Bool {
        let dict = UserDefaults.standard.dictionary(forKey: Keys.pushNotificationsRegisteredDeployments) as? [String: Bool]
        return dict?[deploymentId] ?? false
    }
    
    class func updatePushNotificationsStateFor(deploymentId: String, state: Bool) {
        var dict = UserDefaults.standard.dictionary(forKey: Keys.pushNotificationsRegisteredDeployments) as? [String: Bool] ?? [:]
        dict[deploymentId] = state ? state : nil
        UserDefaults.standard.set(dict, forKey: Keys.pushNotificationsRegisteredDeployments)

    }
}
