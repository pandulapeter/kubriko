# Reset the web full screen state when the browser refuses to enter full screen

**Challenged:** sound

**Kind:** bug  ·  **Severity:** low  ·  **Platforms:** Web
**Files:** `app/web/src/webMain/kotlin/com/pandulapeter/kubrikoShowcase/KubrikoShowcaseApp.kt`, `app/web/CLAUDE.md`

## Problem

`onFullscreenModeToggled` (`KubrikoShowcaseApp.kt`, ~line 54) flips the app state first and then asks the browser:

```kotlin
            onFullscreenModeToggled = {
                isInFullscreenMode.value?.let { currentValue ->
                    isInFullscreenMode.value = !currentValue
                    if (currentValue) {
                        if (document.fullscreenElement != null) {
                            document.exitFullscreen()
                        }
                    } else {
                        document.documentElement?.requestFullscreen()
                    }
                }
            },
```

`requestFullscreen()` returns a promise that rejects when the browser refuses: the page is embedded in an `<iframe>`
without `allow="fullscreen"`, the user has disabled the Fullscreen API (Firefox `full-screen-api.enabled`), or the
call is not treated as a user activation. On a rejection no `fullscreenchange` event fires, so the listener that syncs
the state back (`if (isInFullscreenMode.value == true) isInFullscreenMode.value = document.fullscreenElement != null`)
never runs. The Showcase then hides its top bar and side menu as if it were in full screen while the page is not,
until the user finds the in-game full screen button again. On a browser that lacks the unprefixed method altogether
(older iPad Safari; iPhone is already excluded by `isRunningOnIphone()` returning `null`), the call throws instead,
after the state was already flipped.

## Fix

Keep the optimistic flip (the chrome should hide immediately on success), and undo it when the request fails:

```kotlin
                    } else {
                        try {
                            document.documentElement?.requestFullscreen()?.catch {
                                isInFullscreenMode.value = false
                                null
                            }
                        } catch (_: Throwable) {
                            isInFullscreenMode.value = false
                        }
                    }
```

Adapt to the exact `Promise` API of the Kotlin/Wasm `kotlinx-browser` version in use (`catch` takes a
`(JsAny) -> JsAny?` or similar; it may need `@OptIn(ExperimentalWasmJsInterop::class)`, which `main` already has).
Leave the exit branch as it is: `exitFullscreen()` failing leaves the page in full screen, where the existing
`fullscreenchange` listener still tracks it.

Add one sentence to the "Fullscreen handling" section of `app/web/CLAUDE.md`: if the browser refuses the request, the
state is set back to `false`.

## Tests

None: it depends on the browser's Fullscreen API; the web app has no test source set.

## Manual check

Desktop Firefox (current release):

1. In `about:config` set `full-screen-api.enabled` to `false`.
2. `./gradlew :app:web:wasmJsBrowserDevelopmentRun`, open Wallbreaker, open its menu overlay and press the full screen
   button.
3. Before: the top bar and side menu slide away and stay hidden while the page is not full screen. After: they slide
   back (or never leave), and the console shows no uncaught error.
4. Set `full-screen-api.enabled` back to `true`, repeat step 2: the page enters full screen, Esc leaves it and the
   chrome returns (the existing listener).
