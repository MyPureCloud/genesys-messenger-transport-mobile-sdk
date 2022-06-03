//
//  TestConfig.swift
//  MessengerUITests
//
//  Created by Morehouse, Matthew on 6/2/21.
//  Copyright © 2021 Genesys. All rights reserved.
//

import Foundation
import XCTest

struct Config: Codable {
    public let agentToken: String
    public let agentEmail: String
    public let agentId: String
    public let apiBaseAddress: String
    public let deploymentId: String
    public let domain: String
}

struct TestConfig {

    static let shared = TestConfig()
    var config: Config?
    var isUsingiPhone: Bool {
        return UIDevice.current.userInterfaceIdiom == .phone
    }

    init() {
        let url = Bundle.main.bundleURL.appendingPathComponent("PlugIns/iosAppTests.xctest/config.json")
        do {
            let data = try Data(contentsOf: url)
            config = try JSONDecoder().decode(Config.self, from: data)
        } catch {
            XCTFail("Cannot load test config.")
        }
    }

    func pullTestPng() -> UIImage? {
        let url = Bundle.main.bundleURL.appendingPathComponent("PlugIns/iosAppTests.xctest/testPng.png")
        do {
            let data = try Data(contentsOf: url)
            return UIImage(data: data)
        } catch {
            XCTFail("Cannot load test PNG.")
            return nil
        }
    }
}
