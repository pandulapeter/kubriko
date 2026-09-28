# Render the game viewport from the noop debug menu's wrapping composables

**Challenged:** sound

**Kind:** bug  ·  **Severity:** high  ·  **Platforms:** all
**Artifact:** `tool-debug-menu-noop` (behaviour); docs only in `tool-debug-menu-api`
**Files:** `tools/debug-menu-noop/src/commonMain/kotlin/com/pandulapeter/kubriko/debugMenu/DebugMenu.kt`, `tools/debug-menu-noop/CLAUDE.md`, `tools/debug-menu-api/CLAUDE.md`

**Decision needed:** Should the noop `DebugMenu(...)` and `DebugMenu.OverlayOnly(...)` render the `kubrikoViewport` they are given (as the noop README promises), or stay blank with the README corrected? — recommended: render it (option A).

## Problem

`tool-debug-menu-noop` is the artifact the README tells consumers to swap in for release builds so that "the `invoke` composable will simply render its content without any overlay". At 0008d027 it renders nothing at all:

```kotlin
@Composable
override operator fun invoke(
    modifier: Modifier,
    windowInsets: WindowInsets,
    kubriko: Kubriko?,
    isEnabled: Boolean,
    buttonAlignment: Alignment?,
    debugMenuTheme: @Composable (@Composable () -> Unit) -> Unit,
    kubrikoViewport: @Composable () -> Unit,
    verticalDebugMenuWidth: Dp,
    horizontalDebugMenuHeight: Dp,
) = Unit
...
@Composable
override fun OverlayOnly(
    modifier: Modifier,
    kubriko: Kubriko?,
    kubrikoViewport: @Composable () -> Unit,
    buttonAlignment: Alignment?,
) = Unit
```

(`DebugMenu.kt` ~33-44 and ~66-72.) The game's `KubrikoViewport` is passed in as `kubrikoViewport` and dropped, so a consumer who follows the documented `DebugMenu(kubriko, isEnabled) { KubrikoViewport(...) }` pattern and swaps to the noop for release ships a **blank screen**. The real implementation renders `kubrikoViewport()` whenever the menu is disabled, so the two artifacts disagree on the one thing that must be identical.

It already bites inside this repo: `tools/scene-editor/build.gradle.kts` compiles the scene editor against `debugMenuNoop` when `showcase.isDebugMenuEnabled` is not `true`, and `EditorUserInterface.kt` (~81) wraps the editor's `KubrikoViewport` and `EditorOverlay` in `DebugMenu(kubriko = ..., isEnabled = ...) { ... }` — with the flag off the editor canvas is empty. The Showcase avoids it only by special-casing the flag (`app/shared/.../ShowcaseContent.kt` ~261-268: `if (BuildConfig.IS_DEBUG_MENU_ENABLED) DebugMenu.OverlayOnly(...) else Content()`); that workaround is owned by the app lane and can stay as it is (it becomes redundant, and the app lane may simplify it later).

The detailed overloads in `DebugMenuContract` (`tools/debug-menu-api/.../DebugMenuContract.kt` ~81 and ~167) also carry `= Unit` default bodies. Both published implementations override them, so they do not need to change; do not touch the API file.

## Fix

Option A (recommended) — in the noop `DebugMenu.kt`:

```kotlin
@Composable
override operator fun invoke(
    ...,
) = Box(
    modifier = modifier,
) {
    kubrikoViewport()
}

@Composable
override fun OverlayOnly(
    ...,
) = Box(
    modifier = modifier,
) {
    kubrikoViewport()
}
```

This mirrors the real implementation's disabled branch (`BoxWithConstraints(modifier) { ... kubrikoViewport() }`). `Box` comes from `androidx.compose.foundation.layout`, which is already on the noop's classpath (it imports `WindowInsets` from the same package through the engine's `api` dependency) — no build file change. `Horizontal` and `Vertical` stay `= Unit`: they only draw the panel, never the game. Keep the parameters (they are overrides); unused-parameter warnings are expected in a noop.

Update the KDoc on the noop `object DebugMenu` from "All operations are non-functional." to say the panels and the toggle do nothing while `invoke` and `OverlayOnly` render `kubrikoViewport` unchanged.

Option B — keep the noop blank and change the noop README to say consumers must render their viewport themselves in release builds. Not recommended: it makes the documented wrapping pattern unusable with the noop, which is the noop's whole purpose.

Docs:
- `tools/debug-menu-noop/CLAUDE.md` → Implementation: replace "All four detailed composable overloads (`invoke`, `Horizontal`, `Vertical`, `OverlayOnly`) return `Unit` and render only `content()`" with "`invoke` and `OverlayOnly` render `kubrikoViewport()` inside a `Box(modifier)`; `Horizontal` and `Vertical` render nothing".
- `tools/debug-menu-api/CLAUDE.md`: the sentence "All four composable overloads have default `= Unit` implementations in the interface — the noop module inherits them without override" is wrong; replace it with "The detailed overloads have `= Unit` default bodies in the interface; both the real and the noop implementation override all four."

## Tests

None: the change is two Composable bodies, and the sweep's test setup (plan 00) adds `kotlin-test` only, no Compose UI test harness. There is no pure logic to extract.

## Manual check

1. Set `showcase.isDebugMenuEnabled=false` in `gradle.properties` (keep `showcase.isSceneEditorEnabled=true`), run `./gradlew :app:desktop:run`, open Annoyed Penguins and open its Scene Editor: the scene canvas and the selection overlay are visible (before the fix the canvas area is empty).
2. Restore `showcase.isDebugMenuEnabled=true` and repeat: the editor and its debug menu behave as before.
