package com.chargealert.app.domain

/**
 * Per-charging-session alert state machine (plan.md section 8).
 *
 * Phase 0 only drives NOT_CHARGING <-> CHARGING transitions from real battery
 * events. THRESHOLD_REACHED / ALERT_TRIGGERED / WAITING_FOR_DISCONNECT are
 * wired up in Phase 2 (alert engine) and Phase 3 (session protection).
 */
sealed class ChargingSessionState {
    data object NotCharging : ChargingSessionState()
    data object Charging : ChargingSessionState()
    data object ThresholdReached : ChargingSessionState()
    data object AlertTriggered : ChargingSessionState()
    data object WaitingForDisconnect : ChargingSessionState()
}
