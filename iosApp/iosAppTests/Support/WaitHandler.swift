//
//  WaitHandler.swift
//  iosAppTests
//
//  Created by Morehouse, Matthew on 6/3/22.
//  Copyright © 2022 orgName. All rights reserved.
//

import XCTest

let Wait = WaitHandler()

class WaitHandler {

    func delay(_ time: Double) {
        let expectation = XCTestExpectation(description: "Delay expectation.")
        DispatchQueue(label: "Delay queue.").asyncAfter(deadline: .now() + time) {
            expectation.fulfill()
        }
        _ = XCTWaiter().wait(for: [expectation], timeout: time + 2)
    }

    public func queryUntilTrue(queryInfo: String = "", attempts: Int = 30, delayTime: Double = 1, canFailTest: Bool = true, line: UInt = #line, file: StaticString = #file, runCheck: () -> Bool) {
        var check = false
        for _ in 0..<attempts {
            if runCheck() {
                check = true
                break
            } else {
                delay(delayTime)
            }
        }
        if canFailTest && !check {
            XCTFail("The query was never true. Info: \(queryInfo)", file: file, line: line)
        }
    }
}
