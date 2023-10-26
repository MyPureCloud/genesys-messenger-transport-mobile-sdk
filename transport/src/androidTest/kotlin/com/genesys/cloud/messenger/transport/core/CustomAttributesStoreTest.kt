package com.genesys.cloud.messenger.transport.core

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import io.mockk.mockk
import org.junit.Test

class CustomAttributesStoreTest {

    private val subject: CustomAttributesStoreImpl =
        CustomAttributesStoreImpl(mockk(relaxed = true))

    @Test
    fun `when add a new customAttribute`() {
        val givenCustomAttributes = mapOf("A" to "B")
        val expectedCustomAttributes = mapOf("A" to "B")

        subject.add(givenCustomAttributes)

        assertThat(subject.state).isPending()
        assertThat(subject.get()).isEqualTo(expectedCustomAttributes)
        assertThat(subject.getCustomAttributesToSend()).isEqualTo(expectedCustomAttributes)
    }

    @Test
    fun `when add an empty customAttribute`() {
        subject.add(emptyMap())

        assertThat(subject.state).isPending()
        assertThat(subject.get()).isEmpty()
        assertThat(subject.getCustomAttributesToSend()).isEmpty()
    }

    @Test
    fun `when add the same customAttribute twice`() {
        val givenCustomAttributes = mapOf("A" to "B")
        val expectedCustomAttributes = mapOf("A" to "B")

        subject.add(givenCustomAttributes)
        subject.add(givenCustomAttributes)

        assertThat(subject.state).isPending()
        assertThat(subject.get()).isEqualTo(expectedCustomAttributes)
        assertThat(subject.getCustomAttributesToSend()).isEqualTo(expectedCustomAttributes)
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
        subject.onSending()

        assertThat(subject.state).isSending()
    }

    @Test
    fun `when onError`() {
        subject.onError()

        assertThat(subject.state).isError()
        assertThat(subject.get()).isEmpty()
        assertThat(subject.getCustomAttributesToSend()).isEmpty()
    }

    @Test
    fun `when onSessionClosed`() {
        subject.onSessionClosed()

        assertThat(subject.state).isPending()
    }

    @Test
    fun `when onMessageError`() {
        subject.onMessageError()

        assertThat(subject.state).isPending()
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
}
