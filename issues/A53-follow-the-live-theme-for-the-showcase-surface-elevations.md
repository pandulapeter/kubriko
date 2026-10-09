# Follow the theme `KubrikoTheme` is actually in for the Showcase's surface elevations, and define them once

**Kind:** bug  ·  **Severity:** low  ·  **Platforms:** desktop (the only target whose `KubrikoTheme` polls the system theme)  ·  **Class:** Planned
**Artifact:** unpublished (app); possibly tool-ui-components (see Decision)
**Files:**
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/ShowcaseContent.kt`
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/TopBar.kt`
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/ShowcaseSurfaceElevation.kt` (new)
- `app/desktop/src/main/kotlin/com/pandulapeter/kubrikoShowcase/TitleBarAppearance.kt` (from A02; only under option (a))
- cross-lane (`tools/ui-components`, only under option (a) or for the buttons' follow-up):
  `tools/ui-components/src/commonMain/kotlin/com/pandulapeter/kubriko/uiComponents/theme/KubrikoTheme.kt`,
  `tools/ui-components/src/commonMain/kotlin/com/pandulapeter/kubriko/uiComponents/theme/DynamicDarkTheme.kt`,
  `tools/ui-components/src/{android,desktop,ios,web}Main/kotlin/com/pandulapeter/kubriko/uiComponents/theme/DynamicDarkTheme.<platform>.kt`,
  `tools/ui-components/src/commonMain/kotlin/com/pandulapeter/kubriko/uiComponents/LargeButton.kt`,
  `tools/ui-components/src/commonMain/kotlin/com/pandulapeter/kubriko/uiComponents/SmallButton.kt`,
  `tools/ui-components/src/commonMain/kotlin/com/pandulapeter/kubriko/uiComponents/FloatingButton.kt`,
  `tools/ui-components/CLAUDE.md` (if present)

## Problem
`KubrikoTheme` picks its color scheme with `dynamicIsSystemInDarkTheme()` (`KubrikoTheme.kt:73`), which on desktop polls
skiko's `currentSystemTheme` every 100 ms (`DynamicDarkTheme.desktop.kt:21`) because Compose Desktop's
`isSystemInDarkTheme()` is not known to follow a live system switch. The Showcase's surfaces decide their elevation with
the plain `isSystemInDarkTheme()` (at 2480325f):

```kotlin
// ShowcaseContent.kt:89
tonalElevation = if (isSystemInDarkTheme()) 2.dp else 0.dp,
// ShowcaseContent.kt:281-288 (side menu) and TopBar.kt:80-87 — the same `when` twice
tonalElevation = when (isSystemInDarkTheme()) { true -> 4.dp; false -> 0.dp },
shadowElevation = when (isSystemInDarkTheme()) { true -> 4.dp; false -> 2.dp },
```

If the plain function does not follow a live switch, switching the OS theme while the Showcase runs recolors everything
but leaves the elevations (tonal tint, shadows) of the old theme. **Unconfirmed** — the first step is the manual check.
`TitleBarAppearance` (desktop) re-implements the same polling loop (`TitleBar.kt:108-113`), and `LargeButton`,
`SmallButton`, `FloatingButton` in `tools/ui-components` use the plain function too.

## Fix
After confirming the staleness:
1. App: one `internal` helper in `ShowcaseSurfaceElevation.kt` (e.g. `@Composable fun rememberShowcaseIsDarkTheme()`
   or two `Dp` getters for "raised" surfaces) used by the three surfaces, so the `when` exists once.
2. How the helper knows the theme — see Decision.

## Decision
(a) Expose the flag from `tools/ui-components` (e.g. a public `LocalKubrikoIsDarkTheme` provided by `KubrikoTheme`,
with KDoc) — an **addition** to a published artifact's API; also lets `TitleBarAppearance` drop its own loop only if it
runs inside the theme, which it does not today (it sits outside `KubrikoShowcase`). (b) App-only: derive darkness from
the theme in effect, `MaterialTheme.colorScheme.surface.luminance() < 0.5f`, no API change; the ui-components buttons
are fixed separately inside their module by calling the internal `dynamicIsSystemInDarkTheme()`. (c) Leave as is if the
manual check shows `isSystemInDarkTheme()` is live on desktop with the current Compose version (then drop the polling
in `DynamicDarkTheme.desktop.kt` instead, a separate ui-components plan).
**Recommended: (b)** — fixes the app without touching a published API; the buttons' fix is a one-line internal change
in the ui-components lane.

## Behaviour
Under (b): the same elevations as today in a steady theme; after a live OS switch, the elevations follow the new theme
together with the colors.

## Public API
None under (b) or (c); an addition under (a).

## Tests
None (theme reads need a composition).

## Verify
`./gradlew :app:shared:compileKotlinDesktop :app:desktop:compileKotlin` (+ `:tools:ui-components:compileKotlinDesktop`
under (a)).

## Manual check
First, before any code: `./gradlew :app:desktop:run` on macOS, switch System Settings → Appearance between Light and
Dark while the Showcase is open, and compare the top bar / side menu shadow and tint with a fresh start in that theme.
If they already match, take (c).
