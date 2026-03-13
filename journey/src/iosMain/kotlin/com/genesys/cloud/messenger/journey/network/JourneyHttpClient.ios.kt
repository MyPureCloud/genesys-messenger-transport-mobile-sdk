package com.genesys.cloud.messenger.journey.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

internal actual fun createJourneyPlatformHttpClient(logging: Boolean): HttpClient =
    HttpClient(Darwin) {
        engine {
            configureRequest {
                setAllowsCellularAccess(true)
                setAllowsConstrainedNetworkAccess(true)
                setAllowsExpensiveNetworkAccess(true)
            }
        }
        applyJourneyConfig(logging)
    }
