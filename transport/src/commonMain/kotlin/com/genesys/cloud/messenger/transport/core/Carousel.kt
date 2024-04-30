package com.genesys.cloud.messenger.transport.core

import kotlinx.serialization.Serializable


@Serializable
data class Carousel(
    val cards: List<Card>
)
