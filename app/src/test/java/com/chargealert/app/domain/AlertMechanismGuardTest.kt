package com.chargealert.app.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertMechanismGuardTest {

    @Test
    fun `all disabled is rejected`() {
        assertTrue(AlertMechanismGuard.wouldDisableAllMechanisms(false, false, false))
    }

    @Test
    fun `notification only is allowed`() {
        assertFalse(AlertMechanismGuard.wouldDisableAllMechanisms(true, false, false))
    }

    @Test
    fun `sound only is allowed`() {
        assertFalse(AlertMechanismGuard.wouldDisableAllMechanisms(false, true, false))
    }

    @Test
    fun `vibration only is allowed`() {
        assertFalse(AlertMechanismGuard.wouldDisableAllMechanisms(false, false, true))
    }

    @Test
    fun `all enabled is allowed`() {
        assertFalse(AlertMechanismGuard.wouldDisableAllMechanisms(true, true, true))
    }
}
