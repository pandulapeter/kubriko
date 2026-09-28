# Release held keys on focus loss from the main thread, where every other key state change happens

**Challenged:** sound

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** all
**Artifact:** `plugin-keyboard-input` (internal change only; the same callbacks fire, on the thread they already fire on everywhere else)
**Files:** `plugins/keyboard-input/src/commonMain/kotlin/com/pandulapeter/kubriko/keyboardInput/KeyboardInputManagerImpl.kt`, `plugins/keyboard-input/CLAUDE.md`

## Problem

```kotlin
override fun onInitialize(kubriko: Kubriko) {
    stateManager.isFocused
        .filterNot { it }
        .onEach { releaseAllActiveKeys() }
        .launchIn(scope)
}
...
private fun releaseAllActiveKeys() {
    activeKeysCache.forEach { key -> keyboardInputAwareActors.value.forEach { it.onKeyReleased(key) } }
    activeKeysCache.clear()
    isActiveKeysDirty = true
}
```

`scope` is the Kubriko scope, `SupervisorJob() + Dispatchers.Default`, so this collector — and every
`onKeyReleased` it dispatches — runs on a background thread. Everything else that touches `activeKeysCache` (a plain
`LinkedHashSet`) runs on the main thread: the platform key events (`onKeyPressed`/`onKeyReleased`, from AWT's event
thread, the Android view, the browser, UIKit) and, with the default `TickSource.viewportFrames()`, `onUpdate`. A
focus change arrives while the player is typing more often than not (alt-tab happens with keys held, and the key-up
that follows the focus loss is delivered on main at the same moment), so:

- iterating `activeKeysCache` while main adds or removes a key throws `ConcurrentModificationException` on a
  `Default` worker — an uncaught exception in the Kubriko scope, which **crashes the app on Android and iOS**;
- or the set is corrupted, leaving a key that is reported as held forever (`isKeyPressed` true, `handleActiveKeys`
  keeps including it);
- and Actors receive `onKeyReleased` on a background thread while their other callbacks run on main, so their own
  unsynchronised state races too.

## Fix

Collect on the main thread, the way `keyboardInputAwareActors` is already built with `asStateFlowOnMainThread`:

```kotlin
stateManager.isFocused
    .filterNot { it }
    .onEach { releaseAllActiveKeys() }
    .launchIn(scope + Dispatchers.Main)
```

(`import kotlinx.coroutines.Dispatchers` and `kotlinx.coroutines.plus`.) `Dispatchers.Main` is available on every
target: `Manager.asStateFlowOnMainThread` already relies on it, and the engine's desktop artifact ships
`kotlinx-coroutines-swing`.

Why not a flag consumed in `onUpdate`: with the default tick source no tick runs while the window is unfocused, so
the release would only reach Actors when the player comes back — the opposite of what the flush is for.

Out of scope (note only): with a coroutine-based `TickSource` (`fixedRate`, `fixedFrequency`) `onUpdate` itself runs
on `Dispatchers.Default` and races the main-thread key events; that is a property of those tick sources for every
input plugin, not of this collector.

## Tests

None: the race needs the platform to deliver key events on the main thread concurrently with a focus change, and the
focus state is set by `KubrikoViewport` through an engine-internal call a plugin test can't make. The change is one
dispatcher.

## Manual check

Android with a hardware (or Bluetooth) keyboard, and desktop: in a keyboard-driven game (Wallbreaker's paddle,
Blocky's Journey), hold an arrow key and switch away (Alt/Cmd-Tab, Home button) and back twenty times, releasing the key
at varying moments: no crash, the character never keeps moving with no key held, and a debugger breakpoint in
`releaseAllActiveKeys` shows the main thread (`EventQueue.isDispatchThread()` on desktop, `Looper.myLooper() ==
Looper.getMainLooper()` on Android).

Update `plugins/keyboard-input/CLAUDE.md` → "On focus loss, all active keys are flushed immediately to prevent
stuck-key state." — add "on the main thread, like the platform key events".
