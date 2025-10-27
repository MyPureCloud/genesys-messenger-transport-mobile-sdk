package com.genesys.cloud.messenger.transport.shyrka.receive

import assertk.Assert
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import io.ktor.http.HttpStatusCode

internal fun Assert<WebMessagingMessage<*>>.type() = prop("type", WebMessagingMessage<*>::type)

internal fun Assert<WebMessagingMessage<*>>.code() = prop("code", WebMessagingMessage<*>::code)

internal fun Assert<WebMessagingMessage<*>>.hasCode(expected: HttpStatusCode) = code().isEqualTo(expected.value)

internal fun Assert<WebMessagingMessage<*>>.body() = prop("body", WebMessagingMessage<*>::body)
