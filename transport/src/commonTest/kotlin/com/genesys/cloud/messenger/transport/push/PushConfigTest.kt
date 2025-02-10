package com.genesys.cloud.messenger.transport.push

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.genesys.cloud.messenger.transport.shyrka.WebMessagingJson
import com.genesys.cloud.messenger.transport.util.UNKNOWN
import com.genesys.cloud.messenger.transport.util.UNKNOWN_LONG
import com.genesys.cloud.messenger.transport.utility.PushTestValues
import kotlinx.serialization.encodeToString
import kotlin.test.Test

class PushConfigTest {

    @Test
    fun `test PushConfig serialization`() {
        val expectedPushConfigString = """{"token":"<token>","deviceToken":"<device_token>","preferredLanguage":"Eng","lastSyncTimestamp":1,"deviceType":"Android","pushProvider":"APNS"}"""

        val encodedString = WebMessagingJson.json.encodeToString(PushTestValues.CONFIG)

        assertThat(encodedString).isEqualTo(expectedPushConfigString)
    }

    @Test
    fun `test PushConfig deserialization`() {
        val givenPushConfigString = """{"token":"<token>","deviceToken":"<device_token>","preferredLanguage":"Eng","lastSyncTimestamp":1,"deviceType":"Android","pushProvider":"APNS"}"""

        val result = WebMessagingJson.json.decodeFromString<PushConfig>(givenPushConfigString)

        assertThat(result).isEqualTo(PushTestValues.CONFIG)
    }

    @Test
    fun `test DEFAULT_PUSH_CONFIG values`() {
        DEFAULT_PUSH_CONFIG.run {
            assertThat(token).isEqualTo(UNKNOWN)
            assertThat(deviceToken).isEqualTo(UNKNOWN)
            assertThat(preferredLanguage).isEqualTo(UNKNOWN)
            assertThat(lastSyncTimestamp).isEqualTo(UNKNOWN_LONG)
            assertThat(deviceType).isEqualTo(UNKNOWN)
            assertThat(pushProvider).isNull()
        }
    }
}
