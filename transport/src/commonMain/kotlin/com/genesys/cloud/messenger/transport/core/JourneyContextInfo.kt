package com.genesys.cloud.messenger.transport.core

/**
 * Journey context attached to guest and authenticated session-configure requests.
 *
 * @property customerCookieId the device-scoped customer cookie id. Always present when supplied.
 * @property sessionId the journey session id from a successful tracking event, or `null` if no
 *           tracking event has succeeded yet.
 */
data class JourneyContextInfo(
    val customerCookieId: String,
    val sessionId: String?,
)
