package com.chargealert.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val ChargeAlertTypography = Typography()

/**
 * The battery percentage is the single largest, most identity-defining
 * element on screen (plan.md Phase 4 section 5) -- bigger than Material's
 * own displayLarge, with tight letter spacing so big digits don't look loose.
 */
val HeroPercentageStyle = TextStyle(
    fontSize = 92.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = (-2).sp
)

val SectionLabelStyle = TextStyle(
    fontSize = 13.sp,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = 1.5.sp
)
