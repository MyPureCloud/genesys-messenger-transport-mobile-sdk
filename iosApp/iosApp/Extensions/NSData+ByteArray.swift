//
//  NSData+ByteArray.swift
//  iosApp
//
//  Created by Tony Nguyen on 2021-07-15.
//  Copyright © 2021 Genesys. All rights reserved.
//

import Foundation

extension NSData {
    func toByteArray() -> [UInt8] {
        let count = self.length / MemoryLayout<Int8>.size
        var bytes = [UInt8](repeating: 0, count: count)

        self.getBytes(&bytes, length:count * MemoryLayout<Int8>.size)

        return bytes
    }
}
