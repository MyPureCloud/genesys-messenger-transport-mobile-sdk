package com.genesys.cloud.messenger.transport.shyrka.send

internal interface WebMessagingRequest {
    val action: String
    val token: String
}
