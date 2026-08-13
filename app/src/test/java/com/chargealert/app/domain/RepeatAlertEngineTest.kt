package com.chargealert.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RepeatAlertEngineTest {

    private val noRepeat = RepeatRule(repeatEnabled = false, repeatIntervalMinutes = 5, maxRepeats = 3, snoozeMinutes = 5)
    private val withRepeat = RepeatRule(repeatEnabled = true, repeatIntervalMinutes = 5, maxRepeats = 3, snoozeMinutes = 5)

    // --- Initial alert ---

    @Test
    fun `threshold reached fires initial alert`() {
        val t = RepeatAlertEngine.onBatteryUpdate(
            current = ChargingSessionState.Charging,
            isCharging = true,
            thresholdReached = true,
            rule = noRepeat
        )
        assertTrue(t.fireAlert)
    }

    @Test
    fun `threshold not reached does not alert`() {
        val t = RepeatAlertEngine.onBatteryUpdate(
            current = ChargingSessionState.Charging,
            isCharging = true,
            thresholdReached = false,
            rule = noRepeat
        )
        assertFalse(t.fireAlert)
        assertEquals(ChargingSessionState.Charging, t.newState)
    }

    // --- Repeat disabled: exactly one alert, matches old AlertEngine behavior ---

    @Test
    fun `repeat disabled fires only the initial alert and ends the sequence`() {
        val t = RepeatAlertEngine.onBatteryUpdate(
            current = ChargingSessionState.Charging,
            isCharging = true,
            thresholdReached = true,
            rule = noRepeat
        )
        assertTrue(t.fireAlert)
        assertNull(t.scheduleDelayMinutes)
        assertEquals(ChargingSessionState.AlertSequenceEnded, t.newState)
    }

    // --- Repeat enabled: initial + scheduled repeat ---

    @Test
    fun `repeat enabled schedules a repeat after the initial alert`() {
        val t = RepeatAlertEngine.onBatteryUpdate(
            current = ChargingSessionState.Charging,
            isCharging = true,
            thresholdReached = true,
            rule = withRepeat
        )
        assertTrue(t.fireAlert)
        assertEquals(5, t.scheduleDelayMinutes)
        assertEquals(ChargingSessionState.WaitingForRepeat(0), t.newState)
    }

    @Test
    fun `repeat timer firing dispatches the next repeat and reschedules`() {
        val t = RepeatAlertEngine.onRepeatTimerFired(ChargingSessionState.WaitingForRepeat(0), withRepeat)
        assertTrue(t.fireAlert)
        assertEquals(5, t.scheduleDelayMinutes)
        assertEquals(ChargingSessionState.WaitingForRepeat(1), t.newState)
    }

    // --- Maximum repeats ---

    @Test
    fun `maximum repeats stops scheduling after the last permitted repeat`() {
        // initial (firing #0), repeat1, repeat2, repeat3 all fire; no repeat4 scheduled.
        val initial = RepeatAlertEngine.onBatteryUpdate(
            ChargingSessionState.Charging, isCharging = true, thresholdReached = true, rule = withRepeat
        )
        assertTrue(initial.fireAlert)
        assertEquals(ChargingSessionState.WaitingForRepeat(0), initial.newState)

        val repeat1 = RepeatAlertEngine.onRepeatTimerFired(initial.newState, withRepeat)
        assertTrue(repeat1.fireAlert)
        assertEquals(ChargingSessionState.WaitingForRepeat(1), repeat1.newState)

        val repeat2 = RepeatAlertEngine.onRepeatTimerFired(repeat1.newState, withRepeat)
        assertTrue(repeat2.fireAlert)
        assertEquals(ChargingSessionState.WaitingForRepeat(2), repeat2.newState)

        val repeat3 = RepeatAlertEngine.onRepeatTimerFired(repeat2.newState, withRepeat)
        assertTrue(repeat3.fireAlert)
        assertNull(repeat3.scheduleDelayMinutes)
        assertEquals(ChargingSessionState.AlertSequenceEnded, repeat3.newState)

        // A further timer fire (shouldn't happen, but must be a safe no-op) does nothing.
        val extra = RepeatAlertEngine.onRepeatTimerFired(repeat3.newState, withRepeat)
        assertFalse(extra.fireAlert)
    }

    // --- STOP ---

    @Test
    fun `stop acknowledges and cancels the schedule`() {
        val t = RepeatAlertEngine.onStop(ChargingSessionState.WaitingForRepeat(0))
        assertEquals(ChargingSessionState.Acknowledged, t.newState)
        assertTrue(t.cancelSchedule)
        assertFalse(t.fireAlert)
    }

    @Test
    fun `stop when nothing is waiting is a no-op`() {
        val t = RepeatAlertEngine.onStop(ChargingSessionState.Charging)
        assertEquals(ChargingSessionState.Charging, t.newState)
        assertFalse(t.cancelSchedule)
    }

    // --- SNOOZE ---

    @Test
    fun `snooze reschedules using the snooze interval`() {
        val t = RepeatAlertEngine.onSnooze(ChargingSessionState.WaitingForRepeat(0), withRepeat)
        assertEquals(ChargingSessionState.WaitingForRepeat(0), t.newState)
        assertTrue(t.cancelSchedule)
        assertEquals(5, t.scheduleDelayMinutes)
        assertFalse(t.fireAlert)
    }

    // --- Disconnect ---

    @Test
    fun `disconnect while alerting cancels the sequence`() {
        val t = RepeatAlertEngine.onBatteryUpdate(
            current = ChargingSessionState.WaitingForRepeat(1),
            isCharging = false,
            thresholdReached = false,
            rule = withRepeat
        )
        assertEquals(ChargingSessionState.NotCharging, t.newState)
        assertTrue(t.cancelSchedule)
    }

    @Test
    fun `disconnect while snoozed cancels the pending repeat`() {
        val snoozed = RepeatAlertEngine.onSnooze(ChargingSessionState.WaitingForRepeat(0), withRepeat).newState
        val t = RepeatAlertEngine.onBatteryUpdate(snoozed, isCharging = false, thresholdReached = false, rule = withRepeat)
        assertEquals(ChargingSessionState.NotCharging, t.newState)
        assertTrue(t.cancelSchedule)
    }

    @Test
    fun `disconnect when already not charging is a no-op`() {
        val t = RepeatAlertEngine.onBatteryUpdate(
            ChargingSessionState.NotCharging,
            isCharging = false,
            thresholdReached = false,
            rule = withRepeat
        )
        assertFalse(t.cancelSchedule)
    }

    // --- Duplicate battery events ---

    @Test
    fun `duplicate battery events while already alerted never create duplicate schedules`() {
        val afterInitial = RepeatAlertEngine.onBatteryUpdate(
            ChargingSessionState.Charging, isCharging = true, thresholdReached = true, rule = withRepeat
        ).newState

        val duplicate1 = RepeatAlertEngine.onBatteryUpdate(afterInitial, isCharging = true, thresholdReached = true, rule = withRepeat)
        val duplicate2 = RepeatAlertEngine.onBatteryUpdate(afterInitial, isCharging = true, thresholdReached = true, rule = withRepeat)

        assertFalse(duplicate1.fireAlert)
        assertNull(duplicate1.scheduleDelayMinutes)
        assertFalse(duplicate2.fireAlert)
        assertEquals(afterInitial, duplicate1.newState)
        assertEquals(afterInitial, duplicate2.newState)
    }

    // --- New session after a previous one alerted ---

    @Test
    fun `new charging session after disconnect can alert again`() {
        val alerted = RepeatAlertEngine.onBatteryUpdate(
            ChargingSessionState.Charging, isCharging = true, thresholdReached = true, rule = noRepeat
        ).newState
        assertEquals(ChargingSessionState.AlertSequenceEnded, alerted)

        val disconnected = RepeatAlertEngine.onBatteryUpdate(
            alerted, isCharging = false, thresholdReached = false, rule = noRepeat
        ).newState
        assertEquals(ChargingSessionState.NotCharging, disconnected)

        val newSession = RepeatAlertEngine.onBatteryUpdate(
            disconnected, isCharging = true, thresholdReached = true, rule = noRepeat
        )
        assertTrue(newSession.fireAlert)
    }
}
