import Foundation

// MARK: - Inbound envelope

struct WebMessagingMessage: Decodable, Sendable {
    let type: String
    let code: Int
    let body: AnyCodable
    let tracingId: String?

    enum CodingKeys: String, CodingKey {
        case type, code, body, tracingId
        case messageClass = "class"
    }

    let messageClass: String

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        type = try container.decode(String.self, forKey: .type)
        code = try container.decode(Int.self, forKey: .code)
        messageClass = try container.decode(String.self, forKey: .messageClass)
        tracingId = try container.decodeIfPresent(String.self, forKey: .tracingId)
        body = try container.decode(AnyCodable.self, forKey: .body)
    }
}

struct AnyCodable: Decodable, @unchecked Sendable {
    let value: Any

    init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        if let string = try? container.decode(String.self) {
            value = string
        } else if let dict = try? container.decode([String: AnyCodable].self) {
            value = dict.mapValues { $0.value }
        } else if let array = try? container.decode([AnyCodable].self) {
            value = array.map { $0.value }
        } else if let bool = try? container.decode(Bool.self) {
            value = bool
        } else if let int = try? container.decode(Int.self) {
            value = int
        } else if let double = try? container.decode(Double.self) {
            value = double
        } else {
            value = NSNull()
        }
    }
}

// MARK: - Session response body

struct SessionResponseBody: Decodable {
    let connected: Bool
    let newSession: Bool?
    let readOnly: Bool?
    let maxCustomDataBytes: Int?
}

// MARK: - Structured message body (inbound agent message)

struct StructuredMessageBody: Decodable {
    let text: String?
    let direction: String?
    let id: String?
    let type: String?
    let originatingEntity: String?
    let channel: StructuredChannel?
}

struct StructuredChannel: Decodable {
    let time: String?
    let messageId: String?
    let type: String?
}

// MARK: - Outbound requests

struct ConfigureSessionRequest: Encodable {
    let token: String
    let deploymentId: String
    let startNew: Bool
    let tracingId: String
    let action: String = "configureSession"
}

struct OnMessageRequest: Encodable {
    let token: String
    let message: TextMessagePayload
    let tracingId: String
    let action: String = "onMessage"
}

struct TextMessagePayload: Encodable {
    let text: String
    let type: String = "Text"
}

struct EchoRequest: Encodable {
    let token: String
    let tracingId: String = "SGVhbHRoQ2hlY2tNZXNzYWdlSWQ="
    let action: String = "echo"
    let message: TextMessagePayload = TextMessagePayload(text: "ping")
}

struct CloseSessionRequest: Encodable {
    let token: String
    let closeAllConnections: Bool
    let tracingId: String
    let action: String = "closeSession"
}

struct AutoStartRequest: Encodable {
    let token: String
    let tracingId: String
    let action: String = "onMessage"
    let message: PresenceEventPayload = PresenceEventPayload()
}

struct PresenceEventPayload: Encodable {
    let events: [PresenceEvent] = [PresenceEvent()]
    let type: String = "Event"
}

struct PresenceEvent: Encodable {
    let eventType: String = "Presence"
    let presence: PresenceType = PresenceType()
}

struct PresenceType: Encodable {
    let type: String = "Join"
}

// MARK: - Connection closed event

struct ConnectionClosedEventBody: Decodable {
    let reason: String?
}
