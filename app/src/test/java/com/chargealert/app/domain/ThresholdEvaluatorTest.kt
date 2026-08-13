package com.chargealert.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ThresholdEvaluatorTest {

    @Test
    fun `99 percent charging threshold 100 is not reached`() {
        assertEquals(false, ThresholdEvaluator.isThresholdReached(99, isCharging = true, threshold = 100))
    }

    @Test
    fun `100 percent charging threshold 100 is reached`() {
        assertEquals(true, ThresholdEvaluator.isThresholdReached(100, isCharging = true, threshold = 100))
    }

    @Test
    fun `101 percent charging threshold 100 is reached`() {
        assertEquals(true, ThresholdEvaluator.isThresholdReached(101, isCharging = true, threshold = 100))
    }

    @Test
    fun `100 percent not charging threshold 100 is not reached`() {
        assertEquals(false, ThresholdEvaluator.isThresholdReached(100, isCharging = false, threshold = 100))
    }

    @Test
    fun `90 percent charging threshold 90 is reached`() {
        assertEquals(true, ThresholdEvaluator.isThresholdReached(90, isCharging = true, threshold = 90))
    }

    @Test
    fun `91 percent charging threshold 90 is reached`() {
        assertEquals(true, ThresholdEvaluator.isThresholdReached(91, isCharging = true, threshold = 90))
    }

    @Test
    fun `89 percent charging threshold 90 is not reached`() {
        assertEquals(false, ThresholdEvaluator.isThresholdReached(89, isCharging = true, threshold = 90))
    }
}
