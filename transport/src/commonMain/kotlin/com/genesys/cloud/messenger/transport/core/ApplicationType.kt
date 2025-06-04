package com.genesys.cloud.messenger.transport.core

enum class ApplicationType {
    TRANSPORT_SDK {
        override fun toString(): String = "TransportSDK"
    },
    MESSENGER_SDK {
        override fun toString(): String = "MessengerSDK"
    };
}
