package com.genesys.cloud.messenger.transport.core

import kotlinx.serialization.Serializable


@Serializable
data class DefaultAction(
    val type: String,
    val url: String
)
