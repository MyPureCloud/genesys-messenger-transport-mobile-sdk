package transport.core.messagingclient

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import com.genesys.cloud.messenger.transport.core.Message
import com.genesys.cloud.messenger.transport.core.MessageStore
import com.genesys.cloud.messenger.transport.shyrka.send.StructuredMessage
import com.genesys.cloud.messenger.transport.utility.CardTestValues
import io.mockk.every
import org.junit.Test
import transport.util.Request

class MCCardsAndCarouselTests : BaseMessagingClientTest() {

    @Test
    fun `when preparePostbackMessage is called then returns StructuredMessage with ButtonResponse`() {
        val expectedButtonResponse = CardTestValues.postbackButtonResponse
        every { mockCustomAttributesStore.getCustomAttributesToSend() } returns emptyMap()
        subject.connect()

        val result = MessageStore(mockLogger).preparePostbackMessage(
            token = Request.token,
            buttonResponse = expectedButtonResponse,
            channel = null
        )

        assertThat(result.message).isInstanceOf(StructuredMessage::class)
        val structuredMessage = result.message as StructuredMessage
        val contentObj = structuredMessage.content.firstOrNull()

        assertThat(contentObj).isNotNull()
        assertThat(contentObj?.contentType).isEqualTo(Message.Content.Type.ButtonResponse)
        assertThat(contentObj?.buttonResponse).isNotNull()
        assertThat(contentObj?.buttonResponse?.text).isEqualTo(expectedButtonResponse.text)
        assertThat(contentObj?.buttonResponse?.payload).isEqualTo(expectedButtonResponse.payload)
        assertThat(contentObj?.buttonResponse?.type).isEqualTo(expectedButtonResponse.type)
    }
}
