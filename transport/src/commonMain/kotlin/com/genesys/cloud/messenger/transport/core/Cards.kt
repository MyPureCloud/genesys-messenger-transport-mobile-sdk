package com.genesys.cloud.messenger.transport.core

import kotlinx.serialization.Serializable


@Serializable
data class Cards(
    val body: Action,
    val `class`: String,
    val code: Int,
    val type: String
)
