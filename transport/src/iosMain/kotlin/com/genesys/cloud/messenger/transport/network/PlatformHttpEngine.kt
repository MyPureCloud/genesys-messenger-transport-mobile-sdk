package com.genesys.cloud.messenger.transport.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

internal actual fun createPlatformHttpEngine(): HttpClientEngine = Darwin.create {
    configureRequest {
        setAllowsCellularAccess(true)
        setAllowsConstrainedNetworkAccess(true)
        setAllowsExpensiveNetworkAccess(true)
    }
}