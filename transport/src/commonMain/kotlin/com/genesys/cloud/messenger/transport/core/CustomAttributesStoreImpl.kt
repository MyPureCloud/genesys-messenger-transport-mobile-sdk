package com.genesys.cloud.messenger.transport.core

import com.genesys.cloud.messenger.transport.util.logs.Log

internal class CustomAttributesStoreImpl(private val log: Log) : CustomAttributesStore {
    private var customAttributes: MutableMap<String, String> = mutableMapOf()
    internal var state: State = State.PENDING
        private set

    override fun get(): Map<String, String> = customAttributes

    override fun add(customAttributes: Map<String, String>) {
        if (customAttributes.isEmpty() || this.customAttributes == customAttributes) return
        state = State.PENDING
        log.i { "add: $customAttributes | state = $state" }
        for ((key, value) in customAttributes) {
            this.customAttributes[key] = value
        }
    }

    internal fun getCustomAttributesToSend(): Map<String, String> =
        if (state == State.PENDING) get() else emptyMap()

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
