package com.genesys.cloud.messenger.transport.shyrka.receive

internal object TestWebMessagingMessages {
    fun responseOk(classname: MessageClassName, body: String) = """{"type":"response","class":"${classname.value}","code":200,"body":$body}"""
}
