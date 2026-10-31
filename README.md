# Recentr — Android

Screen-time & focus app. Native Android port (Kotlin, Jetpack Compose, Material 3) of the iOS
Figma design *Offly · Screen time App UI Kit* — design system, Onboarding, Home & Insight, Rules,
Profile & Settings, Focus timer and Insights & Achievements flows.

## Requirements

- Android Studio (bundled JDK) — or any JDK 17+ as `JAVA_HOME`
- Android SDK with platform 37 (compileSdk 37, minSdk 28, targetSdk 36)

## Build & run

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:installDebug
```

Android backs up the app's data (rules, profile, settings) and restores it on a new phone or a
reinstall. Debug builds aren't backed up (`allowBackup` is off for them in `app/build.gradle.kts`),
so a debug build reinstalled starts from the onboarding rather than with an earlier install's rules.

Debug-only shortcuts (QA):

```bash
# skip onboarding and open a tab (home, rules, timer, profile)
adb shell am start -S -n com.recentr.app/.MainActivity --es start main --es tab rules
# force the onboarding
adb shell am start -S -n com.recentr.app/.MainActivity --es start onboarding
```

A routine's reminder opens the rule's page, over the Rules tab: its intent carries
`recentr://rules/<id>` as data (`NotificationLink`), read by `MainActivity` as it starts or, when
it's already up, in `onNewIntent`. To try it without waiting for a reminder:

```bash
adb shell am start -n com.recentr.app/.MainActivity -a android.intent.action.VIEW -d recentr://rules/<id>
```

In debug builds, **Profile › Settings › Developer** has *Replay onboarding* and *Design system*
(living catalogue of every token and component). The focus timer is the **Timer** tab. The **Insights** tab opens on Day, today, as on iOS; the
usage card on **Home** opens it on the Home period (Day / Week / Month); Pickups, Daily goal, Streak and Achievements are in its **⋯** menu, the streak
also from the Home streak tile.

## Modules

| Module | Content |
|---|---|
| `:app` | `MainActivity` (splash, edge-to-edge, launcher icon aliases), root `NavHost`, tab shell with the floating tab bar (Home, Rules, Timer, Profile) |
| `:core:designsystem` | Figma foundations (colour, Figtree typography, spacing, radius, effects, gradients), icons, stickers, mascot, backdrops and all components — see [docs/DESIGN_SYSTEM.md](docs/DESIGN_SYSTEM.md) |
| `:core:model` | Domain models (`UserProfile`, `Rule`, `ScreenTimeSummary`, `AppUsage` / `UsageApp`, `AppId`, `DayStats`, `AppUsageDetail`, `PickupStats`, `Achievement`, `CalmLevel`) |
| `:core:data` | Repositories: `UserPreferencesRepository` and `SettingsRepository` (DataStore), `RulesRepository`, `FocusRepository`, `InstalledAppsRepository` (launcher apps for the pickers), `ScreenTimeRepository` (device usage events, empty without usage access), `AchievementsRepository` (badges, DataStore) and the `RecentrData` locator; `Streaks` (streak savers) and `AchievementEngine` are pure and unit-tested |
| `:feature:onboarding` | 17-screen onboarding (Figma `2023:578`) |
| `:feature:home` | Home & Insight (Figma `2025:1422`) |
| `:feature:rules` | Rules list, editors, sheets and templates (Figma `2027:2433`) |
| `:feature:profile` | Profile tab, edit sheet, weekly recap, Recentr Plus & subscription, Settings with Help, Appearance, Notifications, Language, log out and delete account (Figma `2033:6330`) |
| `:feature:focus` | Focus timer (the Timer tab): set-up dial, sounds / rounds / apps sheets, running session, break, app on hold, session complete and focus log (Figma `2114:15883`) |
| `:feature:insights` | Insights & Achievements (Figma `2119:16391`): overview (Day / Week / Month calendar), app detail, pickups, monthly report, daily goal, badges, new badge, badge sheet, streak, milestones and share card |

## Status / known limits

- Screen time: with Usage Access granted, `UsageStatsScreenTimeRepository` aggregates the device's
  `UsageStatsManager.queryEvents` (foreground time per hour / day, per-app totals, pickups from
  unlocks or screen-ons, `NOTIFICATION_INTERRUPTION` events, longest screen-off stretch); without
  it, `EmptyScreenTimeRepository` reports empty periods and the screens show their empty states
  (`DemoScreenTimeRepository` keeps the Figma values for previews). The access is re-checked on every resume
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
- Streaks: consecutive days under the daily goal; a day over the goal uses one of the month's 3
  streak savers instead of breaking it (`Streaks`, shared by Home and the Streak screen). A streak
  never starts on a saved day.
- Insights & Achievements read the same usage events: per-day totals, per-app hourly use, opens
  (sessions less than 15 s apart are one), longest session and first open, pickup times and the
  first app opened after each pickup. "Won back" is the time under the habit reported in the
  onboarding. The 16 badges are evaluated by `AchievementEngine` against two months of history,
  the focus log and the rules; with device numbers their earned days are kept (DataStore), demo
  numbers are evaluated on the fly. A badge already met before the user started counts from the
  first day; each new badge is celebrated once (the newest one when several arrive together).
  *Top apps* lists the five most used apps, five more on each *See more* until every app is
  listed, and five again for another day or week, as on iOS. *Top apps* and Home's *Most used*
  only list apps with a minute or more on screen (`ScreenTimeSummary.mostUsed`), as on iOS.
  *Set a limit* and *Block* work for any app with a launcher entry.
