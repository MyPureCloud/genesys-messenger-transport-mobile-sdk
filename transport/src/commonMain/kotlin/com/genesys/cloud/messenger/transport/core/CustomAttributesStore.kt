package com.genesys.cloud.messenger.transport.core

interface CustomAttributesStore {

    /**
     * Get existing custom attributes.
     */
    fun get(): Map<String, String>

    /**
     * Add new custom attributes to the existing custom attributes map.
     * Once added, they will be sent along with the next message or autostart event.
     * NOTE: in case of conflict between existing and new keys, the values of latest will be used.
     */
    fun add(customAttributes: Map<String, String>)
}
