package com.genesys.cloud.messenger.transport.core

import kotlinx.serialization.Serializable

@Deprecated(
    "Unused. Card content received from the conversation is exposed via Message.cards (Message.Card) " +
        "and ButtonResponse. This type is never produced by the SDK and will be removed in 3.0.0.",
)
@Serializable
data class Card(
    val title: String,
    val description: String? = null,
    val image: String? = null,
    val defaultAction: Action? = null,
    val actions: List<Action> = emptyList(),
)
