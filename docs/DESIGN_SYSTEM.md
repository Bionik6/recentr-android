# Recentr — Design system (Android)

Port of the Figma file **"Offly · Screen time App UI Kit"** (`PKRDsYvhW5Ntdzwq9giTv3`), pages
*Foundations* (`2045:2`), *Components* (`2043:9403`) and *Dark mode* (`2128:6159`). Module:
`:core:designsystem`, package `com.recentr.core.designsystem`.

> The Figma kit is branded "Offly"; the Android app is **Recentr**. Every user-facing "Offly"
> becomes "Recentr" (wordmark, copy, permission texts).

## Usage

Wrap the UI in `RecentrTheme(darkTheme) { … }` (done in `MainActivity` and `HoldActivity` through
`RecentrAppTheme`, which follows *Settings › Appearance › Theme*). Tokens are read through the
`RecentrTheme` object:

```kotlin
Text("1h12", style = RecentrTheme.typography.displayHero, color = RecentrTheme.colors.textOnDark)
Modifier.padding(horizontal = RecentrTheme.spacing.screenGutter) // 24dp
Modifier.recentrShadow(RecentrElevation.card, RecentrShapes.card)
```

Figma pt map 1:1 to dp; Figma px font sizes map 1:1 to sp.

## Foundations

| Figma | Kotlin |
|---|---|
| Colour variables (`bg/page`, `text/secondary`, `alpha/white-24`, …) | `RecentrTheme.colors.bgPage`, `.textSecondary`, `.alphaWhite24`, … (`theme/Color.kt`): the collection's Light mode is `LightRecentrColors`, its Dark mode `DarkRecentrColors` |
| Text styles (Display/Hero … Note/Handwritten) | `RecentrTheme.typography.displayHero`, `.displayNumber`, `.displayLarge`, `.title1`, `.title2`, `.headline`, `.bodyRegular`, `.bodyMedium`, `.captionRegular`, `.captionMedium`, `.labelSmall`, `.brandWordmark`, `.noteHandwritten` — Figtree, letter-spacing in % converted to `em` |
| Spacing `space/2…48` | `RecentrTheme.spacing.space2 … space48`, `screenGutter` (24) |
| Radius `xs 8 · sm 12 · md 16 · lg 20 · xl 24 · card 28 · sheet 36 · screen 48 · full` | `RecentrTheme.radius.*`, shapes in `RecentrShapes.*` |
| Sizes `icon 24 · control 44 · button 54 · tab-bar 64 · avatar 40` | `RecentrTheme.sizes.*` |
| Effects `Elevation/Card`, `Elevation/Float`, `Glow/Sky` | `RecentrElevation.card / float / glowSky` (+ `control` 0·4·8 6%, `raised` 0·12·16 12%, `glowButton`) via `Modifier.recentrShadow(shadow, shape)` |
| Gradients Brand CTA / Brand deep / Success / Strict / Shield | `RecentrGradients.brandCta / brandDeep / success / strict / shield` (+ `template*`). `CssLinearGradient(angle, stops)` reproduces any Figma `linear-gradient(<deg>)` |

## Assets

* **Icons** — `RecentrIcon` enum (36 icons, vector drawables converted from the Figma SVGs) drawn
  with `RecentrIconImage(icon, tint = …, size = 24.dp)`.
* **Stickers** — `Sticker.Lock / Bolt / Timer / Moon / Leaf / Note / Flame / Headphones / Target /
  Heart / Star / Medal / Trophy / Shield / Calendar`, `StickerImage(sticker, size)`.
  Bitmaps rendered at 4× with transparency; the soft contact shadow overflows the slot like in Figma.
* **Mascot** — `Mascot(MascotMood.Calm | Impatient | Dozing, width)` (160×200 artboard).
* **Backdrops** — `BackdropBox(Backdrop.Sky | Backdrop.Aurora) { content }`: full-screen, anchored
  top, page grey below; `rememberBackdropPainter(backdrop)` for the current palette. Aurora is a 3×
  render per palette (`backdrop_aurora_dark`, the same layers in the Dark mode tokens). Sky is drawn
  natively like iOS (`Sky.kt`): its clouds drift with the wind and billow, rest as in Figma when
  motion is reduced, and hold still while the battery runs low (`rememberBatteryLow`: Battery Saver,
  or unplugged at 20% or less).
