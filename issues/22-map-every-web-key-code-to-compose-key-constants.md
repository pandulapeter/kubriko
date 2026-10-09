# Map every web key code to Compose's own Key constants

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** Web (Wasm)
**Challenged:** sound
**Files:** `plugins/keyboard-input/src/webMain/kotlin/com/pandulapeter/kubriko/keyboardInput/implementation/KeyboardEventHandler.web.kt`, `plugins/keyboard-input/CLAUDE.md`

Ships in `io.github.pandulapeter.kubriko:plugin-keyboard-input`. `mapKeyboardEventCodeToKey(code: String): Key` is a
**public** top-level function of the web artifact (it compiles into the `KeyboardEventHandler.web.kt` facade); keep
its name, file and signature. Its return values change only where they are wrong today (Meta, and keys that now map
instead of returning `Key(-1)`), which is the bug being fixed, not a decision.

## Problem

The web backend maps `KeyboardEvent.code` with hand-written key codes instead of Compose's `Key` constants:

```kotlin
// KeyboardEventHandler.web.kt
"MetaLeft", "MetaRight" -> Key(91) // Meta (Windows/Cmd)
"Delete" -> Key(46)    // Delete
else -> Key(-1)        // Unknown
```

Verified against `ui-wasm-js-1.12.1-sources.jar` (the version in `gradle/libs.versions.toml`), `Key.web.kt`:
`MetaLeft = Key(224)`, `MetaRight = Key(-2147483424)`. `Key(91)` is no Compose key at all, so
`activeKeys.contains(Key.MetaLeft)` is never true on the web while it is on Desktop and Android.

Every code missing from the `when` returns `Key(-1)` (`Key.Unknown`): `Minus`, `Equal`, `F1`–`F12`, the whole numpad,
`Home`/`End`/`PageUp`/`PageDown`/`Insert`, `CapsLock`, `NumLock`, `ScrollLock`, brackets, `Backslash`, `Semicolon`,
`Quote`, `Comma`, `Period`, `Slash`, `Backquote`. Consequences:

- `Set<Key>.zoomState` (`KeyExtensions.kt`: `Key.Equals`/`Key.Minus`/`Key.NumPadAdd`/`Key.NumPadSubtract`) never zooms
  on the web; F-key shortcuts never fire.
- All unknown keys collapse into one `Key(-1)`: hold `F1`, press `F2` — the manager drops the second press
  (`activeKeysCache` already holds `Key(-1)`), and releasing either reports `Key(-1)` released while the other is
  still down. Desktop and Android simply ignore keys they don't map (`keyMap[keyCode]` → `null`).
- A virtual-keyboard event has an empty `code`; Compose falls back to `KeyboardEvent.key` for those, Kubriko returns
  `Key(-1)`.

## Fix

1. Replace the `when` with a private `Map<String, Key>` built from Compose's `Key.*` constants, mirroring the
   `codeMap` in Compose 1.12.1's `KeyEvent.web.kt` entry for entry: `KeyA`..`KeyZ` → `Key.A`..`Key.Z`, `Digit0`..`Digit9`
   → `Key.Zero`..`Key.Nine`, `Numpad0`..`Numpad9` → `Key.NumPad0`..`Key.NumPad9`, `NumpadDivide`/`NumpadMultiply`/
   `NumpadSubtract`/`NumpadAdd`/`NumpadEnter`/`NumpadEqual`/`NumpadDecimal` → `Key.NumPadDivide`/`NumPadMultiply`/
   `NumPadSubtract`/`NumPadAdd`/`NumPadEnter`/`NumPadEquals`/`NumPadDot`, `NumLock`, `Minus` → `Key.Minus`, `Equal` →
   `Key.Equals`, `Backspace`, `BracketLeft`/`BracketRight` → `Key.LeftBracket`/`RightBracket`, `Backslash`,
   `Semicolon`, `Enter`, `Comma`, `Period`, `Slash`, `ArrowLeft`/`Up`/`Right`/`Down` → `Key.DirectionLeft`/…,
   `Home` → `Key.MoveHome`, `End` → `Key.MoveEnd`, `PageUp`, `PageDown`, `Delete`, `Insert`, `Backquote` → `Key.Grave`,
   `Tab`, `CapsLock`, `Escape`, `F1`..`F12`, `Space` → `Key.Spacebar`, `Quote` → `Key.Apostrophe`, plus `ScrollLock` →
   `Key.ScrollLock` (Desktop and Android map it; Compose's web table happens not to).
2. Modifiers: map `ShiftLeft`/`ShiftRight` → `Key.ShiftLeft`, `ControlLeft`/`ControlRight` → `Key.CtrlLeft`,
   `AltLeft`/`AltRight` → `Key.AltLeft`, `MetaLeft`/`MetaRight` → `Key.MetaLeft`. The left/right collapse keeps
   today's web behaviour for Shift/Ctrl/Alt (both sides already map to the left `Key`) and matches Desktop, whose AWT
   key codes can't tell the sides apart (`KeyEvent.VK_SHIFT to Key.ShiftLeft`, …). Only Meta's value changes, from the
   bogus `Key(91)` to `Key.MetaLeft`.
3. `mapKeyboardEventCodeToKey(code)` returns `map[code] ?: Key.Unknown` (unchanged contract for unknown codes). Give
   it KDoc (public API): what it maps, that right-hand modifiers report the left `Key`, and that unknown codes give
   `Key.Unknown`.
4. The handler: resolve the lookup string as Compose does — `code` when non-empty, otherwise `KeyboardEvent.key`;
   track that string in `pressedKeys` as today; and **skip** keys that resolve to `Key.Unknown` (no press, no
   release), matching Desktop and Android. Keep the `pressedKeys` de-duplication by string so `ShiftLeft` and
   `ShiftRight` held together still produce one press and one release of `Key.ShiftLeft` — check the manager
   handles a release of one side while the other is held: the second side's `keydown` is dropped by the manager
   (already active) and the first side's `keyup` releases `Key.ShiftLeft`; this is the same as today and as Desktop.
5. The map is built once (top-level `private val`); lookups don't allocate.
6. `CLAUDE.md` Web row: replace "Many keys return `Key(-1)` (unmapped); test on target" with "`KeyboardEvent.code`
   mapped with Compose's own table; right-hand modifiers report the left `Key` (as on Desktop); unmapped keys are
   ignored". Update the matching Gotcha bullet ("Web: test with physical hardware — many `Key(-1)` returns…").

## Tests

None runnable: the Wasm test tasks are disabled (`commonTest` runs on the JVM only) and the table lives in webMain. The
fix replaces literals with `Key.*` constants, which is exactly what a test would assert. Compile `:plugins:keyboard-input:build`.

## Manual check

Showcase in Chrome and Firefox (`./gradlew :app:web:wasmJsBrowserDevelopmentRun`), `test-input` example (with
`showcase.areTestExamplesEnabled=true`): press Minus/Equal (zoom), F1–F12, numpad keys, Home/End/PageUp/PageDown,
Cmd/Win, and both Shifts — each must light up its own key on the example's on-screen keyboard (compare with the
desktop build). Hold F1 and press F2: both must be reported. In a game, Minus/Equal must zoom where the game zooms
with `zoomState`.
