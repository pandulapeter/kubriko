# Check whether test-audio's extra `windowInsetsPadding` on the `KubrikoViewport` is needed, and drop it if not.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Planned
**Artifact:** unpublished (examples)
**Rebased:** on 70de96c6 after the Now plans landed.
**Files:** `examples/test-audio/src/commonMain/kotlin/com/pandulapeter/kubriko/testAudio/AudioTest.kt`

## Problem
`AudioTest` (`examples/test-audio/src/commonMain/kotlin/com/pandulapeter/kubriko/testAudio/AudioTest.kt:39-45`) is the only example that pads the viewport itself and also passes the insets on:
```kotlin
KubrikoViewport(
    modifier = modifier
        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        .windowInsetsPadding(windowInsets),
    kubriko = stateHolder.kubriko.collectAsState().value,
    windowInsets = windowInsets,
)
```
`AudioTestManager`'s overlay (`override fun Composable(windowInsets: WindowInsets)`, `AudioTestManager.kt:82`) pads by `windowInsets` again (`.windowInsetsPadding(windowInsets)`, :86). Inset consumption by the outer modifier should make the inner padding zero, so it is probably harmless, but every other example applies the insets only inside the overlay. Whether removal changes anything depends on how `KubrikoViewport` forwards `windowInsets` to `Manager.Composable`, which needs a look on a device with insets.

## Fix
Remove `.windowInsetsPadding(windowInsets)` from the viewport modifier (keeping `background`) **only if** the manual check shows the controls in the same place; otherwise drop this plan.

## Decision
None beyond the visual check.

## Behaviour
Expected: unchanged layout (the overlay's own padding takes over).

## Public API
None.

## Tests
None.

## Verify
`./gradlew :examples:test-audio:compileKotlinDesktop`

## Manual check
Android phone in landscape with a cutout, Showcase fullscreen and windowed: the info panel and music controls sit at the same insets before and after.
