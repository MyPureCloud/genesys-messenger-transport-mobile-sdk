package com.genesys.cloud.messenger.journey.validation

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.genesys.cloud.messenger.journey.util.logs.Log
import kotlin.test.Test

class EventValidatorTest {

    private val log = Log(enableLogs = false, tag = "Test")
    private val validator = EventValidator(log)

    @Test
    fun `valid alphanumeric event name`() {
        assertThat(validator.isValidEventName("add_to_cart")).isTrue()
    }

    @Test
    fun `valid event name with hyphens`() {
        assertThat(validator.isValidEventName("page-view")).isTrue()
    }

    @Test
    fun `valid event name with mixed characters`() {
        assertThat(validator.isValidEventName("Event_Name-123")).isTrue()
    }

    @Test
    fun `empty event name is invalid`() {
        assertThat(validator.isValidEventName("")).isFalse()
    }

    @Test
    fun `event name with spaces is invalid`() {
        assertThat(validator.isValidEventName("add to cart")).isFalse()
    }

    @Test
    fun `event name with special characters is invalid`() {
        assertThat(validator.isValidEventName("event@name")).isFalse()
    }

    @Test
    fun `event name with dots is invalid`() {
        assertThat(validator.isValidEventName("event.name")).isFalse()
    }

    @Test
    fun `event name exceeding 255 characters is invalid`() {
        val longName = "a".repeat(256)
        assertThat(validator.isValidEventName(longName)).isFalse()
    }

    @Test
    fun `event name at exactly 255 characters is valid`() {
        val maxName = "a".repeat(255)
        assertThat(validator.isValidEventName(maxName)).isTrue()
    }

    @Test
    fun `single character event name is valid`() {
        assertThat(validator.isValidEventName("a")).isTrue()
    }
}
