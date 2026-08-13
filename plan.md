# Battery Charging Alert — Project Plan

## 1. Project Overview

### Working Name

**ChargeAlert** — name can be changed later.

### Core Idea

A lightweight Android application that monitors the device's charging state and alerts the user when the battery reaches a configured percentage, with **100% as the default**.

The user can decide what happens when the threshold is reached:

* 🔊 Play a sound
* 📳 Vibrate
* 🔊 + 📳 Sound and vibration
* 🔔 Show a notification
* Or disable the alert entirely

The application should be:

* Local-first
* Lightweight
* No account required
* No backend
* No unnecessary permissions
* Open source
* Simple enough to understand and contribute to
* Reliable even when the app UI is not open

---

# 2. Main Goal

The first version should solve exactly one problem:

> **"Tell me when my phone has finished charging."**

The application should not initially try to become a battery-health dashboard, charging optimizer, device monitor, or battery analytics platform.

Those can potentially come later.

---

# 3. Target Platform

## Android

Recommended stack:

* **Kotlin**
* **Jetpack Compose**
* Android SDK
* Material 3
* DataStore for preferences
* Android notification APIs
* BroadcastReceiver for battery/charging events
* Vibrator/VibrationEffect APIs
* Android audio/ringtone APIs

No backend required.

No Firebase required.

No authentication required.

No internet connection should be required for the core functionality.

---

# 4. MVP Feature Set

The MVP should contain only the features necessary to make the app genuinely useful.

## 4.1 Battery Percentage

Display the current battery percentage.

Example:

> 🔋 73%

The percentage should update while charging.

---

## 4.2 Charging State

Display whether the device is:

* Charging
* Not charging
* Fully charged

Example:

> 🔌 Charging

or

> ⚡ Fully Charged

---

## 4.3 Configurable Alert Threshold

Default:

> **100%**

User can optionally change it.

Potential range:

> 50% → 100%

For MVP, 100% can be the primary/default experience.

Later versions can allow arbitrary thresholds.

---

## 4.4 Alert Types

Allow the user to choose:

### Notification

Shows a notification such as:

> 🔋 Battery Fully Charged
> Your battery has reached 100%.

### Sound

Play the selected alert sound.

### Vibration

Trigger vibration.

### Sound + Vibration

Perform both.

---

## 4.5 Enable / Disable

Main toggle:

> **Battery Alert: ON**

Turning it off should completely disable the charging alert.

Turning it back on should restore the user's previous configuration.

---

## 4.6 Trigger Only While Charging

The alert should only activate when:

```text
battery >= threshold
AND
device is connected to charger
```

This prevents weird behavior if the battery percentage changes while the phone is not charging.

---

## 4.7 Trigger Once Per Charging Session

This is extremely important.

Example:

```text
Phone = 100%
↓
Alert
↓
Battery remains 100%
↓
DO NOT alert again every few seconds
```

The app should remember:

> "I already alerted during this charging session."

Once the charger is disconnected:

```text
Charging session ended
↓
Reset alert state
↓
Next charging session can trigger another alert
```

---

# 5. Suggested Main UI

Keep the first screen extremely simple.

```text
┌──────────────────────────────┐
│                              │
│        ChargeAlert            │
│                              │
│           🔋 73%             │
│        ⚡ Charging            │
│                              │
│    Battery Alert              │
│                              │
│    Alert at       100%        │
│                              │
│    🔊 Sound       ON          │
│    📳 Vibration   ON          │
│                              │
│    Alert Sound   Default      │
│                              │
│                              │
│        ● ENABLED             │
│                              │
└──────────────────────────────┘
```

The UI should communicate the state immediately.

Avoid dashboards full of unnecessary numbers.

---

# 6. Architecture

Use a relatively clean architecture, but **do not over-engineer it**.

Suggested structure:

```text
app/
├── data/
│   ├── BatteryRepository
│   ├── BatteryMonitor
│   └── UserPreferencesRepository
│
├── domain/
│   ├── BatteryState
│   ├── AlertSettings
│   └── ChargingSession
│
├── notifications/
│   ├── BatteryAlertManager
│   └── NotificationHelper
│
├── audio/
│   └── AlertSoundManager
│
├── vibration/
│   └── VibrationManager
│
├── ui/
│   ├── HomeScreen
│   ├── SettingsScreen
│   └── components/
│
└── MainActivity
```

This is enough structure to keep things maintainable without turning a tiny app into enterprise architecture.

---

# 7. Battery Monitoring

