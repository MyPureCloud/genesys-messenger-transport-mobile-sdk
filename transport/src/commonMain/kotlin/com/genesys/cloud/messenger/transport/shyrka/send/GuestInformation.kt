package com.genesys.cloud.messenger.transport.shyrka.send

import kotlinx.serialization.Serializable

@Serializable
internal data class GuestInformation(
    val email: String? = null,
    val phoneNumber: String? = null,
    val firstName: String? = null,
    val lastName: String? = null
)
