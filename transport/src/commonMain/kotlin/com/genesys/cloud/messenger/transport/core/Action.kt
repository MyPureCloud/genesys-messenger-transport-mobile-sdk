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
}

/**
 * Represents a response that contains a link.
 *
 * @param text the text to display for the link.
 * @param url the URL the link points to.
 * @param type the type of bot response, which is `Type.Link` for this class.
 */
@Serializable
data class LinkAction(
    val text: String,
    val url: String,
) : Action(type = Type.Link)

/**
 * Represents a response that contains a postback action.
 *
 * @param text the text to display for the postback button.
 * @param payload the payload to send when the postback button is clicked.
 * @param type the type of bot response, which is `Type.Postback` for this class.
 */
@Serializable
data class PostbackAction(
    val text: String,
    val payload: String,
) : Action(type = Type.Postback)
