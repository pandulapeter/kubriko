# Collect each example's back navigation intents inside the effect that owns them

**Challenged:** sound

**Kind:** bug  ·  **Severity:** low  ·  **Platforms:** all
**Files:** `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/KubrikoShowcase.kt`

## Problem

`KubrikoShowcase.kt` (~line 88) starts the collector of the active example's `backNavigationIntent` in a scope that
outlives the effect:

```kotlin
            val activeStateHolder = selectedShowcaseEntry.value?.getStateHolder()
            val scope = rememberCoroutineScope()
            LaunchedEffect(activeStateHolder) {
                activeStateHolder?.backNavigationIntent?.onEach {
                    if (getIsInFullscreenMode() == true) {
                        onFullscreenModeToggled()
                    }
                    selectedShowcaseEntry.value = null
                }?.launchIn(scope)
            }
```

`launchIn(scope)` returns immediately, so the `LaunchedEffect` completes and restarting it on a new key cancels
nothing. The four games expose a never-completing `MutableSharedFlow` as `backNavigationIntent`, so every time a game
is opened another collector is added to the `BoxWithConstraints`' scope and stays until the whole Showcase leaves the
composition. Collectors for state holders that have since been disposed keep their flow and the captured
`getIsInFullscreenMode`/`onFullscreenModeToggled` lambdas alive; and if the same state holder instance is selected
again while it is still alive (e.g. switching away and back before `ExampleScreen`'s `DisposableEffect` has disposed
it), it gets a second collector, so one "Leave" emission runs the handler twice. With the current synchronous
fullscreen implementations the second run is a no-op, but it would toggle fullscreen back on for any host whose
`getIsInFullscreenMode` lags the toggle.

## Fix

Collect inside the effect so that its cancellation on key change (or on leaving the composition) ends the collector:

```kotlin
            val activeStateHolder = selectedShowcaseEntry.value?.getStateHolder()
            LaunchedEffect(activeStateHolder) {
                activeStateHolder?.backNavigationIntent?.collect {
                    if (getIsInFullscreenMode() == true) {
                        onFullscreenModeToggled()
                    }
                    selectedShowcaseEntry.value = null
                }
            }
```

Remove the now unused `scope` and the `rememberCoroutineScope`, `launchIn` and `onEach` imports (check each has no
other use in the file first).

## Tests

None: the behaviour lives in a Composable in the app module, which has no Compose UI test setup.

## Manual check

Any platform, e.g. `./gradlew :app:desktop:run`:

1. Open Wallbreaker, open its menu overlay, choose to leave and confirm; the Showcase returns to the Welcome screen.
2. Repeat step 1 three times, then once more after turning full screen on from the menu overlay.
3. Expected: every leave returns to the Welcome screen, and the last one also leaves full screen with the top bar shown
   (not flickering back into full screen).
4. Optional, in a debugger: after step 2, break in the new `collect` block and confirm the Wallbreaker state holder's
   `backNavigationIntent.subscriptionCount.value` is 1.
