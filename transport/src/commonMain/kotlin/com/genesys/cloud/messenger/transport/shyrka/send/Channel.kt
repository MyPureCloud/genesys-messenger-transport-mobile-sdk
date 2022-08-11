package com.genesys.cloud.messenger.transport.shyrka.send

import kotlinx.serialization.Serializable

@Serializable
internal data class Channel(val metadata: Metadata) {
    @Serializable
    internal data class Metadata(
        @Serializable
        val customAttributes: Map<String, String>,
    )
}
