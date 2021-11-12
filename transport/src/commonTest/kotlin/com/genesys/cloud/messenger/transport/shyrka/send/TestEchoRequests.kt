package com.genesys.cloud.messenger.transport.shyrka.send

object TestEchoRequests {
    fun basic() = """{"token":"token","action":"echo","message":{"text":"ping","type":"Text"}}"""
}
