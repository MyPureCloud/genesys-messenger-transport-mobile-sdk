//
//  TestbedViewController.swift
//  iosApp
//
//  Created by Chris Rumpf on 10/1/21.
//  Copyright © 2021 Genesys. All rights reserved.
//

import UIKit
import MessengerTransport
import Combine

class TestbedViewController: UIViewController {

    private let messenger: MessengerInteractor
    private var attachImageName = ""
    private let attachmentName = "image"
    private var byteArray: [UInt8]? = nil
    private var customAttributes: [String: String] = [:]
    private var cancellables = Set<AnyCancellable>()
    
    init(messenger: MessengerInteractor) {
        self.messenger = messenger
        super.init(nibName: nil, bundle: nil)
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    /**
     User commands for the test bed app.
     */
    enum UserCommand: String, CaseIterable {
        case connect
        case connectAuthenticated
        case newChat
        case send
        case history
        case selectAttachment
        case attach
        case detach
        case deployment
        case bye
        case healthCheck
        case clearConversation
        case addAttribute
        case typing
        case oktaSignIn
        case oktaSignInWithPKCE
        case oktaLogout
        case authenticate

        var helpDescription: String {
            switch self {
            case .send: return "send <msg>"
            case .detach: return "detach <attachmentId>"
            case .addAttribute: return "addAttribute <key> <value>"
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
        view.placeholder = "Send a command"
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
        
        // Subscribe to MessengerInteractor Subjects
        messenger.stateChangeSubject
            .sink(receiveValue: updateWithStateChange)
            .store(in: &cancellables)
        messenger.messageEventSubject
            .sink(receiveValue: updateWithMessageEvent)
            .store(in: &cancellables)
        messenger.eventSubject
            .sink(receiveValue: updateWithEvent)
            .store(in: &cancellables)
    }
    
    private func configureAutoLayout() {
        content.leftAnchor.constraint(equalTo: view.leftAnchor).isActive = true
        content.rightAnchor.constraint(equalTo: view.rightAnchor).isActive = true
        content.topAnchor.constraint(equalTo: view.topAnchor).isActive = true
    }
    
    private func updateWithStateChange(_ stateChange: StateChange) {
        let newState = stateChange.newState
        var stateMessage = "\(newState)"
        switch newState {
        case is MessagingClientState.Connecting:
            stateMessage = "Connecting"
        case is MessagingClientState.Connected:
            stateMessage = "Connected"
        case let configured as MessagingClientState.Configured:
            stateMessage = "Configured, connected=\(configured.connected) newSession=\(configured.newSession) wasReconnecting=\(stateChange.oldState is MessagingClientState.Reconnecting)"
        case let closing as MessagingClientState.Closing:
            stateMessage = "Closing, code=\(closing.code) reason=\(closing.reason)"
        case let closed as MessagingClientState.Closed:
            stateMessage = "Closed, code=\(closed.code) reason=\(closed.reason)"
        case let error as MessagingClientState.Error:
            stateMessage = "Error, code=\(error.code) message=\(error.message?.description ?? "nil")"
        case is MessagingClientState.Reconnecting:
            stateMessage = "Reconnecting"
        case is MessagingClientState.ReadOnly:
            stateMessage = "ReadOnly"
        default:
            break
        }
        status.text = "Messenger Status: " + stateMessage
        info.text = "State changed from \(stateChange.oldState) to \(newState)"
    }
    
    private func updateWithMessageEvent(_ message: MessageEvent) {
        var displayMessage = "Unexpected message event: \(message)"
        switch message {
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
        info.text = displayMessage
    }
    
    private func updateWithEvent(_ event: Event) {
        var displayEvent = "Unexpected event: \(event)"
        switch event {
        case let typing as Event.AgentTyping:
            displayEvent = "Event received: \(typing.description)"
        case let error as Event.Error:
            displayEvent = "Event received: \(error.description)"
        case let healthChecked as Event.HealthChecked:
            displayEvent = "Event received: \(healthChecked.description)"
        case let conversationAutostart as Event.ConversationAutostart:
            displayEvent = "Event received: \(conversationAutostart.description)"
        case let connectionClosed as Event.ConnectionClosed:
            displayEvent = "Event received: \(connectionClosed.description)"
        case let authenticated as Event.Authenticated:
            displayEvent = "Event received: \(authenticated.description)"
        default:
            break
        }
        info.text = displayEvent
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

    private func segmentUserInput(_ input: String) -> (String?, String?) {
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

    private func convertToCommand(command: String?, input: String?) -> (UserCommand?, String?) {
        if(command == nil) {
            return (nil, input)
        }
        return (UserCommand(rawValue: command!), input)
    }

}

// MARK: UITextFieldDelegate
extension TestbedViewController : UITextFieldDelegate {
    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        guard let message = textField.text else {
            textField.resignFirstResponder()
            return true
        }

        attachImageName = ""
        let userInput = segmentUserInput(message)
        let command = convertToCommand(command: userInput.0,input: userInput.1)

        do {
            switch command {
            case (.connect, _):
                try messenger.connect()
            case (.connectAuthenticated, _):
                self.info.text = "Should perform connect authenticated command."
            case (.newChat, _):
                try messenger.newChat()
            case (.bye, _):
                try messenger.disconnect()
            case (.send, let msg?):
                try messenger.sendMessage(text: msg.trimmingCharacters(in: .whitespaces), customAttributes: customAttributes)
                customAttributes = [:]
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
            case(.addAttribute, let msg?):
                let segments = segmentUserInput(msg)
                if let key = segments.0, !key.isEmpty {
                    let value = segments.1 ?? ""
                    customAttributes[key] = value
                    self.info.text = "Custom attribute added: key: \(key) value: \(value)"
                }  else {
                    self.info.text = "Custom attribute key cannot be nil or empty!"
                }
            case (.typing, _):
                try messenger.indicateTyping()
            case (.oktaSignIn, _):
                self.info.text = "Should perform okta sign in command."
            case (.oktaSignInWithPKCE, _):
                self.info.text = "Should perform okta sign in with PKCE command."
            case (.oktaLogout, _):
                self.info.text = "Should perform okta logout command."
            case (.authenticate, _):
                self.info.text = "Should perform authenticate command."
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
