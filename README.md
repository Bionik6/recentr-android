# Recentr — Android

Screen-time & focus app. Native Android port (Kotlin, Jetpack Compose, Material 3) of the iOS
Figma design *Offly · Screen time App UI Kit* — design system, Onboarding, Home & Insight, Rules,
Profile & Settings and Focus timer flows.

## Requirements

- Android Studio (bundled JDK) — or any JDK 17+ as `JAVA_HOME`
- Android SDK with platform 37 (compileSdk 37, minSdk 28, targetSdk 36)

## Build & run

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:installDebug
```

Debug-only shortcuts (QA):

```bash
# skip onboarding and open a tab
adb shell am start -S -n com.recentr.app/.MainActivity --es start main --es tab rules
# force the onboarding
adb shell am start -S -n com.recentr.app/.MainActivity --es start onboarding
```

In debug builds, **Profile › Settings › Developer** has *Replay onboarding* and *Design system*
(living catalogue of every token and component). The focus timer opens from **Rules › ⋯ › New
rule › Focus**.

## Modules

| Module | Content |
|---|---|
| `:app` | `MainActivity` (splash, edge-to-edge, launcher icon aliases), root `NavHost`, tab shell with the floating tab bar, Squads placeholder |
| `:core:designsystem` | Figma foundations (colour, Figtree typography, spacing, radius, effects, gradients), icons, stickers, mascot, backdrops and all components — see [docs/DESIGN_SYSTEM.md](docs/DESIGN_SYSTEM.md) |
| `:core:model` | Domain models (`UserProfile`, `Rule`, `ScreenTimeSummary`, `AppUsage` / `UsageApp`, `AppId`) |
| `:core:data` | Repositories: `UserPreferencesRepository` and `SettingsRepository` (DataStore), `RulesRepository`, `FocusRepository`, `ScreenTimeRepository` (device usage events or demo data) and the `RecentrData` locator |
| `:feature:onboarding` | 17-screen onboarding (Figma `2023:578`) |
| `:feature:home` | Home & Insight (Figma `2025:1422`) |
| `:feature:rules` | Rules list, editors, sheets and templates (Figma `2027:2433`) |
| `:feature:profile` | Profile tab, edit sheet, weekly recap, Recentr Plus & subscription, Settings with Help, Appearance, Notifications, Language, log out and delete account (Figma `2033:6330`) |
| `:feature:focus` | Focus timer: set-up dial, sounds / rounds / apps sheets, running session, break, app on hold, session complete and focus log (Figma `2114:15883`) |

## Status / known limits

- Screen time: with Usage Access granted, `UsageStatsScreenTimeRepository` aggregates the device's
  `UsageStatsManager.queryEvents` (foreground time per hour / day, per-app totals, pickups from
  unlocks or screen-ons, `NOTIFICATION_INTERRUPTION` events, longest screen-off stretch); without
  it, `DemoScreenTimeRepository` shows the Figma values. The access is re-checked on every resume
  (`RecentrData.recheckUsageAccess()`), so granting it from the onboarding switches Home live. The
  aggregation is pure (`ScreenTimeAggregation`) and unit-tested:
  `./gradlew :core:data:testDebugUnitTest`.
  - Home-screen (launcher) and System UI time is not screen time. Package visibility uses
    `<queries>` for launcher / home intents (no `QUERY_ALL_PACKAGES`): apps without a launcher
    entry count in the totals but are not listed in *Most used*.
  - Days before the system's retained event history are left out of averages, so *This month*
    may cover fewer days than the calendar. "Decrease" compares with the onboarding answer
    (*How much time do you spend…*), the streak with the current daily goal.
  - QA: `adb shell appops set com.recentr.app GET_USAGE_STATS allow|default`, then bring the app
    back to the foreground.
- Rules are kept in memory (`RulesRepository`) and do not block apps at OS level yet (that needs
  an accessibility / device-policy service).
- No account backend: "Save your progress" and "I already have an account" only move the flow on.
- Squads is a placeholder tab (not part of the delivered flows).
- Profile & Settings: no billing or account backend. *Start 7-day free trial* keeps the Recentr Plus
  plan on the device (`AppSettings.plusPlan`), *Invoices* opens the Play subscriptions page, *Log
  out* clears the email and replays the onboarding (rules stay), *Delete account* erases every
  local store. Settings are persisted (`SettingsRepository`): *Show mascot on Home* hides the Home
  mascot, *App icon* switches the launcher `activity-alias` when the app goes to the background,
  *Language* sets the per-app locale on Android 13+ (no translations ship yet). *Dark* / *Auto*
  themes are saved but the app stays light until a dark palette exists. Notification preferences
  and quiet hours are stored; no reminder is scheduled yet.
- Focus timer: the session runs on the wall clock in memory (`FocusRepository`, like the rules), so
  it survives leaving the screen but not the process. The focus sounds are synthesised
  (`AmbientSoundPlayer`) and play while a round runs with the screen in front. Apps are not blocked
  at OS level: the *App on hold* dialog opens from the "N apps on hold" row (and the ✕ guard).
  The focus log is seeded with two weeks of sessions, including the four of the design.
