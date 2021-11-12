package com.genesys.cloud.messenger.transport.shyrka.send

import kotlinx.serialization.Serializable

@Serializable
internal data class JourneyContext(
    val customer: JourneyCustomer,
    val customerSession: JourneyCustomerSession,
    val triggeringAction: JourneyAction? = null
)

@Serializable
internal data class JourneyCustomer(
    val id: String,
    val idType: String
)

@Serializable
internal data class JourneyCustomerSession(
    val id: String,
    val type: String
)

@Serializable
internal data class JourneyAction(
    val id: String,
    val actionMap: JourneyActionMap
)

@Serializable
internal data class JourneyActionMap(
    val id: String,
    val version: Float
)
