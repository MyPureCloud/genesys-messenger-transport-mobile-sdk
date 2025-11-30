package com.genesys.cloud.messenger.transport.util

import com.genesys.cloud.messenger.transport.util.logs.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A timer that executes a pre-configured action after a specified delay.
 *
 * @param log Logger instance for logging timer events.
 * @param action The action to be executed when the timer expires.
 * @param dispatcher The coroutine scope to use for timer execution. Defaults to Dispatchers.Default.
 */
internal class ActionTimer(
    private val log: Log,
    private val action: () -> Unit,
    private val dispatcher: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
) {

    private var timerJob: Job? = null

    fun start(delayMillis: Long) {
        log.i { "Starting timer with delay: $delayMillis ms" }
        cancel()
        timerJob = dispatcher.launch {
            delay(delayMillis)
            log.i { "Timer expired, executing action" }
            action()
        }
    }

    fun cancel() {
        timerJob?.let {
            if (it.isActive) {
                log.i { "Cancelling timer" }
                it.cancel()
            }
        }
        timerJob = null
    }

    fun isActive(): Boolean = timerJob?.isActive == true
}

