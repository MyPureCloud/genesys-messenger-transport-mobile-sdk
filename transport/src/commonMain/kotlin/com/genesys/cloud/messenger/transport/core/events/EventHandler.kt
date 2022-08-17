package com.genesys.cloud.messenger.transport.core.events

import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessageEvent

internal interface EventHandler {
    fun onEvent(event: StructuredMessageEvent)
}
