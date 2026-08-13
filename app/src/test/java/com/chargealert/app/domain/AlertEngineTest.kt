package com.chargealert.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertEngineTest {

    @Test
    fun `threshold not reached does not alert`() {
        assertFalse(
            AlertEngine.shouldAlert(
                percentage = 99,
                isCharging = true,
                threshold = 100,
                alreadyAlertedThisSession = false
            )
        )
    }

    @Test
    fun `threshold reached alerts`() {
        assertTrue(
            AlertEngine.shouldAlert(
                percentage = 100,
                isCharging = true,
                threshold = 100,
                alreadyAlertedThisSession = false
            )
        )
    }

    @Test
    fun `not charging does not alert even at threshold`() {
        assertFalse(
            AlertEngine.shouldAlert(
                percentage = 100,
                isCharging = false,
                threshold = 100,
                alreadyAlertedThisSession = false
            )
        )
    }

    @Test
    fun `custom threshold reached alerts`() {
        assertTrue(
            AlertEngine.shouldAlert(
                percentage = 90,
                isCharging = true,
                threshold = 90,
                alreadyAlertedThisSession = false
            )
        )
    }

    @Test
    fun `above custom threshold alerts`() {
        assertTrue(
            AlertEngine.shouldAlert(
                percentage = 95,
                isCharging = true,
                threshold = 90,
                alreadyAlertedThisSession = false
            )
        )
    }

    @Test
    fun `already alerted this session does not alert again`() {
        assertFalse(
            AlertEngine.shouldAlert(
                percentage = 100,
                isCharging = true,
                threshold = 100,
                alreadyAlertedThisSession = true
            )
        )
    }

    @Test
    fun `new charging session after previous alert can alert again`() {
        // Previous session already alerted...
        assertFalse(
            AlertEngine.shouldAlert(100, isCharging = true, threshold = 100, alreadyAlertedThisSession = true)
        )
        // ...charger disconnects, session resets (alreadyAlertedThisSession becomes false)...
        // ...new session reaches threshold again:
        assertTrue(
            AlertEngine.shouldAlert(100, isCharging = true, threshold = 100, alreadyAlertedThisSession = false)
        )
    }

    @Test
    fun `duplicate battery events at threshold only alert once`() {
        var alreadyAlerted = false
        val results = mutableListOf<Boolean>()

        // 99% charging -> no alert
        results += AlertEngine.shouldAlert(99, isCharging = true, threshold = 100, alreadyAlertedThisSession = alreadyAlerted)

        // 100% charging -> alert once, then session marks itself alerted
        val firstHit = AlertEngine.shouldAlert(100, isCharging = true, threshold = 100, alreadyAlertedThisSession = alreadyAlerted)
        results += firstHit
        if (firstHit) alreadyAlerted = true

        // Battery event fires again at 100%, still charging -> no repeat alert
        results += AlertEngine.shouldAlert(100, isCharging = true, threshold = 100, alreadyAlertedThisSession = alreadyAlerted)
        // Fires again -> still no repeat alert
        results += AlertEngine.shouldAlert(100, isCharging = true, threshold = 100, alreadyAlertedThisSession = alreadyAlerted)
        // Battery remains at 100% -> no repeat alert
        results += AlertEngine.shouldAlert(100, isCharging = true, threshold = 100, alreadyAlertedThisSession = alreadyAlerted)

        assertEquals(listOf(false, true, false, false, false), results)
    }
}
