package com.chargealert.app.domain

/** The subset of AlertSettings the repeat engine needs, as plain values so it stays framework-free. */
data class RepeatRule(
    val repeatEnabled: Boolean,
    val repeatIntervalMinutes: Int,
    val maxRepeats: Int,
    val snoozeMinutes: Int
)

/**
 * Result of a single transition: the new session state, plus the side
 * effects the caller (BatteryMonitoringService) needs to perform. Kept as
 * plain flags rather than a sealed hierarchy so a single transition can both
 * fire an alert AND schedule the next one in the same step.
 */
data class Transition(
    val newState: ChargingSessionState,
    val fireAlert: Boolean = false,
    /** Which firing this is when [fireAlert] is true: 0 = initial alert, 1..maxRepeats = a repeat. */
    val firingRepeatCount: Int = 0,
    val scheduleDelayMinutes: Int? = null,
    val cancelSchedule: Boolean = false
)

/**
 * Pure alert/repeat decision logic -- no Android dependencies, no timers.
 * BatteryMonitoringService owns the actual coroutine delay/dispatch/
 * notification-action wiring; this only decides what should happen.
 *
 * Supersedes the Phase 2 AlertEngine: repeatEnabled=false reproduces the old
 * "fire exactly once" behavior exactly (see fireAlert()), so there is only
 * one alert-decision implementation now, not two.
 */
object RepeatAlertEngine {

    fun onBatteryUpdate(
        current: ChargingSessionState,
        isCharging: Boolean,
        thresholdReached: Boolean,
        rule: RepeatRule
    ): Transition {
        if (!isCharging) {
            return if (current == ChargingSessionState.NotCharging) {
                Transition(ChargingSessionState.NotCharging)
            } else {
                // Disconnect (from any state) cancels the entire sequence.
                Transition(ChargingSessionState.NotCharging, cancelSchedule = true)
            }
        }

        return when (current) {
            ChargingSessionState.NotCharging, ChargingSessionState.Charging ->
                if (thresholdReached) fireAlert(thisFiringCount = 0, rule) else Transition(ChargingSessionState.Charging)
            // Already alerted this session (waiting/acknowledged/ended): duplicate
            // battery broadcasts while charging must never re-trigger anything.
            else -> Transition(current)
        }
    }

    fun onRepeatTimerFired(current: ChargingSessionState, rule: RepeatRule): Transition {
        val waiting = current as? ChargingSessionState.WaitingForRepeat ?: return Transition(current)
        return fireAlert(thisFiringCount = waiting.repeatCount + 1, rule)
    }

    fun onStop(current: ChargingSessionState): Transition {
        return when (current) {
            is ChargingSessionState.WaitingForRepeat ->
                Transition(ChargingSessionState.Acknowledged, cancelSchedule = true)
            else -> Transition(current)
        }
    }

    fun onSnooze(current: ChargingSessionState, rule: RepeatRule): Transition {
        val waiting = current as? ChargingSessionState.WaitingForRepeat ?: return Transition(current)
        return Transition(waiting, cancelSchedule = true, scheduleDelayMinutes = rule.snoozeMinutes)
    }

    /**
     * [thisFiringCount]: 0 for the initial alert, 1..maxRepeats for repeats.
     * Always fires (the caller already decided this alert should happen);
     * the only open question is whether another one should be scheduled
     * after it.
     */
    private fun fireAlert(thisFiringCount: Int, rule: RepeatRule): Transition {
        val nextRepeatNumber = thisFiringCount + 1
        val shouldScheduleNext = rule.repeatEnabled && nextRepeatNumber <= rule.maxRepeats

        return if (shouldScheduleNext) {
            Transition(
                newState = ChargingSessionState.WaitingForRepeat(thisFiringCount),
                fireAlert = true,
                firingRepeatCount = thisFiringCount,
                scheduleDelayMinutes = rule.repeatIntervalMinutes
            )
        } else {
            Transition(
                newState = ChargingSessionState.AlertSequenceEnded,
                fireAlert = true,
                firingRepeatCount = thisFiringCount
            )
        }
    }
}
