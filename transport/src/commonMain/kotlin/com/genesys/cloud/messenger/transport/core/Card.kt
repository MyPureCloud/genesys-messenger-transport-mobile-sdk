package com.genesys.cloud.messenger.transport.core

import kotlinx.serialization.Serializable

@Serializable
data class Card(
    val title: String,
    val description: String? = null,
    val image: String? = null,
    val defaultAction: Action? = null,
    val actions: List<Action> = emptyList(),
)
