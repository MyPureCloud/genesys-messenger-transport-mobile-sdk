package com.genesys.cloud.messenger.transport.network

import io.ktor.client.engine.HttpClientEngine

internal expect fun createPlatformHttpEngine(): HttpClientEngine