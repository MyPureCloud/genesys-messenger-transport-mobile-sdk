package com.genesys.cloud.messenger.transport.network

import com.genesys.cloud.messenger.transport.util.logs.Log

internal expect class ReconnectionHandlerImpl(
    reconnectionTimeoutInSeconds: Long,
    log: Log,
) : ReconnectionHandler