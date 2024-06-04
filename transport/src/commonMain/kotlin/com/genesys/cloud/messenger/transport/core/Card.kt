package com.genesys.cloud.messenger.transport.core

import kotlinx.serialization.Serializable

@Serializable
data class Card(
    val title: String,
    val description: String? = null,
    val image: String? = null,
    val defaultResponse: Action? = null,
    val responses: List<Action> = emptyList(),
)
