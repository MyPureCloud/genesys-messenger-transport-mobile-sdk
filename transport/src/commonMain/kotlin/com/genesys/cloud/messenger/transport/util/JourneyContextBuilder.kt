package com.genesys.cloud.messenger.transport.util

import com.genesys.cloud.messenger.transport.core.JourneyContextInfo
import com.genesys.cloud.messenger.transport.shyrka.send.JourneyContext
import com.genesys.cloud.messenger.transport.shyrka.send.JourneyCustomer
import com.genesys.cloud.messenger.transport.shyrka.send.JourneyCustomerSession

private const val COOKIE_ID_TYPE = "cookie"
private const val APP_SESSION_TYPE = "app"

/**
 * Invokes [provider] (if non-null) and maps its [JourneyContextInfo] result to the
 * internal [JourneyContext] wire DTO. Returns `null` when no provider is supplied
 * or when the provider yields `null` so that callers can omit the `journeyContext`
 * field from the serialized payload.
 *
 * `customer.idType` is always `"cookie"`. `customerSession.type` is always `"app"`
 * and `customerSession` itself is omitted when [JourneyContextInfo.sessionId] is `null`.
 */
internal fun buildJourneyContext(
    provider: (() -> JourneyContextInfo?)?,
): JourneyContext? =
    provider?.invoke()?.let { info ->
        JourneyContext(
            customer = JourneyCustomer(id = info.customerCookieId, idType = COOKIE_ID_TYPE),
            customerSession = info.sessionId?.let { JourneyCustomerSession(id = it, type = APP_SESSION_TYPE) },
        )
    }
