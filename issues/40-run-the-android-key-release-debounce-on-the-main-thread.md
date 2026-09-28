# Run the Android keyboard handler's release debounce and listener registration on the main thread

**Challenged:** sound

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** Android
**Artifact:** `plugin-keyboard-input` (internal change only)
**Files:** `plugins/keyboard-input/src/androidMain/kotlin/com/pandulapeter/kubriko/keyboardInput/implementation/KeyboardEventHandler.android.kt`, `plugins/keyboard-input/CLAUDE.md`

## Problem

The handler is created with `coroutineScope = scope`, the Kubriko scope on `Dispatchers.Default`. Two of its
coroutines therefore run off the main thread while the view's key listener runs on it:

1. The release debounce loop:
   ```kotlin
   pendingReleaseJob = coroutineScope.launch {
       ...
       val iterator = keyReleasedTimestamps.entries.iterator()
       while (iterator.hasNext()) {
           val entry = iterator.next()
           if (currentTime - entry.value > DEBOUNCE_TIME_MILLIS) {
               val key = entry.key
               iterator.remove()
               onKeyReleased(key)
           }
       }
   ```
   `keyReleasedTimestamps` is a plain `LinkedHashMap` that the listener writes on the main thread
   (`keyReleasedTimestamps[key] = System.currentTimeMillis()` on `ACTION_UP`, `remove(key)` on `ACTION_DOWN`). Typing
   or holding several keys makes the loop's iteration overlap those writes: `ConcurrentModificationException` on a
   `Default` worker, which is uncaught in the Kubriko scope and **crashes the app**; or a lost entry, which leaves a
   key held forever. The loop also calls `onKeyReleased` — and through it `KeyboardInputManagerImpl`'s
   `activeKeysCache` and every Actor's `onKeyReleased` — from the background thread, while presses arrive on main.
2. The `isListening` collector (`isListening.onEach { … view.addOnUnhandledKeyEventListener(keyListener) … }
   .launchIn(coroutineScope)`) adds and removes the view's listener off the main thread. `View`'s listener list is not
   thread-safe; the `catch (_: ArrayIndexOutOfBoundsException)` / `catch (_: NullPointerException)` around it are
   the symptoms of exactly that race.

## Fix

Run both on the main thread, so the whole handler is single-threaded:

- `pendingReleaseJob = coroutineScope.launch(Dispatchers.Main) { … }` — the loop only `delay`s and waits on the
  channel, so it costs the main thread nothing while idle.
- `isListening.onEach { … }.launchIn(coroutineScope + Dispatchers.Main)`.

Leave the `try`/`catch` blocks in place: `stopListening()` also removes the listener directly and can be called
during an Activity's teardown, and proving they are no longer needed requires a device run the plan can't guarantee.

## Tests

None: the handler is built from an Activity's decor view and Android `KeyEvent`s; the plugin has no Android tests.

## Manual check

Android device or emulator with a hardware keyboard, a keyboard-driven game (Wallbreaker, Blocky's Journey): mash and
roll several keys (e.g. hold A+D and tap W/S rapidly) for a minute — no crash, and the character stops as soon as
every key is up. Rotate the device (Activity recreation re-registers the listener) while holding a key several times:
no crash, and keys keep working afterwards.

Add to the Android row of `plugins/keyboard-input/CLAUDE.md` → Platform Differences that the debounce loop runs on
the main thread with the key listener.
