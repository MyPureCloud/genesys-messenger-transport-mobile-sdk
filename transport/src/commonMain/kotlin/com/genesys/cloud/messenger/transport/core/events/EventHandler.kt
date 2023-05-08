package com.genesys.cloud.messenger.transport.core.events

internal interface EventHandler {
    var eventListener: ((Event) -> Unit)?

    fun onEvent(event: Event?)
}
