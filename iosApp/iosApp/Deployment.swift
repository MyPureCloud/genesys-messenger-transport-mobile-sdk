//
//  Deployment.swift
//  iosApp
//
//  Created by Chris Rumpf on 11/10/21.
//  Copyright © 2021 Genesys. All rights reserved.
//

import Foundation

enum DeploymentError: Error {
    case fileNotFound(String)
}

struct Deployment {

    private enum PropertyKeys: String {
        case deploymentId
        case deploymentDomain
    }

    var deploymentId: String? { properties[PropertyKeys.deploymentId.rawValue]}
    var domain: String? { properties[PropertyKeys.deploymentDomain.rawValue] }

    private let properties: [String: String]

    init() throws {
        properties = try Deployment.readFile("deployment", type: "properties")
            .components(separatedBy: .newlines)
            .reduce(into: [:]) { result, line in
                // The key contains all of the characters in the line starting with the first non-white space character and up to, but not including, the first unescaped '=', ':', or white space character other than a line terminator. All of these key termination characters may be included in the key by escaping them with a preceding backslash character
                // [^\s]      Matches a non-whitespace character
                // \w+        Matches one or more word characters
                // (?<!\\)    Matches if the preceding character is not a backslash
                // (?:\\\\)*  Matches any number of occurrences of two backslashes
                // [=: \t]    Matches a '=', ':', ' ', or tab
                let pattern = #"[^\s]\w+(?<!\\)(?:\\\\)*[=: \t]"#
                guard let range = line.range(of: pattern, options: .regularExpression) else {
                    let key = line.trimmingCharacters(in: .whitespaces)
                    if !key.isEmpty {
                        result[key] = ""
                    }
                    return
                }
                let key = String(line[..<range.upperBound].dropLast()).trimmingCharacters(in: .whitespaces)
                let value = String(line[range.upperBound...]).trimmingCharacters(in: .whitespaces)
                result[key] = value
            }
    }

    private static func readFile(_ name: String, type: String) throws -> String {
        guard let file = Bundle.main.path(forResource: name, ofType: type) else {
            throw DeploymentError.fileNotFound("File \(name).\(type) not found.")
        }
        return try String(contentsOfFile: file, encoding: .utf8)
    }

}