* **Logo mark** — `LogoMark(width)` (Figma "Logo/Mark", the hourglass on its OFF toggle; with the
  `recentr` wordmark it makes the share card's "Logo/Lockup · On blue").
* **Brand icon** — `BrandIconImage(BrandIcon.Sky | Night | Mint | Sunset, size)` (Figma "Logo/App
  Icon" colourways, transparent 4× bitmaps rendered from the Figma SVG).
* **App icons** — `AppIcon(DemoApp.X, size = 32.dp)`; `DemoApp` ↔ `core.model.AppId` by name.
  Other installed apps: `InstalledAppIcon(packageName, size = 36.dp)` (launcher icon, adaptive
  icons drawn full-bleed in the same rounded square, loaded off the main thread).

## Components

| Figma component | Composable |
|---|---|
| Button (Primary / Brand / Secondary / Soft / Danger / Strict) | `RecentrButton(text, onClick, style, icon)` · `RecentrTextButton` |
| Icon Button (Surface / Sunken / On Sky) | `RecentrIconButton(icon, contentDescription, onClick, style)` |
| Chip (Neutral / Selected / Success / On Sky / Outline) | `RecentrChip(label, style, icon, onClick)` |
| Status Bar / Home Indicator | Android system bars (edge-to-edge) + `SystemBarsAppearance(lightIcons)` |
| Tab Bar | `RecentrTabBar(selected, onSelect)` — `RecentrTab.Home / Rules / Timer / Profile` (the kit has no filled hourglass: the Timer keeps its line icon) |
| Rule Card | `RuleCard(title, sticker, streak, apps, value, onClick) { chips }` |
| Today Card | `TodayCard(stats, onClick) { HourlyUsageChart(minutesPerHour) }` |
| Add Card | `AddCard(title, hint, onClick)` |
| Mini Tile | `MiniTile(label, value, sticker)` |
| Rule Tile | `RuleTile(label, sticker, onClick)` |
| Sticky Note | `StickyNote(title, body)` · editable `StickyNoteField(title, value, onValueChange, placeholder)` |
| Avatar (Sky / Mint / Pink / Tangerine / Night + Profile's Sunset) | `RecentrAvatar(initial, style, size)` |
| Progress / Onboarding Header | `OnboardingProgress(progress)` · `OnboardingHeader(progress, onBack, onSkip)` |
| Option Row | `OptionRow(label, selected, onClick, icon, radio)` (`radio = true`: trailing radio ring) |
| Text Field | `RecentrTextField(value, onValueChange, placeholder)` |
| Keyboard | Android IME (not drawn) |
| Review Card | `ReviewCard(title, body, name, initial)` |
| Alert | `RecentrAlertDialog(title, body, primaryText, onPrimary, onDismissRequest, secondaryText)` |
| Toggle / Checkbox | `RecentrToggle(checked, onCheckedChange)` — the native Material 3 `Switch` in the Figma colours · `RecentrCheckbox(checked, onCheckedChange)` |
| Setting Row (Value / Toggle) | `SettingRow(label, value, onClick, icon, iconTint, iconBackground, labelColor)` · `SettingToggleRow(label, checked, onCheckedChange, icon, iconTint, iconBackground, caption, enabled)` |
| Settings "Group" + section caption | `SettingsGroup { rows }` · `SettingsSectionLabel(text)` |
| Segmented | `RecentrSegmented(options, selectedIndex, onSelect)` |
| App Row | `AppRow(app, checked, onCheckedChange)` |
| Template Card (Sky / Night / Mint / Sunset) | `TemplateCard(title, body, duration, sticker, color, onClick)` |
| Sheets (grabber + 36dp corners + scrim) | `RecentrBottomSheet(onDismissRequest) { … }` · `RecentrSheet(onDismissRequest) { controller -> … }` (animated `controller.close { then }`) · `SheetGrabber()` |
| Surface card | `SurfaceCard(cornerRadius, onClick) { … }` |
| Daily goal sheet (Figma 28, Home and Insights) | `DailyGoalSheet(currentGoalMinutes, onSave, onDismissRequest, formatMinutes)` |

A living catalogue of everything above: `DesignSystemCatalog()` (Profile tab → *Design system*).

## Dark mode

The *Dark mode* page is the Light mode screens with the Color collection switched to Dark: every
component and screen reads the same tokens, so nothing branches on the palette except artwork.

* *Settings › Appearance › Theme*: Light, Dark or Auto (the system setting, the default).
  `RecentrTheme.isDark` tells which palette is on.
* Stickers, mascot, logos and the brand gradients (CTA, templates, shield…) are the same in both.
* `SystemBarsAppearance(lightIcons)` always gives light icons under the dark palette.
* Android 12+: the app's night mode (`UiModeManager.setApplicationNightMode`) follows the choice, so
  the splash screen (`values-night`) is already in the right palette. `MainActivity` and
  `HoldActivity` handle `uiMode` changes without restarting.

## Android adaptations (iOS → Android)

* iOS status bar / home indicator mock-ups are **not drawn**: the app is edge-to-edge and pads
  content with `WindowInsets` (`statusBarsPadding()`, `navigationBarsPadding()`).
* The iOS keyboard mock-up is replaced by the system IME (`imePadding()`), with IME actions.
* iOS sheets → `RecentrBottomSheet` (Material 3 `ModalBottomSheet`: drag, back gesture, scrim).
* iOS alerts → `RecentrAlertDialog`; system permissions use the Android flows
  (`POST_NOTIFICATIONS` runtime permission, Usage Access settings screen for screen time).
* System back (gesture / button) is supported everywhere; the chevron back buttons stay as drawn.
* Touch feedback uses Material ripples; toggles/checkboxes expose `Role.Switch` / `Role.Checkbox`.
* All copy lives in `strings.xml` resources.

## Backlog — next design-system iteration

Local components the feature flows had to add, to fold back into `:core:designsystem`:

* **Display text box** — Android pads text whose line height is smaller than the font's
  (Display styles use 1.0): a 72sp `displayHero` measures ~86dp. Onboarding uses a local
  `Modifier.figmaLineBox(72.dp)`; promote it (e.g. `Modifier.figmaLineHeight(style)`).
* **Buttons** — the rules' danger pill (`RulesPillButton`, now `RecentrButtonStyle.Danger`), a "Soft but
  inactive" state for unanswered steps (`ContinueButton`), and a visual disabled state for
  `RecentrIconButton`; long-press repeat for steppers (`StepperButton`).
* **Charts** — per-bar colours and the Figma 13.2dp pitch for `HourlyUsageChart`
  (`HourlyBarsChart`), plus a weekly chart (`WeekBarsChart`).
* **Rules pieces** — icon-tinted chip, small template card,
  active-border state on `RuleCard`, auto-shrinking hero value text.
* **Sheets** — let `RecentrBottomSheet` take window insets for full-height sheets (`SheetBody`);
  move the rules sheets to `RecentrSheet` (same controller as their local `RulesSheet`), and the
  rules `SettingsGroup` / `SectionCaption` to the design-system ones.
* **Focus rings** — the set-up dial and the session rings (`FocusDial`, `drawFocusRing` in
  `:feature:focus`) could become a design-system progress ring, with the Insights goal ring
  (`GoalRing`).
* **Insights pieces** — the centred header and 24dp cards shared with Focus (`InsightsTopBar` /
  `FocusTopBar`, `InsightsCard` / `FocusCard`), the rounded `ProgressTrack`, a tangerine chip style
  (`ChangeChip`) and the badge disc (`BadgeDisc`).
