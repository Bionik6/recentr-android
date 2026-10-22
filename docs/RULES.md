# Rules

Recentr has four kinds of rule. Each one has a single job, starts one way and ends one way. The
iOS and Android apps both follow this page: change it in both repositories together.

## The four kinds

| Kind | What it does | Starts | Ends |
|---|---|---|---|
| Session | Blocks apps for a set time. | When created, right away; Start again from its page. | When its countdown ends. |
| Routine | Blocks apps during windows that repeat. | On its own, on the chosen days and hours. | At the end of each window. |
| Lock | Keeps apps blocked, with a few short unlocks a day. | When created; Activate again from its page once it's off. Then it stays on. | Turn off takes effect at midnight. Its unlocks start over at midnight. |
| Limit | A time budget for today, then blocks the apps until midnight. | When created, for today only; Start again from its page another day. | At midnight. It doesn't start again on its own. |

- Creating a rule turns it on: a session starts, a lock is on, a limit starts for today, and a
  routine follows its schedule. The create button is held down, not just tapped. Creating a lock
  or a limit says first that easing or deleting it only applies after the daily reset, at
  midnight.
- Starting a lock or a limit again from its page (Activate, Start) asks first: once it's on, what
  makes it easier only applies at midnight.
- Anything that follows a calendar is a routine ("Work hours · Weekdays · 9–5"). Sessions, locks
  and limits have no days or hours.

### What each kind asks for

| | Session | Routine | Lock | Limit |
|---|---|---|---|---|
| Apps | ✓ | ✓ | ✓ | ✓ |
| Length | ✓ | | | |
| Days and hours | | ✓ | | |
| Budget for today | | | | ✓ |
| Unlock method | ✓ | ✓ | ✓ | |
| Unlocks per day | | | ✓ | |
| Strict mode | ✓ | ✓ | | |
| Reminder before it starts | | ✓ | | |

### Limits

- The sheet asks for the budget ("Budget for today") and the apps, and says "Resets automatically
  at midnight." There is no "When time is up" and no "Resets": once the budget is spent, the apps
  are blocked straight away, until midnight.
- The budget counts from Start.
- A limit has no unlocks. Only an emergency unlock gets past it.

### Strict mode

- Only sessions and routines, the rules that end on their own, can be strict. Settings › Strict
  mode turns it on for new sessions and routines only.
- While the rule is engaged (see below), its apps can't be unlocked, other than with an emergency
  unlock (three a month, across every rule).
- "How do you unlock?" has no "No unlocks": Strict mode is the way to have none.

## Changing a rule while it's engaged

A rule is engaged:

- a session, while it runs;
- a routine, during one of its windows;
- a lock, from Activate until it's off;
- a limit, from Start until midnight, budget spent or not.

The protection holds from the moment the rule is engaged, not once its unlocks or its budget are
used up: otherwise five unlocks a day could become ten after the fourth, and so on.

"Edit rule" is always there. What changes is when an edit applies:

| Change | While the rule is engaged |
|---|---|
| **Presentation**: name, sticker, note, reminder | Now. |
| **Reinforcement**: add apps, fewer unlocks a day, shorter unlocks, a harder unlock method or a longer wait, a smaller budget, a longer session, a wider routine window, Strict mode on, resume, activate, start | Now, after asking first when it blocks something right away. |
| **Loosening**: remove apps, more unlocks a day, longer unlocks, a lighter unlock method or a shorter wait, a bigger budget, a narrower routine window, Strict mode off, pause, turn off, delete | Kept, and applied when the engagement ends. |

When a kept change applies:

| Kind | Applies |
|---|---|
| Session | When the session ends. |
| Routine | When the current window ends, for the windows after it. |
| Lock | At midnight. |
| Limit | At midnight, for the next Start. |

- When the rule isn't engaged, every change applies now.
- An edit that both reinforces and loosens applies its reinforcing parts now and the rest later.
  Apps count as one setting: a new choice that keeps every app already blocked applies now,
  otherwise the whole choice waits.
- Unlock methods by effort, lightest first: wait, re-type, walk, scan the pass.
- A running session never gets shorter. A longer length makes it last longer now; a shorter one is
  for the next start.
- The rule's page says what's waiting and when ("Your changes apply at midnight"), and the kept
  change can be dropped. A kept delete reads "Deleted at midnight", with "Keep rule".
- Sessions and limits can't be stopped: they end on their own. A lock turns off at midnight. Only
  routines pause ("For 1 hour", "Until tomorrow", "Until I turn it back on"); during a window the
  pause starts when the window ends.

## Always allowed

The apps the person always allows (on iOS, websites too) are never blocked: no rule and no Timer
run holds them, whatever its kind, its apps or its categories, "Block every app" included. Maps, a
ride, a health app.

- Settings › Protection › Always allowed lists them, adds some and removes one. From the second
  rule made (paused or not), Home shows an Always allowed card: the apps allowed, or "No apps
  selected", with Set up. Its × first says the list stays in Settings, then closes the card for
  good.
- Only blocking leaves them out: a limit's budget still counts the time spent in its apps.
- Removing an app applies now, even while others wait to join: wherever a rule or a run picked it,
  it's blocked again.
- Allowing an app makes the rules that block it easier. While one of them is engaged, or a strict
  Timer run holds it, the app waits, and joins the list when the last of those engagements ends:
  when the session ends, at the end of the routine's window, at midnight for a lock or a limit,
  when the strict run is over. The apps allowed with it that nothing engaged blocks join now. The
  apps that wait join together, at the latest of those ends.
- The page says when they join ("Your changes apply at midnight"), and the kept change can be
  dropped.

## Rules made before this

- Locks with hours, and sessions with a weekly schedule (Android), become routines.
- Locks without hours stay on.
- Limits wait for Start: they no longer start over every day.
- Strict mode is off on locks and limits.
- "No unlocks" becomes the default: wait, five unlocks a day.
- Habits (Android) are gone.
