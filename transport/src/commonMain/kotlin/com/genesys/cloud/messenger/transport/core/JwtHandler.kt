package com.genesys.cloud.messenger.transport.core

import com.genesys.cloud.messenger.transport.network.PlatformSocket
import com.genesys.cloud.messenger.transport.shyrka.WebMessagingJson
import com.genesys.cloud.messenger.transport.shyrka.receive.JwtResponse
import com.genesys.cloud.messenger.transport.shyrka.send.JwtRequest
import com.genesys.cloud.messenger.transport.util.Platform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString

internal class JwtHandler(private val webSocket: PlatformSocket, private val token: String) {

    private val jwtChannel = Channel<String>()
    private val socketDispatcher = CoroutineScope(Dispatchers.Main + SupervisorJob())

    internal var jwtResponse: JwtResponse = JwtResponse()
        set(value) {
            field = value
            if (field.jwt.isEmpty()) return
            socketDispatcher.launch {
                jwtChannel.send(field.jwt)
            }
        }

    internal suspend inline fun <reified R> withJwt(jwtFn: (String) -> R): R {
        val jwtResponse = jwtResponse
        return if (jwtResponse.isValid()) {
            jwtFn.invoke(jwtResponse.jwt)
        } else {
            fetchJwt()
            jwtFn.invoke(jwtChannel.receive())
        }
    }

    internal fun clear() {
        jwtResponse = JwtResponse()
    }

    private fun fetchJwt() {
        val request = JwtRequest(token)
        val encodedJson = WebMessagingJson.json.encodeToString(request)
        webSocket.sendMessage(encodedJson)
    }
}

private fun JwtResponse.isValid(): Boolean = this.exp > currentTimeInSeconds() + 5

private fun currentTimeInSeconds(): Long = Platform().epochMillis() / 1000
