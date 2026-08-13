package com.chargealert.app.domain

/**
 * Per-charging-session alert state machine (plan.md Phase 4).
 *
 * Runtime-only, owned by the service -- never persisted to DataStore. A
 * process restart intentionally starts fresh at NotCharging/Charging rather
 * than resuming a pending repeat/snooze: a stale alert firing minutes after
 * an unexpected process death would be worse than simply not firing it.
 */
sealed class ChargingSessionState {
    data object NotCharging : ChargingSessionState()
    data object Charging : ChargingSessionState()

    /** [repeatCount] alerts have fired so far (0 = only the initial alert). A repeat is scheduled. */
    data class WaitingForRepeat(val repeatCount: Int) : ChargingSessionState()

    /** User pressed STOP. No further alerts this session regardless of battery state. */
    data object Acknowledged : ChargingSessionState()

    /** The alert fired its last permitted time (repeats disabled, or the configured cap was reached). */
    data object AlertSequenceEnded : ChargingSessionState()
}
