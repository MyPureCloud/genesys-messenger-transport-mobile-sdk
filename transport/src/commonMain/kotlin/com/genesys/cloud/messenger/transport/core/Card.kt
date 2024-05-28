package com.genesys.cloud.messenger.transport.core

import kotlinx.serialization.Serializable

@Serializable
data class Card(
    val title: String,
    val image: String,
    val defaultResponse: Action? = null,
    val response: List<Action> = emptyList()
)
