# Recentr — Android

Screen-time & focus app. Native Android port (Kotlin, Jetpack Compose, Material 3) of the iOS
Figma design *Offly · Screen time App UI Kit* — design system, Onboarding, Home & Insight and
Rules flows.

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

In the app, **Profile** (placeholder tab) has *Replay onboarding* and *Design system* (living
catalogue of every token and component).

## Modules

| Module | Content |
|---|---|
| `:app` | `MainActivity` (splash, edge-to-edge), root `NavHost`, tab shell with the floating tab bar, Squads/Profile placeholders |
| `:core:designsystem` | Figma foundations (colour, Figtree typography, spacing, radius, effects, gradients), icons, stickers, mascot, backdrops and all components — see [docs/DESIGN_SYSTEM.md](docs/DESIGN_SYSTEM.md) |
| `:core:model` | Domain models (`UserProfile`, `Rule`, `ScreenTimeSummary`, `AppUsage` / `UsageApp`, `AppId`) |
| `:core:data` | Repositories: `UserPreferencesRepository` (DataStore), `RulesRepository`, `ScreenTimeRepository` (device usage events or demo data) and the `RecentrData` locator |
| `:feature:onboarding` | 17-screen onboarding (Figma `2023:578`) |
| `:feature:home` | Home & Insight (Figma `2025:1422`) |
| `:feature:rules` | Rules list, editors, sheets and templates (Figma `2027:2433`) |

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
- Squads and Profile tabs are placeholders (not part of the delivered flows).
