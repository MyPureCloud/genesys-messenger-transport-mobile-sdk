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
        
        super.init(nibName: nil, bundle: nil)
        
        // set up MessengerHandler callbacks
        
        messenger.onStateChange = { [weak self] state in
            var stateMessage = "\(state)"
            switch state {
            case _ as MessagingClientState.Connecting:
                stateMessage = "Connecting"
            case _ as MessagingClientState.Connected:
                stateMessage = "Connected"
            case let configured as MessagingClientState.Configured:
                stateMessage = "Configured, connected=\(configured.connected) newSession=\(configured.newSession?.description ?? "nil"))"
            case let closing as MessagingClientState.Closing:
                stateMessage = "Closing, code=\(closing.code) reason=\(closing.reason)"
            case let closed as MessagingClientState.Closed:
                stateMessage = "Closed, code=\(closed.code) reason=\(closed.reason)"
            case let error as MessagingClientState.Error:
                stateMessage = "Error, code=\(error.code) message=\(error.message?.description ?? "nil")"
            default:
                break
            }
            self?.status.text = "Messenger Status: " + stateMessage
            self?.info.text = "State changed to \(state)"
        }
        
        messenger.onMessageEvent = { [weak self] event in
            var displayMessage = "Unexpected message event: \(event)"
            switch event {
            case let messageInserted as MessageEvent.MessageInserted:
                displayMessage = "Message Inserted: \(messageInserted.message.description)"
            case let messageUpdated as MessageEvent.MessageUpdated:
                displayMessage = "Message Updated: \(messageUpdated.message.description)"
            case let attachmentUpdated as MessageEvent.AttachmentUpdated:
                displayMessage = "Attachment Updated: \(attachmentUpdated.attachment.description)"
            case let history as MessageEvent.HistoryFetched:
                displayMessage = "History Fetched: startOfConversation: <\(history.startOfConversation.description)>, messages: <\(history.messages.description)> "
                print(displayMessage)
            default:
                break
            }
            self?.info.text = displayMessage
        }
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    /**
     User commands for the test bed app.
     */
    enum UserCommand: String, CaseIterable {
        case connect
        case configure
        case send
        case history
        case selectAttachment
        case attach
        case detach
        case deployment
        case bye
        case healthCheck
        case clearConversation
        
        var helpDescription: String {
            switch self {
            case .send: return "send <msg>"
            case .detach: return "detach <attachmentId>"
            default: return rawValue
            }
        }
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
        view.text = "Commands: " + UserCommand.allCases.map { $0.helpDescription }.joined(separator: ", ")
        view.font = UIFont.preferredFont(forTextStyle: .caption1)
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
    
    private let status: UILabel = {
        let view = UILabel()
        view.numberOfLines = 0
        view.text = "Messenger Status: Idle"
        view.font = UIFont.preferredFont(forTextStyle: .callout)
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
        content.addArrangedSubview(input)
        content.addArrangedSubview(instructions)
        content.addArrangedSubview(status)
        content.addArrangedSubview(info)

        view.addSubview(content)

        configureAutoLayout()
        
        observeKeyboard()
    }
    
    private func configureAutoLayout() {
        content.leftAnchor.constraint(equalTo: view.leftAnchor).isActive = true
        content.rightAnchor.constraint(equalTo: view.rightAnchor).isActive = true
        content.topAnchor.constraint(equalTo: view.topAnchor).isActive = true
    }
        
    private func observeKeyboard() {
        NotificationCenter.default.addObserver(forName: UIResponder.keyboardWillShowNotification, object: nil, queue: .main) { [weak self] notification in
            guard let self = self,
                  let keyboardFrameInfo = self.makeFrameInfo(notification: notification) else {
                return
            }

            let inputFrameInView = self.view.convert(self.input.frame, to: self.view)
            let originY = keyboardFrameInfo.0.origin.y < inputFrameInView.origin.y ? keyboardFrameInfo.0.origin.y - inputFrameInView.maxY : 0

            UIViewPropertyAnimator(duration: keyboardFrameInfo.1, curve: keyboardFrameInfo.2) {
                self.view.frame.origin.y = originY
            }
            .startAnimation()
        }

        NotificationCenter.default.addObserver(forName: UIResponder.keyboardWillHideNotification, object: nil, queue: .main) { [weak self] notification in
            guard let self = self,
                  let keyboardFrameInfo = self.makeFrameInfo(notification: notification) else {
                return
            }

            UIViewPropertyAnimator(duration: keyboardFrameInfo.1, curve: keyboardFrameInfo.2) {
                self.view.frame.origin.y = 0
            }
            .startAnimation()
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

    private func segmentUserInput(_ input: String) -> (UserCommand?, String?) {
        let segments = input.split(separator: " ", maxSplits: 1)
        guard !segments.isEmpty else {
            return (nil, nil)
        }
        let command = UserCommand(rawValue: String(segments[0]))
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
        let userInput = segmentUserInput(message)

        do {
            switch userInput {
            case (.connect, _):
                try messenger.connect()
            case (.bye, _):
                try messenger.disconnect()
            case (.configure, _):
                try messenger.configureSession()
            case (.send, let msg?):
                try messenger.sendMessage(text: msg.trimmingCharacters(in: .whitespaces))
            case (.history, _):
                messenger.fetchNextPage()
            case (.healthCheck, _):
                try messenger.sendHealthCheck()
            case (.selectAttachment, _):
                attachImageName = attachmentName
                DispatchQueue.global().async {
                    let image = UIImage(named: self.attachmentName)
                    guard let data = image?.pngData() as NSData? else { return }
                    self.byteArray = data.toByteArray()
                    DispatchQueue.main.async {
                        self.info.text = "<loaded \(String(describing: self.byteArray?.count)) bytes>"
                    }
                }
            case (.attach, _):
                if(byteArray != nil) {
                    let swiftByteArray : [UInt8] = byteArray!
                    let intArray : [Int8] = swiftByteArray
                        .map { Int8(bitPattern: $0) }
                    let kotlinByteArray: KotlinByteArray = KotlinByteArray.init(size: Int32(swiftByteArray.count))
                    for (index, element) in intArray.enumerated() {
                        kotlinByteArray.set(index: Int32(index), value: element)
                    }
                    
                    try messenger.attachImage(kotlinByteArray: kotlinByteArray)
                }
            case (.detach, let attachId?):
                try messenger.detachImage(attachId: attachId)
            case (.deployment, _):
                messenger.fetchDeployment { deploymentConfig, error in
                    if let error = error {
                        self.info.text = "<\(error.localizedDescription)>"
                        return
                    }
                    self.info.text = "<\(deploymentConfig?.description() ?? "Unknown deployment config")>"
                }
            case (.clearConversation, _):
                messenger.clearConversation()
            default:
                self.info.text = "Invalid command"
            }
        } catch {
            self.info.text = error.localizedDescription
        }
        
        self.input.text = ""
        textField.resignFirstResponder()
        return true
    }
}
