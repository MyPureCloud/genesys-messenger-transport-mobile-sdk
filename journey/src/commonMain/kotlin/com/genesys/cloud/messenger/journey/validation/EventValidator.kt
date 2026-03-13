package com.genesys.cloud.messenger.journey.validation

import com.genesys.cloud.messenger.journey.util.logs.Log

private const val MAX_EVENT_NAME_LENGTH = 255
private val EVENT_NAME_REGEX = Regex("^[A-Za-z0-9_-]+$")

internal class EventValidator(private val log: Log) {

    fun isValidEventName(eventName: String): Boolean {
        if (eventName.isEmpty() || eventName.length > MAX_EVENT_NAME_LENGTH) {
            log.e {
                "Invalid eventName: must be between 1 and $MAX_EVENT_NAME_LENGTH characters long. " +
                    "Got ${eventName.length} characters."
            }
            return false
        }
        if (!EVENT_NAME_REGEX.matches(eventName)) {
            log.e {
                "Invalid eventName '$eventName': can only contain alphanumeric characters " +
                    "(A-Z, a-z, 0-9), underscores (_), and hyphens (-)."
            }
            return false
        }
        return true
    }
}
