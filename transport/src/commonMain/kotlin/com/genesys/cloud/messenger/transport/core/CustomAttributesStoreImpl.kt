package com.genesys.cloud.messenger.transport.core

import com.genesys.cloud.messenger.transport.core.events.Event
import com.genesys.cloud.messenger.transport.core.events.EventHandler
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogMessages
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.core.toByteArray

internal class CustomAttributesStoreImpl(
    private val log: Log,
    private val eventHandler: EventHandler,
) : CustomAttributesStore {
    private var customAttributes: MutableMap<String, String> = mutableMapOf()
    var maxCustomDataBytes: Int = 0

    internal var state: State = State.PENDING
        private set

    override fun get(): Map<String, String> = customAttributes

    override fun add(customAttributes: Map<String, String>): Boolean {
        if (!isCustomAttributesValid(customAttributes)) {
            return false
        }
        this.customAttributes.putAll(customAttributes)
        state = State.PENDING
        log.i { LogMessages.addCustomAttribute(customAttributes, state.name) }
        return true
    }

    private fun isCustomAttributesValid(customAttributes: Map<String, String>): Boolean {
        if (customAttributes.isEmpty() || this.customAttributes == customAttributes) {
            log.w { LogMessages.CUSTOM_ATTRIBUTES_EMPTY_OR_SAME }
            return false
        } else if (isSizeExceeded(customAttributes)) {
            eventHandler.onEvent(
                Event.Error(
                    ErrorCode.CustomAttributeSizeTooLarge,
                    ErrorMessage.customAttributesSizeError(maxCustomDataBytes),
                    CorrectiveAction.CustomAttributeSizeTooLarge
                )
            )
            log.e { LogMessages.CUSTOM_ATTRIBUTES_SIZE_EXCEEDED }
            return false
        }
        return true
    }

    private fun isSizeExceeded(attributes: Map<String, String>): Boolean {
        val totalSize = attributes.entries.sumOf { (key, value) ->
            key.toByteArray(Charsets.UTF_8).size + value.toByteArray(Charsets.UTF_8).size
        }
        return totalSize > maxCustomDataBytes
    }

    internal fun getCustomAttributesToSend(): Map<String, String> {
        return if (state == State.PENDING) {
            get()
        } else {
            emptyMap()
        }
    }

    internal fun onSending() {
        log.i { LogMessages.ON_SENDING }
        state = State.SENDING
    }

    internal fun onSent() {
        log.i { LogMessages.onSentState(state.name) }
        state = if (state == State.PENDING) State.PENDING else State.SENT
    }

    internal fun onError() {
        log.i { LogMessages.ON_ERROR }
        customAttributes.clear()
        state = State.ERROR
    }

    internal fun onMessageError() {
        log.i { LogMessages.ON_MESSAGE_ERROR }
        state = State.PENDING
    }

    internal fun onSessionClosed() {
        log.i { LogMessages.ON_SESSION_CLOSED }
        state = State.PENDING
    }

    internal enum class State {
        PENDING,
        SENDING,
        SENT,
        ERROR,
    }
}
