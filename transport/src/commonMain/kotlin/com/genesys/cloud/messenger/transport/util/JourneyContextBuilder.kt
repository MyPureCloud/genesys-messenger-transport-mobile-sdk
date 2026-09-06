package com.genesys.cloud.messenger.transport.util

import com.genesys.cloud.messenger.transport.core.JourneyContextInfo
import com.genesys.cloud.messenger.transport.shyrka.send.JourneyContext
import com.genesys.cloud.messenger.transport.shyrka.send.JourneyCustomer
import com.genesys.cloud.messenger.transport.shyrka.send.JourneyCustomerSession
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogMessages

private const val COOKIE_ID_TYPE = "cookie"
private const val APP_SESSION_TYPE = "app"

/**
 * Invokes [provider] (if non-null) and returns its [JourneyContextInfo]. Returns `null`
 * when no provider is supplied, when the provider yields `null`, or when the provider
 * throws — in which case the exception is logged at warning level.
 */
internal fun journeyContextInfoOrNull(
    provider: (() -> JourneyContextInfo?)?,
    log: Log? = null,
): JourneyContextInfo? {
    if (provider == null) return null
    return try {
        provider()
    } catch (e: Exception) {
        log?.w { LogMessages.journeyContextProviderFailed(e) }
        null
    }
}

/**
 * Maps [journeyContextInfoOrNull] to the internal [JourneyContext] wire DTO.
 *
 * `customer.idType` is always `"cookie"`. `customerSession.type` is always `"app"`
 * and `customerSession` itself is omitted when [JourneyContextInfo.sessionId] is `null`.
 */
internal fun buildJourneyContext(
    provider: (() -> JourneyContextInfo?)?,
    log: Log? = null,
): JourneyContext? {
    return journeyContextInfoOrNull(provider, log)?.let {
        JourneyContext(
            customer = JourneyCustomer(id = it.customerCookieId, idType = COOKIE_ID_TYPE),
            customerSession = it.sessionId?.let { sid -> JourneyCustomerSession(id = sid, type = APP_SESSION_TYPE) },
        )
    }
}
