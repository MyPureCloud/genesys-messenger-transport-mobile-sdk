package com.genesys.cloud.messenger.transport.core

import kotlinx.serialization.Serializable

/**
 * Represents a single quick reply option that the user can select.
 *
 * @param text the quick reply text to present to the user.
 * @param payload the payload of the quick reply.
 * @param type the type of quick reply.
 */
@Serializable
data class ButtonResponse(
    val text: String,
    val payload: String,
    val type: String,
)
