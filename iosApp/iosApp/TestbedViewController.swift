//
//  TestbedViewController.swift
//  iosApp
//
//  Created by Chris Rumpf on 10/1/21.
//  Copyright © 2021 Genesys. All rights reserved.
//
import MobileCoreServices
import UIKit
import MessengerTransport
import Combine

class TestbedViewController: UIViewController {

    private let messenger: MessengerInteractor
    private var cancellables = Set<AnyCancellable>()
    private var pkceEnabled = false
    private var authCode: String? = nil
    private var authState: AuthState = AuthState.noAuth
    private var quickRepliesMap = [String: ButtonResponse]()

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
        case oktaSignIn
        case oktaSignInWithPKCE
        case oktaLogout
        case connect
        case connectAuthenticated
        case newChat
        case send
        case sendQuickReply
        case history
        case attach
        case refreshAttachment
        case detach
        case fileAttachmentProfile
        case deployment
        case bye
        case healthCheck
        case invalidateConversationCache
        case addAttribute
        case typing
        case authorize
        case clearConversation
        case removeToken
        case removeAuthRefreshToken
        case stepUp
        case wasAuthenticated
        case synchronizePush
        case unregisterPush

        var helpDescription: String {
            switch self {
            case .send: return "send <msg>"
            case .detach: return "detach <attachmentId>"
            case .addAttribute: return "addAttribute <key> <value>"
            case .sendQuickReply: return "sendQuickReply <quickReply>"
            case .refreshAttachment: return "refreshAttachment <attachmentId>"
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

    private let authStateView: UILabel = {
        let view = UILabel()
        view.numberOfLines = 0
        view.text = "Auth State"
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
    
    private let pushNotificationsStateView: UILabel = {
        let view = UILabel()
        view.numberOfLines = 0
        view.text = "Push Notifications state:"
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
        content.addArrangedSubview(authStateView)
        content.addArrangedSubview(pushNotificationsStateView)
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
            authStateView.text = "Auth State: \(authState)"
        
        registerForNotificationsObservers()
        updatePushNotificationsStateView()
    }

    private func signIn() {
        guard let authUrl = buildOktaAuthorizeUrl() else {
            authState = AuthState.error(errorCode: ErrorCode.AuthFailed.shared, message: "Failed to build Okta authorize URL.", correctiveAction: CorrectiveAction.ReAuthenticate.shared)
            updateAuthStateView()
            return
        }

        if UIApplication.shared.canOpenURL(authUrl) {
            UIApplication.shared.open(authUrl)
        }
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
        case let quickReplies as MessageEvent.QuickReplyReceived:
            quickRepliesMap =  Dictionary(uniqueKeysWithValues: quickReplies.message.quickReplies.map { ($0.text, $0) })
            displayMessage = "QuickReplyReceived: text: <\(quickReplies.message.text)> | quick reply options: <\(quickReplies.message.quickReplies)>"
        default:
            break
        }
        DispatchQueue.main.async {
            self.info.text = displayMessage
        }
    }
    
    private func updateWithEvent(_ event: Event) {
        var displayEvent = "Unexpected event: \(event)"
        switch event {
        case let typing as Event.AgentTyping:
            displayEvent = "Event received: \(typing.description)"
        case let error as Event.Error:
            if(error.errorCode is ErrorCode.AuthFailed
               || error.errorCode is ErrorCode.AuthLogoutFailed
               || error.errorCode is ErrorCode.RefreshAuthTokenFailure) {
                authState = AuthState.error(errorCode: error.errorCode, message: error.message, correctiveAction: error.correctiveAction)
                updateAuthStateView()
            }
            if(error.errorCode is ErrorCode.CustomAttributeSizeTooLarge) {
                displayEvent = "Custom attribute size is too large: \(error.description)"
            }
            displayEvent = "Event received: \(error.description)"
        case let healthChecked as Event.HealthChecked:
            displayEvent = "Event received: \(healthChecked.description)"
        case let conversationAutostart as Event.ConversationAutostart:
            displayEvent = "Event received: \(conversationAutostart.description)"
        case let connectionClosed as Event.ConnectionClosed:
            displayEvent = "Event received: \(connectionClosed.description)"
        case let authorized as Event.Authorized:
            authState = AuthState.authorized
            updateAuthStateView()
            displayEvent = "Event received: \(authorized.description)"
        case let logout as Event.Logout:
            authState = AuthState.loggedOut
            updateAuthStateView()
            displayEvent = "Event received: \(logout.description)"
        case let disconnect as Event.ConversationDisconnect:
            displayEvent = "Event received: \(disconnect.description)"
        case let signedIn as Event.SignedIn:
            displayEvent = "Event received: \(signedIn.description)"
        case let existingAuthSessionCleared as Event.ExistingAuthSessionCleared:
            displayEvent = "Event received: \(existingAuthSessionCleared.description)"
        default:
            break
        }
        DispatchQueue.main.async {
            self.info.text = displayEvent
        }
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

    func utiForMimeType(mimeType: String) -> String? {
        if let uti = UTTypeCreatePreferredIdentifierForTag(kUTTagClassMIMEType, mimeType as CFString, nil)?.takeRetainedValue() {
            return uti as String
        }
        print("Could not convert mime type to uti: \(mimeType)")
        return nil
    }

    private func buildOktaAuthorizeUrl() -> URL? {
        guard let plistPath = Bundle.main.path(forResource: "Okta", ofType: "plist"),
                let plistData = FileManager.default.contents(atPath: plistPath),
                let plistDictionary = try? PropertyListSerialization.propertyList(from: plistData, options: [], format: nil) as? [String: Any],
                let oktaDomain = plistDictionary["oktaDomain"] as? String,
                let clientId = plistDictionary["clientId"] as? String,
                let signInRedirectURI = plistDictionary["signInRedirectURI"] as? String,
                let scope = plistDictionary["scopes"] as? String,
                let oktaState = plistDictionary["oktaState"] as? String,
                let codeChallengeMethod = plistDictionary["codeChallengeMethod"] as? String,
                let codeChallenge = plistDictionary["codeChallenge"] as? String
        else {
            return nil
        }

        var urlComponents = URLComponents(string: "https://\(oktaDomain)/oauth2/default/v1/authorize")!
        urlComponents.queryItems = [
            URLQueryItem(name: "client_id", value: clientId),
            URLQueryItem(name: "response_type", value: "code"),
            URLQueryItem(name: "scope", value: scope),
            URLQueryItem(name: "redirect_uri", value: signInRedirectURI),
            URLQueryItem(name: "state", value: oktaState)
        ]

        if pkceEnabled {
            urlComponents.queryItems?.append(URLQueryItem(name: "code_challenge_method", value: codeChallengeMethod))
            urlComponents.queryItems?.append(URLQueryItem(name: "code_challenge", value: codeChallenge))
        }

        guard let url = urlComponents.url else {
            return nil
        }

        return URL(string: url.absoluteString)
    }

    func setAuthCode(_ authCode: String) {
        self.authCode = authCode
        authState = AuthState.authCodeReceived(authCode: authCode)
        updateAuthStateView()
    }
}

// MARK: UITextFieldDelegate
extension TestbedViewController : UITextFieldDelegate {
    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        guard let message = textField.text else {
            textField.resignFirstResponder()
            return true
        }

        let userInput = segmentUserInput(message)
        let command = convertToCommand(command: userInput.0,input: userInput.1)

        do {
            switch command {
            case (.connect, _):
                try messenger.connect()
            case (.connectAuthenticated, _):
                try messenger.connectAuthenticated()
            case (.newChat, _):
                try messenger.newChat()
            case (.bye, _):
                try messenger.disconnect()
            case (.send, let msg?):
                try messenger.sendMessage(text: msg.trimmingCharacters(in: .whitespaces))
            case (.sendQuickReply, let quickReply?):
                if let buttonResponse = quickRepliesMap[quickReply] {
                    try messenger.sendQuickReply(buttonResponse: buttonResponse)
                    quickRepliesMap.removeAll()
                } else {
                    self.info.text = "Selected quickReply option: \(quickReply) does not exist."
                }
            case (.history, _):
                messenger.fetchNextPage()
            case (.healthCheck, _):
                try messenger.sendHealthCheck()
            case (.attach, _):
                guard let fileAttachmentProfile = messenger.getFileAttachmentProfile() else {
                    self.info.text = "FileAttachmentProfile is not set. Can not launch file picker."
                    break
                }

                if (!fileAttachmentProfile.hasWildCard && fileAttachmentProfile.allowedFileTypes.isEmpty) {
                    self.info.text = "Allowed file types is empty. Can not launch file picker."
                    break
                }

                showDocumentPicker(fileAttachmentProfile: fileAttachmentProfile)
            case (.refreshAttachment, let attachId?):
                try messenger.refreshAttachmentUrl(attachId: attachId)
            case (.detach, let attachId?):
                try messenger.detachImage(attachId: attachId)
            case (.fileAttachmentProfile, _):
                self.info.text = "FileAttachmentProfile: <\(messenger.getFileAttachmentProfile())>"
            case (.deployment, _):
                messenger.fetchDeployment { deploymentConfig, error in
                    DispatchQueue.main.async {
                        if let error = error {
                            self.info.text = "<\(error.localizedDescription)>"
                            return
                        }
                        self.info.text = "<\(deploymentConfig?.description() ?? "Unknown deployment config")>"
                    }
                }
            case (.invalidateConversationCache, _):
                messenger.invalidateConversationCache()
            case (.addAttribute, let msg?):
                let segments = segmentUserInput(msg)

                if let key = segments.0, !key.isEmpty, let value = segments.1 {
                    let addSuccess = messenger.addCustomAttributes(customAttributes: [key: value])

                    if addSuccess {
                        self.info.text = "Custom attribute added: key: \(key) value: \(value)"
                    } else {
                        self.info.text = "Custom attribute cannot be added: key: \(key) value: \(value)"
                    }
                } else {
                    self.info.text = "Custom attribute key cannot be nil or empty!"
                }
            case (.typing, _):
                try messenger.indicateTyping()
            case (.oktaSignIn, _):
                pkceEnabled = false
                signIn()
            case (.oktaSignInWithPKCE, _):
                pkceEnabled = true
                signIn()
            case (.oktaLogout, _):
                try messenger.oktaLogout()
            case (.authorize, _):
                guard let plistPath = Bundle.main.path(forResource: "Okta", ofType: "plist"),
                        let plistData = FileManager.default.contents(atPath: plistPath),
                        let plistDictionary = try? PropertyListSerialization.propertyList(from: plistData, options: [], format: nil) as? [String: Any],
                        let signInRedirectURI = plistDictionary["signInRedirectURI"] as? String,
                        let codeVerifier: String? = pkceEnabled ? plistDictionary["codeVerifier"] as? String : nil
                else {
                    authState = AuthState.error(errorCode: ErrorCode.AuthFailed.shared, message: "Unable to read Okta.plist or missing required key", correctiveAction: CorrectiveAction.ReAuthenticate.shared)
                    updateAuthStateView()
                return true
                }
                
                messenger.authorize(authCode: self.authCode ?? "", redirectUri: signInRedirectURI, codeVerifier: codeVerifier)
            case (.clearConversation, _):
                try messenger.clearConversation()
            case (.removeToken, _):
                messenger.removeToken()
            case (.removeAuthRefreshToken, _):
                messenger.removeAuthRefreshToken()
            case (.stepUp, _):
                try messenger.stepUp()
            case (.wasAuthenticated, _):
                self.info.text = "wasAuthenticated: \(messenger.wasAuthenticated())"
            case (.synchronizePush, _):
                UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { (granted, error) in
                    DispatchQueue.main.async {
                        if granted {
                            self.updatePushNotificationsStateView(state: "Authorized, not registered")
                            guard UIApplication.shared.delegate is AppDelegate else {
                                print("Can't retrieve AppDelegate")
                                return
                            }
                            
                            print("Register for remote notifications")
                            UIApplication.shared.registerForRemoteNotifications()
                        } else {
                            print("Notifications Disabled")
                            self.showNotificationSettingsAlert()
                        }
                    }
                }
            case (.unregisterPush, _):
                unregisterPushNotifications()
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

    private func updateAuthStateView() {
        authStateView.text = "Auth State: \(authState)"
    }
    
    private func showNotificationSettingsAlert() {
        let alertController = UIAlertController(
            title: "Notifications Disabled",
            message: "To receive updates, please enable notifications in settings.",
            preferredStyle: .alert
        )
        
        alertController.addAction(UIAlertAction(title: "Cancel", style: .cancel, handler: nil))
        alertController.addAction(UIAlertAction(title: "Settings", style: .default) { _ in
            if let settingsURL = URL(string: UIApplication.openSettingsURLString) {
                if UIApplication.shared.canOpenURL(settingsURL) {
                    UIApplication.shared.open(settingsURL)
                }
            }
        })
        
        self.present(alertController, animated: true)
    }

}

enum AuthState {
    case noAuth
    case authCodeReceived(authCode: String)
    case authorized
    case loggedOut
    case error(errorCode: ErrorCode, message: String?, correctiveAction: CorrectiveAction)
}

extension TestbedViewController: UIDocumentPickerDelegate {

    func showDocumentPicker(fileAttachmentProfile: FileAttachmentProfile) {
        let documentPicker: UIDocumentPickerViewController

        if fileAttachmentProfile.hasWildCard {
            documentPicker = UIDocumentPickerViewController(documentTypes: ["public.content"], in: .import)
        } else {
            var utiArray: [String] = []
            for fileType in fileAttachmentProfile.allowedFileTypes {
                if let result = utiForMimeType(mimeType: fileType) {
                    utiArray.append(result)
                }
            }

            documentPicker = UIDocumentPickerViewController(documentTypes: utiArray, in: .import)
        }

        documentPicker.delegate = self
        present(documentPicker, animated: true, completion: nil)
    }

    func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
        guard let pickedURL = urls.first else { return }

        let fileName = pickedURL.lastPathComponent

        DispatchQueue.global().async {
            do {
                let fileData = try Data(contentsOf: pickedURL, options: .mappedIfSafe)
                let kotlinByteArray = TransportUtil().nsDataToKotlinByteArray(data: fileData)
                DispatchQueue.main.async {
                    do {
                        try self.messenger.attachImage(kotlinByteArray: kotlinByteArray, fileName: fileName)
                    } catch {
                        self.info.text = "\(error.localizedDescription)"
                    }
                }
            } catch {
                DispatchQueue.main.async {
                    self.info.text = "Error converting file URL to ByteArray: \(error.localizedDescription)"
                }
            }
        }
    }

    func documentPickerWasCancelled(_ controller: UIDocumentPickerViewController) {
        self.info.text = "File selection canceled. No attachment selected."
    }
}

// MARK: Push Notifications
extension TestbedViewController {
    private func updatePushNotificationsStateView(state: String? = nil) {
        if let state {
            self.pushNotificationsStateView.text = "Push Notifications state: \(state)"
            return
        }
        
        UNUserNotificationCenter.current().getNotificationSettings(completionHandler: { permission in
            DispatchQueue.main.async { [weak self] in
                guard let self else { return }

                if permission.authorizationStatus != .authorized {
                    self.pushNotificationsStateView.text = "Push Notifications state: Unauthorized"
                } else {
                    var stateString = "Push Notifications state: "
                    stateString = stateString + (self.messenger.isDeploymentRegisteredForPush() ? "Registered" : "Unregistered")
                    pushNotificationsStateView.text = stateString
                }
            }
        })
    }
    
    private func registerForNotificationsObservers() {
        NotificationCenter.default.addObserver(self, selector: #selector(handleDeviceToken(_:)), name: Notification.Name.deviceTokenReceived, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(handleNotificationReceived(_:)), name: Notification.Name.notificationReceived, object: nil)
    }
    
    @objc func handleDeviceToken(_ notification: Notification) {
        guard let userInfo = notification.userInfo, let deviceToken = userInfo["token"] as? String else {
            print("Error: no device token")
            return
        }
        
        let pushService = messenger.createPushService()
        pushService.synchronize(deviceToken: deviceToken, pushProvider: .apns, completionHandler: { [weak self] error in
            if let error {
                print(error.localizedDescription)
            } else {
                print("Registration was successful")
                self?.messenger.updateDeploymentRegisteredForPush(state: true)
                self?.updatePushNotificationsStateView()
            }
        })
    }
    
    private func unregisterPushNotifications() {
        let pushService = messenger.createPushService()
        pushService.unregister(completionHandler: { [weak self] error in
            if let error {
                print(error.localizedDescription)
            } else {
                print("Unregistration was successful")
                self?.messenger.updateDeploymentRegisteredForPush(state: false)
                self?.updatePushNotificationsStateView()
            }
        })
    }
    
    // This will only be triggered after .bye command
    @objc func handleNotificationReceived(_ notification: Notification) {
        guard let userInfo = notification.userInfo else {
            print("Error: empty userInfo")
            return
        }
        
        guard UIApplication.shared.applicationState == .active else {
            print("App is not in foreground")
            return
        }
        
        guard let senderID = userInfo["deeplink"] as? String else {
            print("Sender ID not found")
            return
        }

        if senderID == "genesys-messaging" {
            showNotificationReceivedAlert(userInfo: userInfo)
        }
    }
    
    private func showNotificationReceivedAlert(userInfo: [AnyHashable: Any]) {
        if let aps = userInfo["aps"] as? [String: Any],
           let alert = aps["alert"] as? [String: Any],
           let title = alert["title"] as? String,
           let body = alert["body"] as? String {
            let alertController = UIAlertController(
                title: title,
                message: body,
                preferredStyle: .alert
            )
            
            alertController.addAction(UIAlertAction(title: "OK", style: .default, handler: nil))
            
            self.present(alertController, animated: true)
        } else {
            print("Error retrieving UserInfo")
        }
    }
}
