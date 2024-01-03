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
     * Returns true if add successful, false otherwise:
     * - False if new attributes are empty or same as existing
     * - False if combined attributes exceed maximum size limit
     * - True if attributes added to existing map successfully
     * NOTE: in case of conflict between existing and new keys, the values of latest will be used.
     */
    fun add(customAttributes: Map<String, String>): Boolean
}
