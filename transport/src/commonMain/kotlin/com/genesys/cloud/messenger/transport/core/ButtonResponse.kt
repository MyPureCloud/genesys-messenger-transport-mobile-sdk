package com.genesys.cloud.messenger.transport.core

import kotlinx.serialization.Serializable

/**
 * Represents a single reply option that the user can select.
 * Can be used for reply to a quick reply, card action or a time slot picker.
 *
 * @param text the text to present to the user.
 * @param payload the payload data of the reply.
 * @param type the type of reply.
 * @param originatingMessageId the ID of the message which this reply is related to.
 */
@Serializable
data class ButtonResponse(
    val text: String,
    val payload: String,
    val type: String,
    val originatingMessageId: String? = null
) {

    constructor(text: String, payload: String, type: String) : this(text, payload, type, null)
}
