package com.genesys.cloud.messenger.transport.shyrka.receive

import assertk.Assert
import assertk.assertions.prop

internal fun Assert<StructuredMessage>.text() = prop("text") { it.text }

internal fun Assert<StructuredMessage>.type() = prop("type") { it.type }

internal fun Assert<StructuredMessage>.direction() = prop("direction") { it.direction }

internal fun Assert<StructuredMessage>.id() = prop("id") { it.id }

internal fun Assert<StructuredMessage>.channel() = prop("channel") { it.channel }

internal fun Assert<StructuredMessage.Channel>.time() = prop("time") { it.time }

internal fun Assert<StructuredMessage.Channel>.messageId() = prop("messageId") { it.messageId }
