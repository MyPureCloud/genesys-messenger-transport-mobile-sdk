//
//  PropertiesResource.swift
//  iosApp
//
//  Created by Chris Rumpf on 11/17/22.
//  Copyright © 2022 Genesys. All rights reserved.
//

import Foundation

enum PropertiesResourceError: Error {
    case resourceNotFound(String)
}

struct PropertiesResource {
    let name: String
    let properties: [String: String]
    
    init(resourceName: String) throws {
        self.properties = try PropertiesResource.dictionary(forResource: resourceName)
        self.name = resourceName
    }
    
    static func dictionary(forResource name: String) throws -> [String: String] {
        try readFile(name, type: "properties")
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
            throw PropertiesResourceError.resourceNotFound("Resource \(name).\(type) not found in main bundle.")
        }
        return try String(contentsOfFile: file, encoding: .utf8)
    }
}
