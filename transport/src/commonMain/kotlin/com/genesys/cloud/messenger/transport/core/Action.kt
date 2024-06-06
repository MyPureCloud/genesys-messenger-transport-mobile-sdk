package com.genesys.cloud.messenger.transport.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a generic bot response.
 *
 * @param type the type of the bot response, which can be `Link`, or `Postback`.
 */
@Serializable
sealed class Action(
    val type: Type
) {
    enum class Type {
        Link,
        @SerialName("Button")
        Postback
    }

    /**
     * Represents a response that contains a link.
     *
     * @param text the text to display for the link.
     * @param url the URL the link points to.
     */
    @Serializable
    data class Link(
        val text: String,
        val url: String,
    ) : Action(type = Type.Link)

    /**
     * Represents a response that contains a postback action.
     *
     * @param text the text to display for the postback button.
     * @param payload the payload to send when the postback button is clicked.
     */
    @Serializable
    data class Postback(
        val text: String,
        val payload: String,
    ) : Action(type = Type.Postback)
}
