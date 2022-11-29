//
//  Deployment.swift
//  iosApp
//
//  Created by Chris Rumpf on 11/10/21.
//  Copyright © 2021 Genesys. All rights reserved.
//

import Foundation

struct Deployment {

    private enum PropertyKeys: String {
        case deploymentId
        case deploymentDomain
    }
    
    let deploymentId: String
    let domain: String

    init() throws {
        let properties = try PropertiesResource.dictionary(forResource: "deployment")
        self.init(
            deploymentId: properties[PropertyKeys.deploymentId.rawValue] ?? "",
            domain: properties[PropertyKeys.deploymentDomain.rawValue] ?? ""
        )
    }

    init(deploymentId: String, domain: String) {
        self.deploymentId = deploymentId
        self.domain = domain
    }
}
