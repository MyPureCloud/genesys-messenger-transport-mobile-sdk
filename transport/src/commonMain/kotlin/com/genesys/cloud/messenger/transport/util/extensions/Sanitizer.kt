package com.genesys.cloud.messenger.transport.util.extensions

import com.genesys.cloud.messenger.transport.core.ButtonResponse

/**
 * Replaces characters with stars (*) except for the last 4 characters
 */

internal fun String.sanitize(): String {
    val lastChars = 4
    if (this.length <= lastChars) {
        return this // Nothing to sanitize if length is lastChars or fewer characters
    }

    return "*".repeat(this.length - lastChars) + this.takeLast(lastChars)
}

fun String.sanitizeSensitiveData(): String = this.sanitizeToken().sanitizeText().sanitizeCustomAttributes()

internal fun String.sanitizeCustomAttributes(): String {
    val regex = """("customAttributes":\{)(.*?)(\})""".toRegex()
    return this.replace(regex) {
        """${it.groupValues[1]}${it.groupValues[2].sanitize()}${it.groupValues[3]}"""
    }
}

internal fun String.sanitizeText(): String {
    var regex = """("text":")([^"]*)(")""".toRegex()
    var sanitizedInput =
        this.replace(regex) {
            """${it.groupValues[1]}${it.groupValues[2].sanitize()}${it.groupValues[3]}"""
        }
    regex = """(text=)(.*?)(?=, \w+:|$|[)])""".toRegex()
    sanitizedInput =
        sanitizedInput.replace(regex) {
            """${it.groupValues[1]}${it.groupValues[2].sanitize()}"""
        }
    return sanitizedInput
}

internal fun String.sanitizeToken(): String {
    val tokenRegex = """("token":")([a-fA-F0-9-]{36})(")""".toRegex()
    return this.replace(tokenRegex) {
        """${it.groupValues[1]}${it.groupValues[2].sanitize()}${it.groupValues[3]}"""
    }
}

internal fun Map<String, String>.sanitizeValues(): Map<String, String> {
    return this.mapValues { (_, value) ->
        value.sanitize()
    }
}

internal fun ButtonResponse.sanitize(): ButtonResponse {
    return ButtonResponse(
        text = this.text.sanitize(),
        payload = this.payload.sanitize(),
        type = this.type
    )
}
