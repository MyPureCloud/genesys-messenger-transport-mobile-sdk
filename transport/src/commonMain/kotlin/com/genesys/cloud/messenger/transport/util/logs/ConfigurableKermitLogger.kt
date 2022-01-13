package com.genesys.cloud.messenger.transport.util.logs

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity

internal class ConfigurableKermitLogger(
    private val enabled: Boolean,
    private val delegate: Logger,
) : Logger() {

    override fun isLoggable(severity: Severity): Boolean {
        return enabled
    }

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        delegate.log(
            severity = severity,
            message = message,
            tag = tag,
            throwable = throwable
        )
    }
}
