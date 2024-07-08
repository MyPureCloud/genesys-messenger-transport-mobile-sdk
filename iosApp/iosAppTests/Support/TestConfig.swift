//
//  TestConfig.swift
//  MessengerUITests
//
//  Created by Morehouse, Matthew on 6/2/21.
//  Copyright © 2021 Genesys. All rights reserved.
//

import Foundation
import XCTest
import MessengerTransport

struct Config: Codable {
    public let agentToken: String
    public let agentEmail: String
    public let agentId: String
    public let agentName: String
    public let expectedAvatarUrl: String
    public let apiBaseAddress: String
    public let deploymentId: String
    public let humanizeDisableDeploymentId: String
    public let botDeploymentId: String
    public let agentDisconnectDeploymentId: String
    public let botName: String
    public let domain: String
    public let authCode: String
    public let authCode2: String
    public let authCode3: String
    public let authDeploymentId: String
    public let redirectUri: String
    public let oktaCodeVerifier: String
    public let quickReplyBot: String
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

    func pullConfigDataAsKotlinByteArray() -> KotlinByteArray? {
        let url = Bundle.main.bundleURL.appendingPathComponent("PlugIns/iosAppTests.xctest/config.json")
        do {
            let data = (try Data(contentsOf: url)) as NSData
            let swiftByteArray: [UInt8] = data.toByteArray()
            let intArray : [Int8] = swiftByteArray
                .map { Int8(bitPattern: $0) }
            let kotlinByteArray: KotlinByteArray = KotlinByteArray.init(size: Int32(swiftByteArray.count))
            for (index, element) in intArray.enumerated() {
                kotlinByteArray.set(index: Int32(index), value: element)
            }
            return kotlinByteArray
        } catch {
            XCTFail("Cannot load test config or failed to parse into a KotlinByteArray")
            return nil
        }
    }
}

extension NSData {
    func toByteArray() -> [UInt8] {
        let count = self.length / MemoryLayout<Int8>.size
        var bytes = [UInt8](repeating: 0, count: count)

        self.getBytes(&bytes, length:count * MemoryLayout<Int8>.size)

        return bytes
    }
}
