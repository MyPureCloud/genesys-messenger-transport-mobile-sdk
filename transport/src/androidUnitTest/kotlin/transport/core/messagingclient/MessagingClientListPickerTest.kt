package transport.core.messagingclient

import com.genesys.cloud.messenger.transport.core.Message
import com.genesys.cloud.messenger.transport.shyrka.send.Channel
import com.genesys.cloud.messenger.transport.shyrka.send.OnMessageRequest
import com.genesys.cloud.messenger.transport.shyrka.send.StructuredMessage
import com.genesys.cloud.messenger.transport.utility.ListPickerTestValues
import io.mockk.every
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.Test
import transport.util.Request
import kotlin.test.assertFailsWith

class MessagingClientListPickerTest : BaseMessagingClientTest() {

    private val givenSingleSelection = ListPickerTestValues.singleSelection
    private val givenMultiSelection = ListPickerTestValues.multiSelection
    private val givenCrossSectionSelection = ListPickerTestValues.crossSectionSelection

    @Test
    fun `when submitListPicker() but client is not configured`() {
        assertFailsWith<IllegalStateException> {
            subject.submitListPicker(givenSingleSelection)
        }
    }

    @Test
    fun `when connect() and then submitListPicker() with an empty list then it throws and does not send`() {
        subject.connect()

        assertFailsWith<IllegalArgumentException> {
            subject.submitListPicker(emptyList())
        }

        verify(exactly = 0) {
            mockMessageStore.prepareListPickerSubmissionMessageWith(any(), any(), any())
        }
    }

    @Test
    fun `when connect() and then submitListPicker() with a single selection but no custom attributes`() {
        every { mockCustomAttributesStore.getCustomAttributesToSend() } returns emptyMap()
        subject.connect()

        subject.submitListPicker(givenSingleSelection)

        verifySequence {
            connectSequence()
            mockLogger.d(capture(logSlot))
            mockCustomAttributesStore.getCustomAttributesToSend()
            mockMessageStore.prepareListPickerSubmissionMessageWith(
                Request.token,
                givenSingleSelection,
                null
            )
            mockLogger.d(capture(logSlot))
            mockPlatformSocket.sendMessage(match { Request.isListPickerRequest(it) })
        }

        verify(exactly = 0) {
            mockAttachmentHandler.onSending()
        }
    }

    @Test
    fun `when submitListPicker() with a multi-select then each item is a separate content entry`() {
        every {
            mockMessageStore.prepareListPickerSubmissionMessageWith(
                Request.token,
                givenMultiSelection,
                null
            )
        } returns onMessageRequestWith(givenMultiSelection)
        every { mockCustomAttributesStore.getCustomAttributesToSend() } returns emptyMap()

        subject.connect()
        subject.submitListPicker(givenMultiSelection)

        verify {
            mockMessageStore.prepareListPickerSubmissionMessageWith(
                Request.token,
                givenMultiSelection,
                null
            )
            mockPlatformSocket.sendMessage(
                match {
                    Request.isListPickerRequest(it) &&
                        it.contains(""""payload":"${ListPickerTestValues.ITEM_ID}"""") &&
                        it.contains(""""payload":"${ListPickerTestValues.ITEM_ID_2}"""") &&
                        it.contains(""""originatingMessageId":"${ListPickerTestValues.PICKER_MESSAGE_ID}"""")
                }
            )
        }
    }

    @Test
    fun `when submitListPicker() with a cross-section selection then all items are submitted in one message`() {
        every {
            mockMessageStore.prepareListPickerSubmissionMessageWith(
                Request.token,
                givenCrossSectionSelection,
                null
            )
        } returns onMessageRequestWith(givenCrossSectionSelection)
        every { mockCustomAttributesStore.getCustomAttributesToSend() } returns emptyMap()

        subject.connect()
        subject.submitListPicker(givenCrossSectionSelection)

        verify {
            mockPlatformSocket.sendMessage(
                match {
                    Request.isListPickerRequest(it) &&
                        it.contains(""""payload":"${ListPickerTestValues.ITEM_ID}"""") &&
                        it.contains(""""payload":"${ListPickerTestValues.ITEM_ID_3}"""")
                }
            )
        }
    }

    @Test
    fun `when connect() and then submitListPicker() with custom attributes`() {
        val expectedCustomAttributes = mapOf("source" to "listpicker")
        val expectedChannel = Channel(Channel.Metadata(expectedCustomAttributes))

        every { mockCustomAttributesStore.getCustomAttributesToSend() } returns expectedCustomAttributes
        every {
            mockMessageStore.prepareListPickerSubmissionMessageWith(
                Request.token,
                givenSingleSelection,
                expectedChannel
            )
        } returns onMessageRequestWith(givenSingleSelection, expectedChannel)

        subject.connect()
        subject.submitListPicker(givenSingleSelection)

        verifySequence {
            connectSequence()
            mockLogger.d(capture(logSlot))
            mockCustomAttributesStore.getCustomAttributesToSend()
            mockCustomAttributesStore.onSending()
            mockMessageStore.prepareListPickerSubmissionMessageWith(
                Request.token,
                givenSingleSelection,
                expectedChannel
            )
            mockLogger.d(capture(logSlot))
            mockPlatformSocket.sendMessage(
                match {
                    it.contains(""""contentType":"ButtonResponse"""") &&
                        it.contains(""""action":"onMessage"""") &&
                        it.contains(""""customAttributes":{"source":"listpicker"}""")
                }
            )
        }
    }

    @Test
    fun `when error response received after submitListPicker() then onMessageError is called`() {
        every {
            mockPlatformSocket.sendMessage(match { Request.isListPickerRequest(it) })
        } answers {
            slot.captured.onMessage(transport.util.Response.tooManyRequests)
        }

        subject.connect()
        subject.submitListPicker(givenSingleSelection)

        verify {
            mockMessageStore.onMessageError(any(), any())
        }
    }

    private fun onMessageRequestWith(
        buttonResponses: List<com.genesys.cloud.messenger.transport.core.ButtonResponse>,
        channel: Channel? = null,
    ) = OnMessageRequest(
        token = Request.token,
        message =
            StructuredMessage(
                text = "",
                content =
                    buttonResponses.map {
                        Message.Content(
                            contentType = Message.Content.Type.ButtonResponse,
                            buttonResponse = it,
                        )
                    },
                channel = channel,
            ),
    )
}
