# ChargeAlert — Progress

Last updated: 2026-08-13

## Status: Phase 2 complete (code + unit tests verified; emulator validation pending, to be done by user in Android Studio)

## Phase 0 — Foundation ✅
Android project (Kotlin, Compose, Material 3, minSdk 26, compileSdk 35). Domain models (`BatteryState`, `AlertSettings`, `ChargingSessionState`). DataStore-backed `UserPreferencesRepository`. `BatteryRepository` reading `ACTION_BATTERY_CHANGED`. `MainActivity` + `HomeScreen` skeleton UI.

**Architecture correction:** the originally-specced "manifest receiver on `ACTION_POWER_CONNECTED` starts/stops the service" design was verified against current Android docs and found non-functional (that broadcast isn't exempt from background-execution/foreground-service-start restrictions). Replaced with: foreground service starts on an exempted trigger (user toggling the in-app switch, or `BOOT_COMPLETED` if already enabled) and stays alive continuously while the alert is enabled, not just during a charging session. Documented in `plan.md` §15.2.

`specialUse` foreground service type used, with a documented subtype string for Play review.

## Phase 1 — Battery Monitoring + Threshold Detection ✅
Fixed a real bug found during verification: `startForeground()` wasn't passing the explicit `FOREGROUND_SERVICE_TYPE_SPECIAL_USE` constant required by Android 14+ even though the manifest declared it — now uses `ServiceCompat.startForeground(...)`.

Added `ThresholdEvaluator` (pure, unit-tested): `isCharging && percentage >= threshold`. Wired into `BatteryMonitoringService` via `combine()` of live battery state + DataStore settings, so threshold changes apply without a restart. Fixed a duplicate-subscription bug (flow was being re-subscribed on every `onStartCommand`).

Emulator-validated: 100% battery, not charging → correctly no alert (see `plan.md` §16).

## Phase 2 — Alert Engine ✅
`AlertEngine` (pure, unit-tested): adds "already alerted this session" gating on top of `ThresholdEvaluator`. `BatteryAlertManager` (Android-specific dispatcher): posts a notification, plays default sound, and/or vibrates, each independently gated by `AlertSettings`. Notification permission denial is handled gracefully (mechanism skipped, no crash).

Two notification channels: `battery_monitoring` (low importance, silent, ongoing status) and `battery_alerts` (high importance, for actual threshold alerts) — kept separate because they need different importance levels.

`ChargingSessionState` now transitions through `NotCharging → Charging → AlertTriggered → WaitingForDisconnect → NotCharging`, guaranteeing exactly one alert per charging session and a reset on disconnect.

15 unit tests passing (`ThresholdEvaluatorTest`, `AlertEngineTest`, including an explicit duplicate-alert-sequence test). `./gradlew test` and `./gradlew assembleDebug` both succeed.

## Known Limitations
- Physical-device, OEM, and Doze-mode reliability not yet verified (Phase 5)
- Phase 2 alert behavior verified via unit tests and build only; full emulator run-through (charging simulation, notification/sound/vibration firing) is pending — user will validate in Android Studio
- `BootReceiver` won't fire if the app is in Android's "stopped" state (never opened / force-stopped) — platform limitation, not fixable

## Next Phase
Phase 3 — User Controls / Settings screen (not started).
