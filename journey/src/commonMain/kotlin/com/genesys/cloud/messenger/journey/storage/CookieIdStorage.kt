package com.genesys.cloud.messenger.journey.storage

internal expect class CookieIdStorage() {

    fun getCustomerCookieId(): String?

    fun setCustomerCookieId(cookieId: String)
}
