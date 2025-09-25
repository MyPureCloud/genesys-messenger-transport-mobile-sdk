package com.genesys.cloud.messenger.transport.push

import io.ktor.http.HttpMethod

internal sealed class DeviceTokenOperation(val httpMethod: HttpMethod) {
    data object Register : DeviceTokenOperation(HttpMethod.Post)

    data object Update : DeviceTokenOperation(HttpMethod.Patch)

    data object Delete : DeviceTokenOperation(HttpMethod.Delete)
}
