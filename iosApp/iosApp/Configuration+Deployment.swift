//
//  Configuration+Deployment.swift
//  iosApp
//
//  Created by Chris Rumpf on 11/10/21.
//  Copyright © 2021 Genesys. All rights reserved.
//

import MessengerTransport

extension Configuration {
    convenience init?(deployment: Deployment, tokenStoreKey: String, logging: Bool, maxReconnectAttempts: Int32) {
        guard let deploymentId = deployment.deploymentId,
              let domain = deployment.domain else {
            return nil
        }
        self.init(deploymentId: deploymentId,
                  domain: domain,
                  tokenStoreKey: tokenStoreKey,
                  logging: logging,
                  maxReconnectAttempts: maxReconnectAttempts)
    }
}
