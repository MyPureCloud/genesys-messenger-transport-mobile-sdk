//
//  ContentViewController.swift
//  iosApp
//
//  Created by Chris Rumpf on 10/1/21.
//  Copyright © 2021 Genesys. All rights reserved.
//

import UIKit
import MessengerTransport

class ContentViewController: UIViewController {

    let messenger: MessengerHandler
    private var attachImageName = ""
    private let attachmentName = "image"
    private var byteArray: [UInt8]? = nil

    init(deployment: Deployment) {
        self.messenger = MessengerHandler(deployment: deployment)
        messenger.setOutputLabel(outputLabel: info)
        super.init(nibName: nil, bundle: nil)
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    private let content: UIStackView = {
        let view = UIStackView()
        view.translatesAutoresizingMaskIntoConstraints = false
        view.axis = .vertical
        view.alignment = .fill
        view.distribution = .fill
        view.spacing = 8
        view.isLayoutMarginsRelativeArrangement = true
        view.layoutMargins = UIEdgeInsets(top: view.layoutMargins.top, left: 8, bottom: view.layoutMargins.bottom, right: 8)
        return view
    }()

    private let heading: UILabel = {
        let view = UILabel()
        view.text = "Web Messaging Testbed"
        view.font = UIFont.preferredFont(forTextStyle: .headline)
        return view
    }()

    private let instructions: UILabel = {
        let view = UILabel()
        view.numberOfLines = 0
        view.text = """
        Commands:
            'connect'
            'configure'
            'send <msg>'
            'history'
            'selectAttachment'
            'attach'
            'detach <attachmentId>'
            'deployment'
            'bye'
            'healthCheck'
            'clearConversation'
        """
        view.font = UIFont.preferredFont(forTextStyle: .subheadline)
        return view
    }()

    private lazy var input: UITextField = {
        let view = UITextField()
        view.font = UIFont.preferredFont(forTextStyle: .body)
        view.borderStyle = .line
        view.placeholder = "Send a message"
        view.autocapitalizationType = .none
        view.autocorrectionType = .no
        view.accessibilityIdentifier = "Text-Field"
        view.delegate = self
        return view
    }()

    private let info: UILabel = {
        let view = UILabel()
        view.numberOfLines = 0
        view.text = "🚀"
        view.font = UIFont.preferredFont(forTextStyle: .body)
        return view
    }()

    override func viewDidLoad() {
        super.viewDidLoad()

        view.backgroundColor = UIColor(white: 0.99, alpha: 1.0)

        content.addArrangedSubview(heading)
        content.addArrangedSubview(instructions)
        content.addArrangedSubview(input)
        content.addArrangedSubview(info)

        view.addSubview(content)

        configureAutoLayout()

        messenger.setupSocketListeners()
        messenger.setupMessageListener()

        NotificationCenter.default.addObserver(forName: UIResponder.keyboardWillShowNotification, object: nil, queue: .main) { [weak self] notification in
            guard let self = self,
                  let keyboardFrameInfo = self.makeFrameInfo(notification: notification) else {
                return
            }

            let inputFrameInView = self.view.convert(self.input.frame, to: self.view)
            let originY = keyboardFrameInfo.0.origin.y < inputFrameInView.origin.y ? keyboardFrameInfo.0.origin.y - inputFrameInView.maxY : 0

            UIView.setAnimationCurve(keyboardFrameInfo.2)
            UIView.animate(withDuration: keyboardFrameInfo.1) {
                self.view.frame.origin.y = originY
            }
        }

        NotificationCenter.default.addObserver(forName: UIResponder.keyboardWillHideNotification, object: nil, queue: .main) { [weak self] notification in
            guard let self = self,
                  let keyboardFrameInfo = self.makeFrameInfo(notification: notification) else {
                return
            }

            UIView.setAnimationCurve(keyboardFrameInfo.2)
            UIView.animate(withDuration: keyboardFrameInfo.1) {
                self.view.frame.origin.y = 0
            }
        }
    }

    private func makeFrameInfo(notification: Notification) -> (CGRect, Double, UIView.AnimationCurve)? {
        guard let frame = notification.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? NSValue,
              let duration = notification.userInfo?[UIResponder.keyboardAnimationDurationUserInfoKey] as? NSNumber,
              let raw = notification.userInfo?[UIResponder.keyboardAnimationCurveUserInfoKey] as? Int,
              let curve = UIView.AnimationCurve(rawValue: raw)
        else {
            return nil
        }

        return (frame.cgRectValue, duration.doubleValue, curve)
    }

    private func configureAutoLayout() {
        content.leftAnchor.constraint(equalTo: view.leftAnchor).isActive = true
        content.rightAnchor.constraint(equalTo: view.rightAnchor).isActive = true
        content.topAnchor.constraint(equalTo: view.topAnchor).isActive = true
    }

    private func splitUserInput(_ input: String) -> (String?, String?) {
        let segments = input.split(separator: " ", maxSplits: 1)
        guard !segments.isEmpty else {
            return (nil, nil)
        }
        let command = String(segments[0])
        if segments.indices.contains(1) {
            return (command, String(segments[1]).trimmingCharacters(in: .whitespaces))
        }
        return (command, nil)
    }

}

// MARK: UITextFieldDelegate
extension ContentViewController : UITextFieldDelegate {
    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        guard let message = textField.text else {
            textField.resignFirstResponder()
            return true
        }

        attachImageName = ""
        let userInput = splitUserInput(message)

        switch userInput {
        case ("connect", _):
            messenger.connect()
        case ("bye", _):
            messenger.disconnect()
        case ("configure", _):
            messenger.configureSession()
        case ("send", let msg?):
            messenger.sendMessage(text: msg.trimmingCharacters(in: .whitespaces))
        case ("history", _):
            messenger.fetchNextPage()
        case ("healthCheck", _):
            messenger.sendHealthCheck()
        case ("selectAttachment", _):
            attachImageName = attachmentName
            DispatchQueue.global().async {
                let image = UIImage(named: self.attachmentName)
                guard let data = image?.pngData() as NSData? else { return }
                self.byteArray = data.toByteArray()
                DispatchQueue.main.async {
                    self.info.text = "<loaded \(String(describing: self.byteArray?.count)) bytes>"
                }
            }
        case ("attach", _):
            if(byteArray != nil) {
                let swiftByteArray : [UInt8] = byteArray!
                let intArray : [Int8] = swiftByteArray
                    .map { Int8(bitPattern: $0) }
                let kotlinByteArray: KotlinByteArray = KotlinByteArray.init(size: Int32(swiftByteArray.count))
                for (index, element) in intArray.enumerated() {
                    kotlinByteArray.set(index: Int32(index), value: element)
                }

                messenger.attachImage(kotlinByteArray: kotlinByteArray)
            }
        case ("detach", let attachId?):
            messenger.detachImage(attachId: attachId)
        case ("deployment", _):
            messenger.fetchDeployment()
        case ("clearConversation", _):
            messenger.clearConversation()
        default:
            break
        }
        self.input.text = ""
        textField.resignFirstResponder()
        return true
    }
}
