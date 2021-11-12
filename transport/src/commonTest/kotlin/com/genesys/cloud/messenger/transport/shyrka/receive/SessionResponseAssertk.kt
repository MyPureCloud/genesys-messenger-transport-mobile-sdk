package com.genesys.cloud.messenger.transport.shyrka.receive

import assertk.Assert
import assertk.assertions.prop

internal fun Assert<SessionResponse>.connected() = prop("connected", SessionResponse::connected)

internal fun Assert<SessionResponse>.newSession() = prop("newSession", SessionResponse::newSession)
