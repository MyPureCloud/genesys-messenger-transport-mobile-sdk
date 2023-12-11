package com.genesys.cloud.messenger.transport.core

import com.genesys.cloud.messenger.transport.core.events.Event
import com.genesys.cloud.messenger.transport.core.events.EventHandler
import com.genesys.cloud.messenger.transport.util.logs.Log
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.core.toByteArray

internal class CustomAttributesStoreImpl(
    private val log: Log,
    private val eventHandler: EventHandler,
) : CustomAttributesStore {
    private var customAttributes: MutableMap<String, String> = mutableMapOf()
    private var maxCustomDataBytes: Int = 0

    internal var state: State = State.PENDING
        private set

    override fun get(): Map<String, String> = customAttributes

    override fun add(customAttributes: Map<String, String>) {
        if (isSizeExceeded(customAttributes)) {
            eventHandler.onEvent(Event.SizeLimitExceeded)
        } else {
            eventHandler.onEvent(Event.AttributesAdded(customAttributes))
            this.customAttributes.putAll(customAttributes)
            state = State.PENDING
            log.i { "add: $customAttributes | state = $state" }
        }
    }

    private fun isSizeExceeded(attributes: Map<String, String>): Boolean {
        val totalSize = attributes.entries.sumOf { (key, value) ->
            key.toByteArray(Charsets.UTF_8).size + value.toByteArray(Charsets.UTF_8).size
        }
        return totalSize > maxCustomDataBytes
    }

    internal fun updateMaxCustomDataBytes(maxBytes: Int) {
        this.maxCustomDataBytes = maxBytes
    }

    internal fun getCustomAttributesToSend(): Map<String, String> {
        return if (state == State.PENDING) {
            get()
        } else {
            emptyMap()
        }
    }

    internal fun onSending() {
        log.i { "onSending()" }
        state = State.SENDING
    }

    internal fun onSent() {
        log.i { "onSent. state = $state" }
        state = if (state == State.PENDING) State.PENDING else State.SENT
    }

    internal fun onError() {
        log.i { "onError()" }
        customAttributes.clear()
        state = State.ERROR
    }

    internal fun onMessageError() {
        log.i { "onMessageError()" }
        state = State.PENDING
    }

    internal fun onSessionClosed() {
        log.i { "onSessionClosed()" }
        state = State.PENDING
    }

    internal enum class State {
        PENDING,
        SENDING,
        SENT,
        ERROR,
    }
}
