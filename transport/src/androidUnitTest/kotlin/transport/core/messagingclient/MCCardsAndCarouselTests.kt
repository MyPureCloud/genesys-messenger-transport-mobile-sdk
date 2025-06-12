package transport.core.messagingclient

import com.genesys.cloud.messenger.transport.core.Message
import com.genesys.cloud.messenger.transport.core.MessageStore
import com.genesys.cloud.messenger.transport.shyrka.send.StructuredMessage
import com.genesys.cloud.messenger.transport.utility.QuickReplyTestValues
import io.mockk.every
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test
import transport.util.Request
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MCCardsAndCarouselTests : BaseMessagingClientTest() {

    @Test
    fun `when preparePostbackMessage is called then returns StructuredMessage with ButtonResponse`() {
        val json = Json {
            ignoreUnknownKeys = true
        }

        val expectedButtonResponse = QuickReplyTestValues.postbackButtonResponse

        every { mockCustomAttributesStore.getCustomAttributesToSend() } returns emptyMap()

        subject.connect()

        val result = MessageStore(mockLogger).preparePostbackMessage(
            token = Request.token,
            buttonResponse = expectedButtonResponse,
            channel = null
        )

        val structuredMessage = result.message as StructuredMessage
        val messageId = structuredMessage.metadata?.get("customMessageId")
        requireNotNull(messageId) { "customMessageId should not be null" }

        val messageJson = json.encodeToJsonElement(
            StructuredMessage.serializer(),
            structuredMessage
        ).jsonObject

        val text = messageJson["text"]?.jsonPrimitive?.content
        val content = messageJson["content"]?.jsonArray?.firstOrNull()?.jsonObject
        val buttonResponse = content?.get("buttonResponse")?.jsonObject

        requireNotNull(text)
        requireNotNull(content)
        requireNotNull(buttonResponse)

        assertEquals(expectedButtonResponse.text, text)
        assertEquals(expectedButtonResponse.payload, buttonResponse["payload"]?.jsonPrimitive?.content)
        assertEquals(expectedButtonResponse.type, buttonResponse["type"]?.jsonPrimitive?.content)

        val contentObj = structuredMessage.content.firstOrNull()
        assertNotNull(contentObj)
        assertEquals(Message.Content.Type.ButtonResponse, contentObj.contentType)
        assertEquals(expectedButtonResponse.payload, contentObj.buttonResponse?.payload)
        assertEquals(expectedButtonResponse.type, contentObj.buttonResponse?.type)
        assertEquals(expectedButtonResponse.text, contentObj.buttonResponse?.text)
    }
}
