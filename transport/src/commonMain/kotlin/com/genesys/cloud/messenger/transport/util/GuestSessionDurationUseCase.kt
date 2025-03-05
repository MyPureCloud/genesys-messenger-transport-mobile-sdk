package com.genesys.cloud.messenger.transport.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.TimeSource
import kotlin.time.toDuration

object GuestSessionDurationUseCase {

    internal const val DEFAULT_DURATION = 300L // 5 minutes
    private val scope = CoroutineScope(Dispatchers.Default)

    internal fun getCurrentTimeInSeconds(): Long {
        return TimeSource.Monotonic.markNow().elapsedNow().toLong(DurationUnit.SECONDS)
    }

    private var timer: CountdownTimer? = null

    /**
     * Handles Gust session expiration based on the given dates and times
     *
     * @param duration the time in seconds the guest session end event action should be fired
     * @param expiration the time when the guest session is about to be ended
     * @param action the action which should be done at the end of the guest session
     */
    operator fun invoke(
        duration: Long = DEFAULT_DURATION,
        expiration: Long = getCurrentTimeInSeconds() + DEFAULT_DURATION,
        action: () -> Unit
    ) {
        val timerDuration = 10L//expiration - duration
        timer?.reset()
        timer = CountdownTimer(timerDuration.toDuration(DurationUnit.SECONDS), scope)
        scope.launch {
            timer?.remainingTime?.collectLatest { remaining ->
                println("Guest session Remaining time: ${remaining.inWholeSeconds} seconds")
                if (remaining == Duration.ZERO) {
                    println("Guest session Timer finished!")
                    action()
                }
            } ?: run {
                println("Guest session: timer listener was not able to start")
            }
        }

        timer?.start()
    }

}


private class CountdownTimer(
    private val totalDuration: Duration,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {

    private val _remainingTime = MutableStateFlow(totalDuration)
    val remainingTime: StateFlow<Duration> = _remainingTime.asStateFlow()

    private var countdownJob: Job? = null

    fun start() {
        countdownJob?.cancel()
        countdownJob = scope.launch {
            var remaining = totalDuration
            while (remaining > Duration.ZERO) {
                _remainingTime.value = remaining
                delay(1.toDuration(DurationUnit.SECONDS))
                remaining = remaining.minus(1.toDuration(DurationUnit.SECONDS))
            }
            _remainingTime.value = Duration.ZERO
        }
    }

    fun stop() {
        countdownJob?.cancel()
    }

    fun reset() {
        stop()
        _remainingTime.value = totalDuration
    }
}