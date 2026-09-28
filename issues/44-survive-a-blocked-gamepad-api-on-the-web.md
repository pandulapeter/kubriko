# Stop polling gamepads on the web when the page is not allowed to use the Gamepad API, instead of throwing every tick

**Challenged:** sound

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** web
**Artifact:** `plugin-gamepad-input` (internal change only)
**Files:** `plugins/gamepad-input/src/webMain/kotlin/com/pandulapeter/kubriko/gamepadInput/implementation/GamepadEventHandler.web.kt`, `plugins/gamepad-input/CLAUDE.md`

## Problem

`poll()` calls, on every tick:

```kotlin
private fun refreshGamepads(): Int =
    js("(() => { const pads = navigator.getGamepads ? navigator.getGamepads() : []; globalThis.__kubrikoGamepads = pads; return pads.length; })()")
```

The Gamepad spec has `getGamepads()` throw a `SecurityError` when the document is not allowed to use the `gamepad`
permissions-policy feature. Its default allowlist is `self`, so this is the normal state of a game embedded in a
**cross-origin iframe** without `allow="gamepad"` — which is how itch.io, many portals and most blog embeds serve a web
build — or served with `Permissions-Policy: gamepad=()`. Firefox enforces it; Chromium's enforcement has been rolling
out.

The JavaScript exception crosses into Wasm as a Kotlin `Throwable` and nothing catches it:
`poll()` ← `GamepadInputManagerImpl.onUpdate` ← `KubrikoImpl.onTick` (a plain loop, no `try`) ← the frame callback of
`KubrikoViewport`'s tick `LaunchedEffect`. The effect fails on the first tick, which fails the composition's
recomposer: the whole game freezes (or, with a coroutine tick source, the exception is thrown into the Kubriko scope
on every tick). A game that registers `GamepadInputManager` "just in case" becomes unplayable in the most common web
distribution setup.

## Fix

Catch on the JavaScript side (no Kotlin `try` in the per-tick path, no exception object per tick) and stop asking once
the API is known to be blocked — a permissions policy can't change for the life of the page:

```kotlin
private fun refreshGamepads(): Int =
    js("(() => { try { const pads = navigator.getGamepads ? navigator.getGamepads() : []; globalThis.__kubrikoGamepads = pads; return pads.length; } catch (e) { globalThis.__kubrikoGamepads = []; return -1; } })()")
```

In the handler, add `private var isGamepadApiBlocked = false`; in `poll()`, return early when it is set, and set it
when `refreshGamepads()` returns `-1` (then fall through with a count of 0 so any slot marked connected is reset).
Update the comment above `refreshGamepads` to mention the `-1`.

## Tests

None: the snippet runs only in a browser. It can be sanity-checked without a browser by pasting the arrow function
into `node -e` after
`Object.defineProperty(globalThis, "navigator", { value: { getGamepads() { throw new Error("SecurityError") } }, configurable: true })`
(a plain assignment is ignored, Node's `navigator` is a read-only accessor): it must print `-1` (verified during the
review).

## Manual check

Build the web Showcase and serve it from one origin, then embed it from another origin in a page with
`<iframe src="http://localhost:8081/" allow="">` (or serve it with the response header `Permissions-Policy:
gamepad=()`), in Firefox: the games run, and the console shows no `SecurityError` per frame. Served normally, a
controller still works after pressing a button.

Add to the Web row of `plugins/gamepad-input/CLAUDE.md` → Platform Differences: when the page's permissions policy
blocks the Gamepad API (a cross-origin iframe without `allow="gamepad"`), the plugin stays inert.
