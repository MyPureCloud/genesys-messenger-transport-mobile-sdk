package com.genesys.cloud.messenger.transport.core

interface CustomAttributesStore {
    /**
     * Get existing custom attributes.
     */
    fun get(): Map<String, String>

    /**
     * Adds new custom string attributes to existing customAttributes map.
     * On success, attributes are merged with existing ones.
     * Updated attributes are sent with next message or autostart event.
     * Returns true if add successful, false otherwise.
     */
    fun add(customAttributes: Map<String, String>): Boolean
}
