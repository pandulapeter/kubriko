# Release and forget the web keyboard handler's held keys when the window loses focus

**Challenged:** sound

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** web
**Artifact:** `plugin-keyboard-input` (internal change only)
**Files:** `plugins/keyboard-input/src/webMain/kotlin/com/pandulapeter/kubriko/keyboardInput/implementation/KeyboardEventHandler.web.kt`, `plugins/keyboard-input/CLAUDE.md`

## Problem

The web handler de-duplicates the browser's key repeat with its own set:

```kotlin
private val pressedKeys = mutableSetOf<String>() // Track keys that are currently pressed
private val keyDownListener: (Event) -> Unit = { event ->
    (event as? KeyboardEvent)?.let { keyboardEvent ->
        if (pressedKeys.add(keyboardEvent.code)) {
            onKeyPressed(mapKeyboardEventCodeToKey(keyboardEvent.code))
        }
    }
}
```

`pressedKeys` is only cleared in `stopListening()`. A browser does not send `keyup` for a key released while the
window is not focused, so:

1. hold W, alt-tab away (or click into the address bar, or open another tab), release W;
2. come back and press W: `pressedKeys.add("KeyW")` returns `false`, so **the press is swallowed** — the manager
   already released W on focus loss, and now never hears it pressed again until the player releases and presses it a
   second time.

The same happens when focus moves into an iframe on the page: then `document.hasFocus()` stays true, the engine
does not report a focus loss, and the manager keeps W held forever because the `keyup` goes to the iframe.

## Fix

Listen for the window's `blur` and report every key still tracked as released, then clear the set:

```kotlin
private val blurListener: (Event) -> Unit = {
    pressedKeys.forEach { code -> onKeyReleased(mapKeyboardEventCodeToKey(code)) }
    pressedKeys.clear()
}
```

Register it in `startListening()` next to the key listeners (`window.addEventListener("blur", blurListener)`) and
remove it in `stopListening()`. Reporting the releases (instead of only clearing) covers the iframe case, where the
manager gets no focus loss; in the ordinary alt-tab case it runs before the manager's own focus-loss flush (the
engine's focus flow reaches the manager asynchronously), so that flush then finds nothing left and no key is released
twice.

## Tests

None: needs browser `window` events; the plugin has no browser test setup.

## Manual check

Web Showcase in Chrome and Firefox, a keyboard-driven game:
1. Hold an arrow key, Alt-Tab to another window, release the key, come back, press the same key: the game reacts to
   the press.
2. Hold an arrow key, click the browser's address bar, release, click back into the game: the character is not
   still moving.

Add a line to the Web row of `plugins/keyboard-input/CLAUDE.md` → Platform Differences: keys held when the window
loses focus are reported as released then, since the browser sends no `keyup` for them.
