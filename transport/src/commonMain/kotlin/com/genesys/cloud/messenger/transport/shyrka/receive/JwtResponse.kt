package com.genesys.cloud.messenger.transport.shyrka.receive

import kotlinx.serialization.Serializable

@Serializable
internal data class JwtResponse(val jwt: String = "", val exp: Long = 0)
