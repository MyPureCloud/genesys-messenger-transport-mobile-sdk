//
//  ApiHelper.swift
//  MessengerUITests
//
//  Created by Morehouse, Matthew on 6/2/21.
//  Copyright © 2021 Genesys. All rights reserved.
//

import Foundation
import XCTest

public class ApiHelper {

    static let shared = ApiHelper()
    private let requestToken = TestConfig.shared.config?.agentToken ?? ""
    private let apiBaseAddress = TestConfig.shared.config?.apiBaseAddress ?? ""

    private func waitForApi(expectation: XCTestExpectation, delay: Double = 60, file: StaticString = #file, line: UInt = #line) {
        let result = XCTWaiter().wait(for: [expectation], timeout: delay)
        if result != .completed {
            XCTFail("The API request did not complete within the expected time.", file: file, line: line)
        }
    }

    func publicAPICall(httpMethod: String, httpURL: String, jsonBody: Any? = nil, delay: Int = 10, file: StaticString = #file, line: UInt = #line) -> JsonDictionary? {
        if let jsonBody = jsonBody {
            print("Sending a \(httpMethod) to \(httpURL) with the following json body:\n\(jsonBody)")
            do {
                let data = try JSONSerialization.data(withJSONObject: jsonBody, options: JSONSerialization.WritingOptions())
                return publicAPICall(httpMethod: httpMethod, httpURL: httpURL, jsonData: data, delay: delay, file: file, line: line)
            } catch {
                XCTFail("There was an issue parsing the json body that was going to be sent to \(httpURL)")
                return nil
            }
        } else {
            print("Sending a \(httpMethod) to \(httpURL) with no json body.")
            return publicAPICall(httpMethod: httpMethod, httpURL: httpURL, jsonData: nil, delay: delay, file: file, line: line)
        }
    }

    func publicAPICall(httpMethod: String, url: String, imageData: Data, delay: Int = 10, file: StaticString = #file, line: UInt = #line) -> JsonDictionary? {
        print("Sending a \(httpMethod) to \(url) with an image.")
        boundary = "----Boundary-\(UUID().uuidString)"
        var data = Data()
        let header = "--\(boundary)\r\nContent-Disposition: form-data; name=\"file\"; filename=\"testPng.png\"\r\nContent-Type: image/png\r\n\r\n"
        let footer = "\r\n--\(boundary)--\r\n"
        guard let headerData = header.data(using: String.Encoding.ascii), let footerData = footer.data(using: String.Encoding.ascii) else {
            XCTFail("Failed to parse data.")
            return nil
        }
        data.append(headerData)
        data.append(imageData)
        data.append(footerData)
        return publicAPICall(httpMethod: httpMethod, url: url, formData: data, delay: delay, file: file, line: line)
    }

    func publicAPICall_Array(httpMethod: String, httpURL: String, jsonBody: Any? = nil, delay: Int = 10, file: StaticString = #file, line: UInt = #line) -> [JsonDictionary]? {
        if let jsonBody = jsonBody {
            print("Sending a \(httpMethod) to \(httpURL) with the following json body:\n\(jsonBody)")
            do {
                let data = try JSONSerialization.data(withJSONObject: jsonBody, options: JSONSerialization.WritingOptions())
                return publicAPICall(httpMethod: httpMethod, httpURL: httpURL, jsonData: data, delay: delay, file: file, line: line)
            } catch {
                XCTFail("There was an issue parsing the json body that was going to be sent to \(httpURL)")
                return nil
            }
        } else {
            print("Sending a \(httpMethod) to \(httpURL) with no json body.")
            return publicAPICall(httpMethod: httpMethod, httpURL: httpURL, jsonData: nil, delay: delay, file: file, line: line)
        }
    }

    var boundary = ""

    fileprivate func publicAPICall<T>(httpMethod: String, httpURL: String = "", url: String? = nil, jsonData: Data? = nil, formData: Data? = nil, delay: Int = 10, file: StaticString = #file, line: UInt = #line) -> T? {
        // Formatting the request.
        let finalString = url ?? "\(apiBaseAddress)\(httpURL)"
        guard let finalUrl = URL(string: finalString) else {
            XCTFail("Failed get the URL.")
            return nil
        }
        var request = URLRequest(url: finalUrl)
        request.httpMethod = httpMethod
        request.setValue("bearer " + requestToken, forHTTPHeaderField: "Authorization")
        request.setValue("ios-MMSDK-Testing", forHTTPHeaderField: "User-Agent")
        if let jsonData = jsonData {
            request.setValue("application/json; charset=utf-8", forHTTPHeaderField: "Content-Type")
            request.httpBody = jsonData
        } else if let formData = formData {
            request.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")
            request.httpBody = formData
        }

        // Sending the request
        var json: T?
        var requestResults = (invalidToken: false, serverError: false)
        let expectation = XCTestExpectation(description: "Wait for API task to finish.")
        URLSession.shared.dataTask(with: request, completionHandler: { (data, response, _) in
            do {
                // If the saved token was used and is invalid, specify that happened in the json variable.
                if let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 401 {
                    requestResults.invalidToken = true
                }

                // If a 500 error occurred, then retry the request one time.
                if let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode >= 500 && httpResponse.statusCode < 600 {
                    print("Server side error was received.")
                    if let data = data {
                        print(try JSONSerialization.jsonObject(with: data, options: []) as? T ?? "No json.")
                    }
                    requestResults.serverError = true
                }

                // Error handling for non 200 return codes.
                if let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode != 200 && !requestResults.invalidToken, let data = data {
                    if let jsonString = NSString(data: data, encoding: String.Encoding.utf8.rawValue) {
                        print(jsonString)
                    }
                }
                if !requestResults.invalidToken && !requestResults.serverError, let data = data { // Delete endpoints don't return a payload.
                    json = try JSONSerialization.jsonObject(with: data, options: []) as? T // parsing JSON response into a dictionary
                }
                expectation.fulfill()
            } catch {
                print("There was no json payload returned.")
                expectation.fulfill()
            }
        }).resume()
        waitForApi(expectation: expectation, file: file, line: line)

        if requestResults.invalidToken {
            XCTFail("There was an issue with the saved token. Replace the existing token and start tests again.", file: file, line: line)
        }
        return json
    }
}
