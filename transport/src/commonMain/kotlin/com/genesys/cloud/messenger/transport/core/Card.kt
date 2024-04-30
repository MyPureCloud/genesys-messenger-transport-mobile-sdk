package com.genesys.cloud.messenger.transport.core

import kotlinx.serialization.Serializable


@Serializable
data class Card(
    val actions: List<Action> = emptyList(),
    val defaultAction: DefaultAction,
    val description: String,
    val image: String,
    val title: String
)
