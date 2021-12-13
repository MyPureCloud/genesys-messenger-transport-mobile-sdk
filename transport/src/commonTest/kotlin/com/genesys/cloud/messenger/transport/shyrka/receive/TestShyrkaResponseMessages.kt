package com.genesys.cloud.messenger.transport.shyrka.receive

object TestShyrkaResponseMessages {
    fun sessionResponseOk(connected: Boolean = true, newSession: Boolean = true) = """
            {
              "type": "response",
              "class": "SessionResponse",
              "code": 200,
              "body": {
                "connected": $connected,
                "newSession": $newSession
              }
            }
            """

    fun stringResponseOk(code: Int, body: String) =
        """
            {
              "type": "response",
              "class": "string",
              "code": $code,
              "body": "$body"
            }
            """

    const val sessionExpiredResponseOk =
        """
            {
              "type": "response",
              "class": "SessionExpiredEvent",
              "code": 200,
              "body": {}
            }
            """
}
