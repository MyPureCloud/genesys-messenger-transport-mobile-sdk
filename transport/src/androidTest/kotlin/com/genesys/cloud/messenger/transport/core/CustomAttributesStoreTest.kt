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
import com.genesys.cloud.messenger.transport.utility.TestValues
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class CustomAttributesStoreTest {
    private val mockLogger: Log = mockk(relaxed = true)
    private val logSlot = mutableListOf<() -> String>()
    private val mockEventHandler = mockk<EventHandler>(relaxed = true)
    private val subject: CustomAttributesStoreImpl =
        CustomAttributesStoreImpl(mockLogger, mockEventHandler).also { it.maxCustomDataBytes = TestValues.MaxCustomDataBytes }

    @Test
    fun `when add a new customAttribute`() {
        val givenCustomAttributes = mapOf("A" to "B")
        val expectedCustomAttributes = mapOf("A" to "B")
        val expectedLogMessage = "add: $expectedCustomAttributes | state = ${State.PENDING}"

        val result = subject.add(givenCustomAttributes)

        verify { mockLogger.i(capture(logSlot)) }
        assertThat(subject.state).isPending()
        assertThat(subject.get()).isEqualTo(expectedCustomAttributes)
        assertThat(subject.getCustomAttributesToSend()).isEqualTo(expectedCustomAttributes)
        assertThat(result).isTrue()
        assertThat(logSlot[0].invoke()).isEqualTo(expectedLogMessage)
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
        val expectedLogMessage = "custom attributes are empty or same."

        val result = subject.add(emptyMap())

        assertThat(subject.state).isPending()
        assertThat(subject.get()).isEmpty()
        assertThat(subject.getCustomAttributesToSend()).isEmpty()
        assertThat(result).isFalse()
        verify { mockLogger.w(capture(logSlot)) }
        assertThat(logSlot[0].invoke()).isEqualTo(expectedLogMessage)
    }

    @Test
    fun `when add the same customAttribute twice`() {
        val givenCustomAttributes = mapOf("A" to "B")
        val expectedCustomAttributes = mapOf("A" to "B")
        val expectedLogMessage = "custom attributes are empty or same."

        val result1 = subject.add(givenCustomAttributes)
        val result2 = subject.add(givenCustomAttributes)

        assertThat(subject.state).isPending()
        assertThat(subject.get()).isEqualTo(expectedCustomAttributes)
        assertThat(subject.getCustomAttributesToSend()).isEqualTo(expectedCustomAttributes)
        assertThat(result1).isTrue()
        assertThat(result2).isFalse()
        verify { mockLogger.w(capture(logSlot)) }
        verify(exactly = 0) { mockEventHandler.onEvent(any()) }
        assertThat(logSlot[0].invoke()).isEqualTo(expectedLogMessage)
    }

    @Test
    fun `when add customAttributes with similar key but different value`() {
        val initialCustomAttributes = mapOf("A" to "B")
        val updatedCustomAttributes = mapOf("A" to "C")
        val expectedCustomAttributes = mapOf("A" to "C")

        subject.add(initialCustomAttributes)
        subject.add(updatedCustomAttributes)

        assertThat(subject.state).isPending()
        assertThat(subject.get()).isEqualTo(expectedCustomAttributes)
        assertThat(subject.getCustomAttributesToSend()).isEqualTo(expectedCustomAttributes)
    }

    @Test
    fun `when getCustomAttributesToSend and current state is Pending `() {
        val givenCustomAttributes = mapOf("A" to "B")
        val expectedCustomAttributes = mapOf("A" to "B")
        subject.add(givenCustomAttributes)

        val result = subject.getCustomAttributesToSend()

        assertThat(subject.state).isPending()
        assertThat(subject.get()).isEqualTo(expectedCustomAttributes)
        assertThat(result).isEqualTo(expectedCustomAttributes)
    }

    @Test
    fun `when getCustomAttributesToSend and current state is Sending`() {
        val givenCustomAttributes = mapOf("A" to "B")
        val expectedCustomAttributes = mapOf("A" to "B")
        subject.add(givenCustomAttributes)
        subject.onSending()

        val result = subject.getCustomAttributesToSend()

        assertThat(subject.state).isSending()
        assertThat(subject.get()).isEqualTo(expectedCustomAttributes)
        assertThat(result).isEmpty()
    }

    @Test
    fun `when getCustomAttributesToSend and current state is Sent`() {
        val givenCustomAttributes = mapOf("A" to "B")
        val expectedCustomAttributes = mapOf("A" to "B")
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
        val givenCustomAttributes = mapOf("A" to "B")
        subject.add(givenCustomAttributes)
        subject.onError()

        val result = subject.getCustomAttributesToSend()

        assertThat(subject.state).isError()
        assertThat(subject.get()).isEmpty()
        assertThat(result).isEmpty()
    }

    @Test
    fun `when onSending`() {
        val expectedLogMessage = "onSending()"

        subject.onSending()

        assertThat(subject.state).isSending()
        verify { mockLogger.i(capture(logSlot)) }
        assertThat(logSlot[0].invoke()).isEqualTo(expectedLogMessage)
    }

    @Test
    fun `when onError`() {
        val expectedLogMessage = "onError()"

        subject.onError()

        assertThat(subject.state).isError()
        assertThat(subject.get()).isEmpty()
        assertThat(subject.getCustomAttributesToSend()).isEmpty()
        verify { mockLogger.i(capture(logSlot)) }
        assertThat(logSlot[0].invoke()).isEqualTo(expectedLogMessage)
    }

    @Test
    fun `when onSessionClosed`() {
        val expectedLogMessage = "onSessionClosed()"

        subject.onSessionClosed()

        assertThat(subject.state).isPending()
        verify { mockLogger.i(capture(logSlot)) }
        assertThat(logSlot[0].invoke()).isEqualTo(expectedLogMessage)
    }

    @Test
    fun `when onMessageError`() {
        val expectedLogMessage = "onMessageError()"

        subject.onMessageError()

        assertThat(subject.state).isPending()
        verify { mockLogger.i(capture(logSlot)) }
        assertThat(logSlot[0].invoke()).isEqualTo(expectedLogMessage)
    }

    @Test
    fun `when add customAttributes and then onSessionClosed`() {
        val givenCustomAttributes = mapOf("A" to "B")
        val expectedCustomAttributes = mapOf("A" to "B")

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
        val expectedLogMessage = "error: custom attributes size exceeded"
        subject.maxCustomDataBytes = 0

        val result = subject.add(mapOf("A" to "B"))

        assertThat(subject.get()).isEmpty()
        assertThat(subject.getCustomAttributesToSend()).isEmpty()
        assertThat(result).isFalse()

        verify {
            mockEventHandler.onEvent(expectedEvent)
            mockLogger.e(capture(logSlot))
        }
        assertThat(logSlot[0].invoke()).isEqualTo(expectedLogMessage)
    }
}
