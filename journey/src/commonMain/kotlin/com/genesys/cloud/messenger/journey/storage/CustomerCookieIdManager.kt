package com.genesys.cloud.messenger.journey.storage

import com.genesys.cloud.messenger.journey.util.logs.Log
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal class CustomerCookieIdManager(
    private val storage: CookieIdStorage,
    private val log: Log,
) {
    @OptIn(ExperimentalUuidApi::class)
    fun getOrCreateCookieId(): String {
        val existing = storage.getCustomerCookieId()
        if (existing != null) {
            log.i { "Reusing existing CustomerCookieId" }
            storage.setCustomerCookieId(existing)
            return existing
        }
        val newId = Uuid.random().toString()
        log.i { "Generated new CustomerCookieId" }
        storage.setCustomerCookieId(newId)
        return newId
    }
}
