# Add the missing trailing commas in `app/` and drop the redundant enum semicolons

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (app)
**Files:**
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ShowcaseEntry.kt`
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/Menu.kt`
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/TopBar.kt`
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/ShowcaseStateHolders.kt` (from A08; `ExampleScreen.kt` if A08 did not land)
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/welcome/WelcomeScreen.kt`
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/licenses/LicenseType.kt` (from A05)
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/licenses/Dependency.kt` (from A05)
- `app/desktop/src/main/kotlin/com/pandulapeter/kubrikoShowcase/KubrikoShowcaseApp.kt`
- `app/web/src/webMain/kotlin/com/pandulapeter/kubrikoShowcase/KubrikoShowcaseApp.kt`
- `app/android/src/main/kotlin/com/pandulapeter/kubrikoShowcase/KubrikoShowcaseActivity.kt`

**Last plan of lane A**: it touches files the earlier plans move.

## Problem
`code-style`: always a trailing comma after the last element of a multi-line comma-separated list. At 2480325f these
lack one (identified by content, since earlier plans move lines):

- `ShowcaseEntry.kt` — the nine `areResourcesLoaded = { <X>StateHolder.areResourcesLoaded() }` last arguments of
  `CONTENT_SHADERS`, `ISOMETRIC_GRAPHICS`, `PARTICLES`, `PERFORMANCE`, `PHYSICS`, `SHADER_ANIMATIONS`, `AUDIO`, `COLLISION`,
  `INPUT` (:102, 108, 114, 120, 126, 132, 140, 146, 152).
- `Menu.kt` — `key = { it.name }` in `items(items = entries, key = { it.name })` (:62).
- `TopBar.kt` — `targetState = selectedShowcaseEntry` of the inner `Crossfade` (:112); the closing `}` of the
  `resource = if (…) { … } else { … }` argument of `stringResource(` in `Header` (:163); `onClick = { onShowcaseEntrySelected(null) }`
  (:203); the closing `}` of the `navigationIcon = { … }` argument of `TopAppBar(` (:211).
- `getOrCreateState`'s parameter `creator: () -> T` (`ExampleScreen.kt:327`; in `ShowcaseStateHolders.kt` after A08).
- `WelcomeScreen.kt` — the second argument of `stringResource(Res.string.welcome_app_details, stringResource(…))` (:202).
- desktop `KubrikoShowcaseApp.kt` — the four `defaultSceneFolderPath = "…"` arguments (:139, 142, 145, 148).
- web `KubrikoShowcaseApp.kt` — the closing `}` of `configure = { isA11YEnabled = false }` in `ComposeViewport(` (:29).
- `KubrikoShowcaseActivity.kt` — the closing `}` of the `object : Animator.AnimatorListener { … }` argument of
  `.setListener(` (:68).

And the two enums end their entry lists with a redundant `;` though no member follows: `MPL_2_0(…);` in `LicenseType`
and `KUBRIKO(…);` in `Dependency` (`LicensesScreen.kt:139, 261`).

## Fix
Add the comma after each element above; replace each of the two `);` with `),`. No other edit. Before committing,
re-scan `app/` for a multi-line list whose last element still lacks a comma (e.g. a line not ending in `,`, `(`, `[` or
`{` followed by a line starting with `)`), skipping conditions inside `if (` / `when (`, which are not lists — in
particular `TitleBar.kt`'s / `ExtendedTitleBar.kt`'s `placement != WindowPlacement.Fullscreen` and
`KubrikoShowcase.kt`'s `) == false` stay as they are.

## Behaviour
Syntax only. Unchanged.

## Public API
None.

## Tests
The existing ones.

## Verify
`./gradlew :app:shared:compileKotlinDesktop :app:shared:desktopTest :app:desktop:compileKotlin :app:web:compileKotlinWasmJs :app:android:compileDebugKotlin`

## Manual check
None.
