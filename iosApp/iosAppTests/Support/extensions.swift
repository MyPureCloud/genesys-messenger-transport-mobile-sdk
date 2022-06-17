//
//  extensions.swift
//  MessengerUITests
//
//  Created by Morehouse, Matthew on 6/3/21.
//  Copyright © 2021 Genesys. All rights reserved.
//

import Foundation
import XCTest

public typealias JsonDictionary = [String: Any]

extension Dictionary where Key == String, Value == Any {

    func value(forKey: String) -> Any? {
        return self[forKey]
    }

    func value(forKeyPath: String, quiet: Bool = false) -> Any? {
        let completePath = forKeyPath.components(separatedBy: ".")
        var componentPath: [String: Any] = self
        var count = 1
        for component in completePath {
            if count == completePath.count {
                continue
            }
            guard let newPath = componentPath[component] as? [String: Any] else {
                if !quiet {
                    print("There was no path in the dictionary for \(forKeyPath).")
                }
                return nil
            }
            componentPath = newPath
            count += 1
        }

        guard let endOfPath = componentPath[completePath.last ?? ""] else {
            if !quiet {
                print("There was no path in the dictionary for \(forKeyPath).")
            }
            return nil
        }

        return endOfPath
    }
}
