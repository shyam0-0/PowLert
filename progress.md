# ChargeAlert — Progress

Last updated: 2026-08-14

## Status: Phase 4 complete (emulator-validated)

## Phase 0 — Foundation ✅
Android project (Kotlin, Compose, Material 3, minSdk 26, compileSdk 35). Domain models, DataStore-backed `UserPreferencesRepository`, `BatteryRepository`.

**Architecture correction:** the originally-specced "manifest receiver on `ACTION_POWER_CONNECTED` starts/stops the service" design was verified against current Android docs and found non-functional (not exempt from background-execution/foreground-service-start restrictions). Replaced with: service starts on an exempted trigger (user toggle, or `BOOT_COMPLETED` if already enabled) and stays alive while the alert is enabled (`plan.md` §15.2). Uses `specialUse` foreground service type.

## Phase 1 — Battery Monitoring + Threshold Detection ✅
Fixed a real bug: `startForeground()` wasn't passing the explicit type constant Android 14+ requires — now uses `ServiceCompat.startForeground(...)`. Added `ThresholdEvaluator` (pure, unit-tested), combined with DataStore settings so threshold changes apply live. Fixed a duplicate flow-subscription bug.

## Phase 2 — Alert Engine ✅
Dispatch layer (`BatteryAlertManager`) for notification/sound/vibration, each gated by `AlertSettings`; permission denial degrades gracefully. Two notification channels (monitoring = low, alerts = high).

**Bug fixed in Phase 3:** the alert channel reused the Phase 0/1 channel ID; Android makes channel importance immutable after creation, so it stayed stuck at LOW. Renamed to `battery_full_alerts`.

## Phase 3 — Premium Dashboard & Settings ✅
Added `HomeViewModel` (MVVM: Compose never touches DataStore/service directly). Threshold slider, alert-method toggles, sound picker, Test Alert, `AlertMechanismGuard` blocking the last alert method from being disabled, notification-permission banner.

## Phase 4 — Alert Experience + Visual Redesign ✅
**Redesign:** removed the card-per-section pattern. Hero is now card-less with a 92sp percentage, status pill, and a custom `Canvas` progress track with a threshold marker. Sections are typography + divider driven (`Spacing`/`SectionLabelStyle` tokens). All emoji iconography replaced with Material vector icons.

**Alert engine rewrite:** `RepeatAlertEngine` (pure, 14 unit tests) supersedes the Phase 2 `AlertEngine` — one implementation, not two; `repeatEnabled=false` reproduces the old fire-once behavior. Returns a `Transition` (fire / schedule / cancel) that the service executes. Repeat scheduling is a single cancel-then-schedule coroutine `delay()` in the service's own scope — no WorkManager/AlarmManager, and structurally impossible to double-schedule.

**New:** custom sound via `OpenDocument` + persisted URI permission (falls back to default if the URI dies), sound preview (`SoundPreviewPlayer`, stops on dismiss, never touches session state), repeat/interval/max-repeats/snooze settings, notification STOP/SNOOZE actions, and an in-app Stop/Snooze banner for when notification permission is denied.

26 unit tests passing. Emulator-verified live: initial alert fires at threshold; repeats fire on schedule (0→1→2→3 at 1-min intervals); cap ends the sequence; STOP stops repeats; disconnect mid-sequence cancels the pending repeat; a new session alerts again.

## Known Limitations
- Physical-device, OEM, and Doze-mode reliability not yet verified (Phase 5)
- Snooze verified by unit test + code path, not end-to-end on emulator (STOP was, via the same intent mechanism)
- `MonitoringRow` reflects the persisted preference, not a live service-health check
- Pending snooze/repeat intentionally not persisted — a process restart drops it rather than firing a stale alert
- `BootReceiver` won't fire if the app is in Android's "stopped" state

## Next Phase
Phase 5 (not started) — background/OEM/Doze reliability on physical devices.
