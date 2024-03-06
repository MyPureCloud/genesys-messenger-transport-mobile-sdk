package com.genesys.cloud.messenger.transport.core

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.genesys.cloud.messenger.transport.core.CustomAttributesStoreImpl.State
import com.genesys.cloud.messenger.transport.core.events.Event
import com.genesys.cloud.messenger.transport.core.events.EventHandler
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogMessages
import com.genesys.cloud.messenger.transport.utility.TestValues
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class CustomAttributesStoreTest {
    private val mockLogger: Log = mockk(relaxed = true)
    private val logSlot = mutableListOf<() -> String>()
    private val mockEventHandler = mockk<EventHandler>(relaxed = true)
    private var subject: CustomAttributesStoreImpl =
        CustomAttributesStoreImpl(mockLogger, mockEventHandler).also { it.maxCustomDataBytes = TestValues.MaxCustomDataBytes }

    @Test
    fun `when add a new customAttribute`() {
        val givenCustomAttributes = TestValues.defaultMap
        val expectedCustomAttributes = TestValues.defaultMap

        val result = subject.add(givenCustomAttributes)

        verify { mockLogger.i(capture(logSlot)) }
        assertThat(subject.state).isPending()
        assertThat(subject.get()).isEqualTo(expectedCustomAttributes)
        assertThat(subject.getCustomAttributesToSend()).isEqualTo(expectedCustomAttributes)
        assertThat(result).isTrue()
        assertThat(logSlot[0].invoke()).isEqualTo(
            LogMessages.addCustomAttribute(
                expectedCustomAttributes,
                State.PENDING.name
            )
        )
    }

    @Test
    fun `when add an empty customAttribute`() {
        subject.add(emptyMap())

        assertThat(subject.state).isPending()
        assertThat(subject.get()).isEmpty()
        assertThat(subject.getCustomAttributesToSend()).isEmpty()
    }

    @Test
    fun `when add a customAttribute and then add an empty map`() {
        val result = subject.add(emptyMap())

        assertThat(subject.state).isPending()
        assertThat(subject.get()).isEmpty()
        assertThat(subject.getCustomAttributesToSend()).isEmpty()
        assertThat(result).isFalse()
        verify { mockLogger.w(capture(logSlot)) }
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.CUSTOM_ATTRIBUTES_EMPTY_OR_SAME)
    }

    @Test
    fun `when add the same customAttribute twice`() {
        val givenCustomAttributes = TestValues.defaultMap
        val expectedCustomAttributes = TestValues.defaultMap

        val result1 = subject.add(givenCustomAttributes)
        val result2 = subject.add(givenCustomAttributes)

        assertThat(subject.state).isPending()
        assertThat(subject.get()).isEqualTo(expectedCustomAttributes)
        assertThat(subject.getCustomAttributesToSend()).isEqualTo(expectedCustomAttributes)
        assertThat(result1).isTrue()
        assertThat(result2).isFalse()
        verify { mockLogger.w(capture(logSlot)) }
        verify(exactly = 0) { mockEventHandler.onEvent(any()) }
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.CUSTOM_ATTRIBUTES_EMPTY_OR_SAME)
    }

    @Test
    fun `when add customAttributes with similar key but different value`() {
        val initialCustomAttributes = TestValues.defaultMap
        val updatedCustomAttributes = TestValues.defaultMap
        val expectedCustomAttributes = TestValues.defaultMap

        subject.add(initialCustomAttributes)
        subject.add(updatedCustomAttributes)

        assertThat(subject.state).isPending()
        assertThat(subject.get()).isEqualTo(expectedCustomAttributes)
        assertThat(subject.getCustomAttributesToSend()).isEqualTo(expectedCustomAttributes)
    }

    @Test
    fun `when getCustomAttributesToSend and current state is Pending `() {
        val givenCustomAttributes = TestValues.defaultMap
        val expectedCustomAttributes = TestValues.defaultMap
        subject.add(givenCustomAttributes)

        val result = subject.getCustomAttributesToSend()

        assertThat(subject.state).isPending()
        assertThat(subject.get()).isEqualTo(expectedCustomAttributes)
        assertThat(result).isEqualTo(expectedCustomAttributes)
    }

    @Test
    fun `when getCustomAttributesToSend and current state is Sending`() {
        val givenCustomAttributes = TestValues.defaultMap
        val expectedCustomAttributes = TestValues.defaultMap
        subject.add(givenCustomAttributes)
        subject.onSending()

        val result = subject.getCustomAttributesToSend()

        assertThat(subject.state).isSending()
        assertThat(subject.get()).isEqualTo(expectedCustomAttributes)
        assertThat(result).isEmpty()
    }

    @Test
    fun `when getCustomAttributesToSend and current state is Sent`() {
        val givenCustomAttributes = TestValues.defaultMap
        val expectedCustomAttributes = TestValues.defaultMap
        subject.add(givenCustomAttributes)
        subject.onSending()
        subject.onSent()

        val result = subject.getCustomAttributesToSend()

        assertThat(subject.state).isSent()
        assertThat(subject.get()).isEqualTo(expectedCustomAttributes)
        assertThat(result).isEmpty()
    }

    @Test
    fun `when getCustomAttributesToSend and current state is Error`() {
        val givenCustomAttributes = TestValues.defaultMap
        subject.add(givenCustomAttributes)
        subject.onError()

        val result = subject.getCustomAttributesToSend()

        assertThat(subject.state).isError()
        assertThat(subject.get()).isEmpty()
        assertThat(result).isEmpty()
    }

    @Test
    fun `when onSending`() {
        subject.onSending()

        assertThat(subject.state).isSending()
        verify { mockLogger.i(capture(logSlot)) }
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.ON_SENDING)
    }

    @Test
    fun `when onError`() {
        subject.onError()

        assertThat(subject.state).isError()
        assertThat(subject.get()).isEmpty()
        assertThat(subject.getCustomAttributesToSend()).isEmpty()
        verify { mockLogger.i(capture(logSlot)) }
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.ON_ERROR)
    }

    @Test
    fun `when onSessionClosed`() {
        subject.onSessionClosed()

        assertThat(subject.state).isPending()
        assertThat(subject.maxCustomDataBytes).isEqualTo(MAX_CUSTOM_DATA_BYTES_UNSET)
        verify { mockLogger.i(capture(logSlot)) }
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.ON_SESSION_CLOSED)
    }

    @Test
    fun `when onMessageError`() {
        subject.onMessageError()

        assertThat(subject.state).isPending()
        verify { mockLogger.i(capture(logSlot)) }
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.ON_MESSAGE_ERROR)
    }

    @Test
    fun `when add customAttributes and then onSessionClosed`() {
        val givenCustomAttributes = TestValues.defaultMap
        val expectedCustomAttributes = TestValues.defaultMap

        subject.add(givenCustomAttributes)
        subject.onSessionClosed()

        assertThat(subject.state).isPending()
        assertThat(subject.get()).isEqualTo(expectedCustomAttributes)
        assertThat(subject.getCustomAttributesToSend()).isEqualTo(expectedCustomAttributes)
    }

    @Test
    fun `when onSent and current state is Pending`() {
        subject.onSent()

        assertThat(subject.state).isPending()
        verify { mockLogger.i(capture(logSlot)) }
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.onSentState(State.PENDING.name))
    }

    @Test
    fun `when onSent and current state is Sending`() {
        subject.onSending()

        subject.onSent()

        assertThat(subject.state).isSent()
    }

    @Test
    fun `when onSent and current state is Error`() {
        subject.onError()

        subject.onSent()

        assertThat(subject.state).isSent()
    }

    @Test
    fun `when onSent and current state is Sent`() {
        // In order to properly transition to State.Sent
        // we should first simulate transition to State.Sending
        subject.onSending()
        subject.onSent()

        subject.onSent()

        assertThat(subject.state).isSent()
    }

    @Test
    fun `when maxCustomDataBytes is set`() {
        val expectedMaxCustomDataBytes = TestValues.MaxCustomDataBytes

        subject.maxCustomDataBytes = expectedMaxCustomDataBytes

        assertThat(subject.maxCustomDataBytes).isEqualTo(expectedMaxCustomDataBytes)
    }

    @Test
    fun `when customAttributes size exceeds maxCustomDataBytes`() {
        val expectedEvent = Event.Error(
            ErrorCode.CustomAttributeSizeTooLarge,
            ErrorMessage.customAttributesSizeError(0),
            CorrectiveAction.CustomAttributeSizeTooLarge
        )
        subject.maxCustomDataBytes = 0

        val result = subject.add(TestValues.defaultMap)

        assertThat(subject.get()).isEmpty()
        assertThat(subject.getCustomAttributesToSend()).isEmpty()
        assertThat(result).isFalse()

        verify {
            mockEventHandler.onEvent(expectedEvent)
            mockLogger.e(capture(logSlot))
        }
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.CUSTOM_ATTRIBUTES_SIZE_EXCEEDED)
    }

    @Test
    fun `when maxCustomDataBytes was not set yet`() {
        // Given
        val expectedMaxCustomDataBytes = MAX_CUSTOM_DATA_BYTES_UNSET
        subject = CustomAttributesStoreImpl(mockLogger, mockEventHandler)

        // When
        val result = subject.maxCustomDataBytes

        // Then
        assertThat(result).isEqualTo(expectedMaxCustomDataBytes)
    }

    @Test
    fun `when maxCustomDataBytes is updated with valid size and ca are already added`() {
        // Given
        subject = CustomAttributesStoreImpl(mockLogger, mockEventHandler)
        val givenMaxCustomDataBytes = TestValues.MaxCustomDataBytes
        val givenCustomAttributes = TestValues.defaultMap
        val expectedCustomAttributes = TestValues.defaultMap
        val result = subject.add(givenCustomAttributes)
        assertThat(subject.maxCustomDataBytes).isEqualTo(MAX_CUSTOM_DATA_BYTES_UNSET)
        // When
        subject.maxCustomDataBytes = givenMaxCustomDataBytes
        // Then
        verify(exactly = 0) {
            mockEventHandler.onEvent(any())
        }
        assertThat(subject.get()).isEqualTo(expectedCustomAttributes)
        assertThat(result).isTrue()
    }

    @Test
    fun `when custom attributes are already added but are bigger than updated maxCustomDataBytes`() {
        // Given
        subject = CustomAttributesStoreImpl(mockLogger, mockEventHandler)
        val givenMaxCustomDataBytes = TestValues.DefaultNumber
        val givenCustomAttributes = TestValues.defaultMap
        val result = subject.add(givenCustomAttributes)
        assertThat(subject.maxCustomDataBytes).isEqualTo(MAX_CUSTOM_DATA_BYTES_UNSET)
        // When
        subject.maxCustomDataBytes = givenMaxCustomDataBytes
        // Then
        verify {
            mockEventHandler.onEvent(any())
        }
        assertThat(subject.get()).isEmpty()
        assertThat(result).isTrue()
    }
}
