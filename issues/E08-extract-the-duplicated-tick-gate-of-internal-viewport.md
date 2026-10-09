# Extract the frame loop's duplicated "may it tick now" condition into one private function

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** engine
**Files:**
- `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/implementation/InternalViewport.kt`

Runs after E07 (same file).

## Problem

The `LaunchedEffect` frame loop spells out the same gate twice, once inside the hoisted `onFrame` lambda and once at
the top of the `while` loop (`InternalViewport.kt:129-132` and `:188-191` at 2480325f):

```kotlin
val canTick = viewportTickSource != null &&
        viewportTickSource.isRunningInternal.value &&
        !kubrikoImpl.viewportManager.size.value.isEmpty() &&
        (!viewportTickSource.shouldPauseOnFocusLoss || kubrikoImpl.stateManager.isFocused.value)
...
val canTickNow = viewportTickSource != null &&
        viewportTickSource.isRunningInternal.value &&
        !kubrikoImpl.viewportManager.size.value.isEmpty() &&
        (!viewportTickSource.shouldPauseOnFocusLoss || kubrikoImpl.stateManager.isFocused.value)
```

A third, flow-based form of the same condition feeds the suspended gate (`combine(...) { size, isFocused,
isTickSourceRunning -> isTickSourceRunning && !size.isEmpty() && (!viewportTickSource.shouldPauseOnFocusLoss ||
isFocused) }`, :198-204); it reads the combined values rather than `.value`, so it stays as it is.

## Fix

Add, below the `InternalViewport` Composable and above the two constants at the end of the file:

```kotlin
private fun isTickingAllowed(kubrikoImpl: KubrikoImpl, viewportTickSource: ViewportFrameTickSource) =
    viewportTickSource.isRunningInternal.value &&
            !kubrikoImpl.viewportManager.size.value.isEmpty() &&
            (!viewportTickSource.shouldPauseOnFocusLoss || kubrikoImpl.stateManager.isFocused.value)
```

and replace both conditions with
`viewportTickSource != null && isTickingAllowed(kubrikoImpl, viewportTickSource)`, keeping the `canTick` /
`canTickNow` local names. The `viewportTickSource != null &&` prefix stays at the call sites, because the code after
`if (canTick)` (`viewportTickSource.tick(...)`) and after `if (!canTickNow)` relies on that smart cast.

## Behaviour
Unchanged: the same reads in the same short-circuit order. Calling a private top-level function from the hoisted
`onFrame` lambda allocates nothing (it captures nothing new), so the frame loop stays allocation-free.

## Public API
None. The function is private; `InternalViewport`'s facade `InternalViewportKt` keeps its public members.

## Tests
The existing ones. (The frame loop itself becomes testable in E52.)

## Verify
`./gradlew :engine:compileKotlinDesktop :engine:compileKotlinWasmJs :engine:desktopTest`

## Manual check
none