- Share cards are rendered to a PNG (`FileProvider`, cache): Stories / Post / Facebook open the app
  when it is installed, the share sheet otherwise; *Save* writes to Pictures/Recentr (Android 10+,
  the share sheet before).
- Completing the onboarding sets up three routines blocking the apps the user picks on its apps
  step ("What pulls you in?", right after Screen Time, as on iOS; nothing is picked for them):
  Working Time (weekdays, 9–5), Night Pause (every day, 11 PM–7 AM) and Weekend Zen (weekends,
  9–12), none strict, each with a 10-second wait to unlock (`StarterRoutine` in `:core:model`, [docs/RULES.md](docs/RULES.md) ›
  Starter routines). Someone who already has rules gets none. Home's next rule tile shows its rule
  on Rules: the list opens its group and scrolls to the card.
- Home's rule tile shows the rule blocking now and the time it has left ("Ongoing · Focus",
  "27m left"; a routine in its window reads "Blocking"), the one ending first, else the next
  routine to start, as on iOS. Home has no "Start blocking" button: the Rules tab's "+ New rule"
  opens the New rule sheet (a kind, or Browse templates), as its header's "+" does.
- App pickers (Rules › Choose apps, Focus › Block) list the device's launcher apps
  (`InstalledAppsRepository`, `PackageManager`), grouped by their declared `android:appCategory`
  (catalogue packages keep their Figma category); rules and focus sessions keep the chosen apps'
  package names and labels.
- Rules come in four kinds, each with one job (the spec, shared with iOS, is
  [docs/RULES.md](docs/RULES.md)): a **session** blocks apps for a set time from *Start*; a
  **routine** blocks them during windows that repeat (saved as `RuleType.Bedtime`); a **lock**
  keeps them blocked from *Activate*, with a few unlocks a day, and *Turn off* takes effect at
  midnight; a **limit** is a time budget for today from *Start*, then blocks its apps until
  midnight, and has no unlocks but emergency ones. Creating a rule (held down, not tapped) turns
  it on: a session starts, a lock is on, a limit starts for today. While a rule is
  engaged, an edit that loosens it (fewer apps, more unlocks, a bigger budget, Strict mode off,
  pause, turn off, delete) is kept on the rule and applied when the engagement ends; what
  reinforces it applies now (`Rule.edit` in `:core:model`, unit-tested).
- Rules are saved to `files/rules.json` (`RulesRepository`, written after every change, with a
  format version: a file of another one isn't read, as Recentr hasn't shipped); the unlocks used
  start over each day. The focus session and its log are still in memory.
- App blocking: `HoldService` (`:app`) is an accessibility service told only which activity comes
  to the front (no window content). When that app is on hold — `HoldState` in `:core:data`, pure
  and unit-tested: locks that are on, running sessions, routines (their chosen apps, inside the
  window), limits whose budget is spent (device usage since *Start*, so usage access is needed),
  focus rounds — it opens `HoldActivity` ("App on hold"). "Unlock anyway" follows the rule's
  unlock method (wait, re-type, walk 20 steps or scan the pass) and counts against its unlocks per
  day; a strict rule or a limit only offers an emergency unlock (three a month). Recentr, the home
  screens, System UI and Settings are never held (and not offered by the pickers).
  - The user turns it on in Settings › Accessibility, after the in-app explanation
    (`BlockingAccessSheet`: Rules banner, after saving a rule, Focus set-up, Settings › App
    blocking). Force-stopping Recentr turns the service off (Android behaviour).
  - QA: Settings › Accessibility › Recentr app blocking, then open a blocked app.
  - Walking to unlock counts steps from "Start walking" with the phone's step counter
    (`Sensor.TYPE_STEP_COUNTER`, the Physical activity permission from Android 10). The counter
    reports steps seconds late (it waits for several in a row, then sends them together), so while
    the hold screen is open `StepDetector` also finds each step's bounce in the accelerometer, once
    three come in a row at a walking pace; the counter catches up on steps taken with the screen
    off. `Walk` shows whichever counted more. Both are in `:core:data`, pure and unit-tested. A
    phone without a step counter can't walk to unlock; debug builds without one (the emulator)
    pretend to walk two steps a second.
- Always allowed (Settings › Protection, and Home's Always allowed card from the end of onboarding,
  before any rule, until its × closes it, after a sheet saying the list stays in Settings): apps no
  rule and no focus session ever holds ([docs/RULES.md](docs/RULES.md) › "Always
  allowed"). Saved to `files/always_allowed.json` (`AlwaysAllowedRepository`). Removing one
  applies now; an app an engaged rule (or a strict focus session) holds waits to join until that
  engagement ends (`AlwaysAllowed.choosing` in `:core:model`, unit-tested), and the page says
  when. A limit's budget still counts their time.
- Strict mode is a toggle on sessions and routines: while the rule is engaged, its apps can't be
  unlocked, other than with an emergency unlock (turning it on while the rule already blocks asks
  first; turning it off waits for the end). Settings › Strict mode makes new sessions and
  routines start strict. Every toggle is the native Material 3 switch.
- No account backend: the onboarding has no account screens ("Save your progress", "I already
  have an account"), as on iOS.
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