The core Android functionality will revolve around battery/charging state broadcasts, delivered via the foreground-service strategy decided in [15.2](#152-battery-monitoring-strategy--decided).

`STATUS_FULL` (from `BatteryManager.EXTRA_STATUS`) should be treated as a secondary/optional signal only — many devices trickle-charge at 100% without ever reporting it. The primary trigger is always `percentage >= threshold`.

The app needs to detect:

```text
Battery percentage
Charging state
Charging connected/disconnected
Full battery state
```

Conceptually:

```text
Battery Event
     ↓
Battery Monitor
     ↓
Current BatteryState
     ↓
Alert Logic
     ↓
Trigger Alert
```

---

# 8. Alert State Machine

Instead of simply checking:

```text
if battery == 100%
    alert()
```

implement a small state machine.

### Example

```text
NOT_CHARGING
      ↓
CHARGING
      ↓
THRESHOLD_REACHED
      ↓
ALERT_TRIGGERED
      ↓
WAITING_FOR_UNPLUG
      ↓
NOT_CHARGING
```

This prevents repeated alerts.

---

# 9. Charging Session Logic

Every charging session gets its own state.

Example:

```text
09:00
Battery = 40%
Charger connected

↓
Charging

10:30
Battery = 80%

↓
Charging

11:15
Battery = 100%

↓
Alert triggered

11:16
Battery remains 100%

↓
No additional alert

11:30
Charger disconnected

↓
Reset session
```

Next time:

```text
Charger connected
↓
New charging session
↓
Alert can trigger again
```

---

# 10. Persistent Settings

Use **DataStore Preferences**.

Store settings such as:

```text
alertEnabled
threshold
soundEnabled
vibrationEnabled
notificationEnabled
selectedSound
vibrationPattern
```

Potential future settings:

```text
repeatAlert
repeatInterval
quietHours
notificationMessage
```

---

# 11. Notifications

Android notification behavior needs to be handled properly.

The app should create a dedicated notification channel.

Example:

> Battery Alerts

Possible notification:

```text
🔋 Battery Fully Charged

Your battery has reached 100%.

[Dismiss]
```

Later:

> Battery reached 80%

if custom thresholds are enabled.

The application should also handle Android notification permission requirements on supported Android versions.

---

# 12. Sound System

The user should eventually be able to select:

### Built-in sounds

```text
Default
Soft Chime
Bell
Digital
Success
None
```

Potentially allow:

> Choose from device audio

Later.

Important:

The app should respect Android audio behavior rather than blindly playing sound at maximum volume.

---

# 13. Vibration System

Possible patterns:

### Short

```text
• 
```

### Double

```text
••
```

### Long

```text
——
```

### Strong

```text
——••
```

Later versions can expose vibration customization.

---

# 14. Important Android Reliability Considerations

This is where the project becomes more interesting.

A naive implementation may work perfectly while the application is open but fail after the user closes it. The foreground-service strategy decided in [15.2](#152-battery-monitoring-strategy--decided) addresses the biggest risk here up front, but the following still need real-device verification in Phase 5:

* Doze mode / App Standby Buckets
* Battery optimization (should be unnecessary with the foreground service, but verify)
* Android version differences
* Do Not Disturb
* Notification permissions (denied `POST_NOTIFICATIONS` fallback path)
* OEM battery management
* Samsung background restrictions
* Xiaomi/MIUI restrictions
* OnePlus/OxygenOS behavior
* Real-device testing

Do **not** assume:

> "It works in Android Studio, therefore it works on every phone."

Testing on actual devices will matter.

---

# 15. Phase 0 — Project Setup & Architecture Decisions

## Goal

Create the basic Android project and lock in the architectural decisions that everything else depends on, so Phase 1+ isn't retrofitted later.

## 15.1 Project Setup

Tasks:

* Create GitHub repository
* Create Android project
* Configure Kotlin
* Configure Jetpack Compose
* Configure Material 3
* Create basic application theme
* Create initial README
* Add `.gitignore`
* Create initial commit

Repository:

```text
charge-alert/
```

Possible branches:

```text
main
develop
feature/*
```

For a small solo project, even just `main` + feature branches is enough.

## 15.2 Battery Monitoring Strategy — DECIDED (revised)

**Foreground service, active whenever the alert feature is enabled — not only during an active charging session.**

The original plan ("manifest receiver on `ACTION_POWER_CONNECTED` starts/stops the service per session") was verified against current Android documentation and found unreliable:

* `ACTION_POWER_CONNECTED` / `ACTION_POWER_DISCONNECTED` are **not** in Android's implicit-broadcast manifest exemption list (API 26+), so a manifest-declared receiver for them will not fire once the app is no longer running. ([Broadcast exceptions](https://developer.android.com/develop/background-work/background-tasks/broadcasts/broadcast-exceptions))
* Even if it fired, starting a foreground service from that receiver is not in the documented background-start exemption list for Android 12+, risking `ForegroundServiceStartNotAllowedException`. ([Background-start restrictions](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start))

Corrected flow:

```text
User toggles "Battery Alert: ON" in the app UI      (exempt: user interaction with app UI)
        OR
Device reboots + alert was already enabled          (exempt: ACTION_BOOT_COMPLETED)
        ↓
Start foreground service — stays alive continuously while alert is enabled
        ↓
Service dynamically registers ACTION_BATTERY_CHANGED receiver (code, not manifest)
        ↓
While not charging: low-priority "Monitoring" notification, minimal work
While charging: notification shows live status, evaluates threshold
        ↓
User toggles "Battery Alert: OFF"
        ↓
Service stops itself, unregisters receiver
```

Tradeoff accepted: the service is no longer fully absent while not charging — it's a lightweight always-on listener while the feature is enabled. This is the pattern real charging-alert apps use, and is necessary because the session-scoped start/stop trigger does not reliably fire on modern Android. Rejected alternative: WorkManager periodic polling (no persistent service) — avoids any idle notification, but the minimum ~15-minute interval means the alert could fire up to 15 minutes late, undermining the app's core value proposition.

## 15.3 Alert State Model — DECIDED

State machine per section 8 (`NOT_CHARGING → CHARGING → THRESHOLD_REACHED → ALERT_TRIGGERED → WAITING_FOR_UNPLUG → NOT_CHARGING`), implemented as a sealed class in `domain/ChargingSession`.

Held **in-memory only**, owned by the foreground service. Not persisted to DataStore — if the process dies mid-session, the service (and its state) restarts fresh, which is the semantically correct behavior anyway (no misleading "already alerted" carried across a real restart).

## 15.4 Permissions — DECIDED

Minimal set:

* `POST_NOTIFICATIONS` (Android 13+, runtime-requested). If denied, fall back to sound/vibration-only — must not silently break.
* `FOREGROUND_SERVICE` + a specific-use foreground service type (confirm which type — `FOREGROUND_SERVICE_SPECIAL_USE` vs. `FOREGROUND_SERVICE_HEALTH` — against current Play policy before submission).
* `VIBRATE` (normal permission, no runtime prompt).
* No battery-optimization-exemption request in v1.0. Add only if Phase 5 real-device testing shows the foreground service alone isn't sufficient on a given OEM.

## 15.5 Persistence — DECIDED

DataStore Preferences for user settings only (section 10's list: `alertEnabled`, `threshold`, `soundEnabled`, etc.). Session/alert state is explicitly *not* persisted (see 15.3).

---

# 16. Phase 1 — Basic Battery Information

## Goal

Get battery information working.

Implement:

* Current battery percentage
* Charging state
* Charger connected/disconnected
* Fully charged detection

Create:

```text
BatteryState
```

Example:

```text
percentage = 87
isCharging = true
isFull = false
```

Display this in Compose.

### Completion condition

The application correctly shows:

```text
Battery: 87%
Status: Charging
```

and updates when the battery changes.

### Emulator validation — 2026-08-13

Built and launched successfully in an Android Studio emulator (Pixel 9 API 35):

* App launched without crash.
* Battery percentage correctly detected and displayed: **100%**.
* Emulator correctly reported **not charging**.
* Foreground monitoring service ran successfully; persistent notification showed "ChargeAlert monitoring" / "Waiting for charger to be connected".
* No threshold alert fired despite battery being at 100%, because `isCharging == false` — confirms the `percentage >= threshold AND isCharging` trigger condition holds correctly (`100 >= 100 → true`, `isCharging → false` → no trigger).

Physical-device, OEM, and Doze testing remain pending for Phase 5.

---

# 17. Phase 2 — Basic Alert

## Goal

Make the actual idea work.

Implement:

```text
threshold = 100%
```

When:

```text
isCharging == true
AND
percentage >= threshold
```

trigger:

* Notification
* Sound
* Vibration

For the first implementation, even a fixed alert is fine.

### Completion condition

Put phone on charger.

Wait.

Reach 100%.

Phone alerts.

🎉 MVP technically works.

---

# 18. Phase 3 — Charging Session Protection

## Goal

Prevent repeated alerts.

Implement:

```text
alertTriggered = false
```

When threshold is reached:

```text
if !alertTriggered:
    triggerAlert()
    alertTriggered = true
```

When charger disconnects:

```text
alertTriggered = false
```

Test:

```text
100%
↓
Alert

100%
↓
No alert

100%
↓
No alert

Unplug
↓
Reset

Plug in again
↓
Alert can trigger again
```

This phase is critical.

---

# 19. Phase 4 — Settings

## Goal

Give the user control.

Add:

### Main toggle

```text
Battery Alert
ON / OFF
```

### Threshold

```text
Alert when battery reaches:

[ 100% ]
```

### Alert method

```text
☑ Notification
☑ Sound
☑ Vibration
```

### Sound

```text
Alert Sound
Default >
```

Save everything using DataStore.

---

# 20. Phase 5 — Background Reliability

## Goal

Make the app useful when the UI isn't open.

Test:

```text
Open app
Enable alert
Close app
Lock phone
Connect charger
Wait
```

The alert should still work.

Then test:

```text
Force close app
Restart phone
Battery optimization enabled
Battery optimization disabled
Screen locked
Screen off
```

Document which Android restrictions affect behavior.

This phase is likely to uncover the most annoying bugs 😂.

---

# 21. Phase 6 — Polished UI

Once functionality is stable, improve the UI.

Add:

* Clean battery indicator
* Charging animation
* Current percentage
* Large enable/disable switch
* Settings page
* Alert preview
* Material 3 design
* Dark mode
* Proper accessibility labels

Possible home screen:

```text
             🔋

            87%

         Charging...

    ────────────────
       Alert at 100%

       [ ENABLED ]

    Sound        🔊
    Vibration    📳
    Notification 🔔
```

Keep it visually calm.

---

# 22. Phase 7 — Advanced Alerts

Optional features.

### Custom threshold

Allow:

```text
50%
60%
70%
80%
90%
95%
100%
```

Potentially a slider.

---

### Custom sound

Allow user to choose:

```text
System ringtone
Notification sound
Local audio file
Built-in sounds
```

---

### Custom vibration

Allow:

```text
Short
Double
Long
Custom
```

---

### Repeat alert

Optional:

```text
Repeat every:
[ 5 minutes ]
```

This should be OFF by default.

---

# 23. Phase 8 — Smart Features

Only add these if the core app is already reliable.

Possible features:

## Quiet Hours

Example:

```text
Do not alert between:

11:00 PM
     ↓
07:00 AM
```

The app can still record that the threshold was reached, but avoid making noise.

---

## Charging History

Simple local history:

```text
Today

10:42 PM — Charging started
12:31 AM — 100% reached
```

Later:

```text
Average charging time
```

No cloud required.

---

## Battery Temperature

Potentially display:

```text
Temperature: 34°C
```

But this should be treated as optional information rather than the application's core purpose.

---

# 24. Phase 9 — Testing

Create tests for the alert logic.

Important cases:

### Case 1

```text
Charging
99%
→ No alert
```

### Case 2

```text
Charging
100%
→ Alert
```

### Case 3

```text
100%
Already alerted
→ No second alert
```

### Case 4

```text
Unplug
→ Reset
```

### Case 5

```text
Plug back in
→ New session
```

### Case 6

```text
Alert disabled
→ No alert
```

### Case 7

```text
Threshold = 90
Battery = 90
Charging
→ Alert
```

### Case 8

```text
Battery = 90
Not charging
→ No alert
```

---

# 25. Real Device Testing

Test on multiple Android versions/devices if possible.

At minimum:

```text
Your personal Android phone
Another Android phone
Android emulator
```

Ideally test different OEM behavior.

Record:

```text
Device
Android version
Battery optimization
Notification permission
Result
```

This can become part of the GitHub README.

---

# 26. Phase 10 — Open Source Release

Before publishing v1.0:

### README

Include:

* What the app does
* Screenshots
* Features
* Installation
* Permissions
* Architecture
* Supported Android versions
* Known limitations
* Contributing
* License

---

## Suggested README structure

```text
# ChargeAlert 🔋

A lightweight open-source Android app that alerts
you when your battery reaches your configured
charging threshold.

## Features

- Battery-full alerts
- Sound
- Vibration
- Notifications
- Custom threshold
- Local settings
- No account
- No backend
- Open source

## Screenshots

...

## Installation

...

## Architecture

...

## Permissions

...

## Known limitations

...

## Contributing

...

## License
```

---

# 27. Version Roadmap

## v0.1 — Prototype

```text
✓ Battery percentage
✓ Charging state
✓ Basic alert
```

---

## v0.2 — Reliable Core

```text
✓ Charging session detection
✓ Alert once per session
✓ Notification
✓ Sound
✓ Vibration
```

---

## v0.3 — User Controls

```text
✓ Enable/disable
✓ Custom threshold
✓ Sound selection
✓ Vibration selection
✓ Persistent settings
```

---

## v0.4 — Reliability

```text
✓ Background testing
✓ Android permission handling
✓ Battery optimization investigation
✓ OEM testing
✓ Edge-case fixes
```

---

## v1.0 — Public Release

```text
✓ Polished UI
✓ Stable monitoring
✓ Settings
✓ Documentation
✓ Tests
✓ README
✓ Screenshots
✓ GitHub release
```

---

## v1.x — Optional Features

```text
○ Quiet hours
○ Charging history
○ Custom sounds
○ Custom vibration patterns
○ Charging statistics
○ Battery temperature
```

---

# 28. Things NOT to Build Initially

This is important.

Do NOT start with:

```text
❌ Login
❌ Firebase
❌ Backend
❌ Cloud synchronization
❌ AI
❌ Account system
❌ Battery optimization claims
❌ Battery health diagnosis
❌ Complex analytics
❌ Widgets
❌ Wear OS
❌ iOS version
```

The project should first become:

> **A stupidly reliable battery-full alarm.**

Then expand.

---

# 29. Potential Differentiation

Since battery-full alarm applications already exist, the project's selling point should not be:

> "Nobody has made this."

Instead:

> **"A simple, privacy-friendly, open-source alternative without unnecessary features."**

Potential principles:

### Privacy

No battery information leaves the device.

### Simplicity

One purpose.

### Transparency

Open source.

### Lightweight

Minimal background work.

### No unnecessary permissions

Only request what the feature actually needs.

---

# 30. Future Possibilities

If the project unexpectedly becomes popular, it could evolve into:

### Charging Companion

```text
Battery alerts
+
Charging history
+
Battery temperature
+
Charging speed
+
Charging statistics
```

Or:

### Automation

Allow actions such as:

```text
Battery reaches 100%
        ↓
Sound
        +
Vibration
        +
Notification
        +
Launch custom action
```

Potential integration with Android automation applications could also be explored.

---

# 31. Development Strategy

The recommended development order is:

```text
1. Create project + lock architecture decisions (Phase 0)
       ↓
2. Read battery state
       ↓
3. Detect charging
       ↓
4. Detect threshold
       ↓
5. Trigger alert
       ↓
6. Prevent repeated alerts
       ↓
7. Save settings
       ↓
8. Build settings UI
       ↓
9. Test background behavior
       ↓
10. Polish UI
       ↓
11. Test real devices
       ↓
12. Release v1.0
```

Do not jump to advanced features before step 9. Architecture decisions in step 1 (monitoring strategy, permissions, persistence) are made once, up front — not revisited during step 9, only verified there.

---

# 32. Definition of "Done"

The project can be considered successfully completed when:

* The user installs the app.
* Opens it once.
* Enables the alert.
* Selects their preferred alert behavior.
* Locks the phone.
* Connects the charger.
* Walks away.
* Battery reaches the configured threshold.
* The phone reliably alerts them.
* The alert happens only once during that charging session.
* Unplugging resets the session.
* The next charging session works again.
* Settings survive app restarts.
* No internet connection is required.

At that point:

**The project is DONE.**

Everything after that is optional polish.

---

# 33. Final Project Philosophy

Keep reminding yourself:

> **Small project ≠ bad project.**

The purpose of this project is not to compete with massive Android applications.

It is an opportunity to demonstrate that you can:

* Identify a real-world annoyance.
* Design a focused solution.
* Work with Android system APIs.
* Handle background behavior.
* Persist application state.
* Build a clean UI.
* Test edge cases.
* Package and document an application.
* Publish something genuinely usable.

The strongest version of this project is not the one with 50 features.

It is the one where someone says:

> "I installed it yesterday and it actually works exactly how I wanted."

---

# 34. First Milestone

The very first milestone should be extremely small:

**Milestone 1:**

```text
Create Android project
        ↓
Show battery %
        ↓
Show charging/not charging
        ↓
Detect 100%
        ↓
Print/log "BATTERY FULL"
```

Once that works:

**then build the actual alert.**

No fancy UI yet.

No settings yet.

No architecture rabbit hole.

Just make the phone tell us:

> **"BRO YOUR BATTERY IS FULL."** 🔋💀
