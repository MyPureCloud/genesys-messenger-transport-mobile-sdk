package com.genesys.cloud.messenger.transport.core

import kotlinx.serialization.Serializable


@Serializable
data class Action(
    val payload: String? = null,
    val text: String,
    val type: String,
    val url: String? = null
)
