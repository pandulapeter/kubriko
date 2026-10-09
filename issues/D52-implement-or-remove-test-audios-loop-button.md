# Decide whether test-audio's permanently disabled loop button gets implemented or removed with its dead resources.

**Kind:** bug  ·  **Severity:** low  ·  **Platforms:** android, desktop  ·  **Class:** Planned
**Artifact:** unpublished (examples)
**Files:** `examples/test-audio/src/commonMain/kotlin/com/pandulapeter/kubriko/testAudio/implementation/ui/MusicControls.kt` (after D30), `examples/test-audio/src/commonMain/kotlin/com/pandulapeter/kubriko/testAudio/implementation/managers/AudioTestManager.kt`, `examples/test-audio/src/commonMain/kotlin/com/pandulapeter/kubriko/testAudio/implementation/AudioTestStateHolder.kt`, `examples/test-audio/src/commonMain/composeResources/values/strings.xml`, `examples/test-audio/src/commonMain/composeResources/drawable/ic_loop_on.xml`, `examples/test-audio/src/commonMain/composeResources/drawable/ic_loop_off.xml`, `examples/test-audio/CLAUDE.md`
**Challenged:** amended — option A notes `play()` already loops by default (initial `true`) and preloads `loop_off`; option B also rewords the on-screen `description`, which advertises looping.

## Problem
- The loop button is hardwired: `ControlButton(icon = Res.drawable.ic_loop_on, contentDescription = Res.string.loop_on, isEnabled = false, onClick = {})` (originally `AudioTestManager.kt:163-168`).
- `ic_loop_off` is preloaded in the resource gate (`AudioTestStateHolder.kt:43`) but never shown; the `loop_off` string is unused anywhere.
- `examples/test-audio/CLAUDE.md` ("play, pause, stop, and looping") promises a looping test the screen cannot perform.
- `MusicManager.play(uri, shouldLoop = …)` already supports a per-play loop flag, so the feature is a few lines away.

## Fix
Option A (**recommended** — this is a plugin test page, and looping is a `MusicManager` feature worth exercising): track `isTrackNLooping` (`mutableStateOf(true)`) in `AudioTestManager`, pass `shouldLoop` to `play`, enable the button and toggle between `ic_loop_on`/`loop_on` and `ic_loop_off`/`loop_off`. Today's `play(uri)` already loops (`shouldLoop` defaults to `true`), so `true` is the behaviour-preserving initial value; after D30 this means passing the flag through `togglePlayback`. Add `loop_off` to the string preloads next to `loop_on`. Check what `play(shouldLoop)` does to a track that is already playing (the toggle may only take effect on the next play — say so in the button's state).
Option B: remove the button, both loop drawables, `loop_on`/`loop_off` and their preloads, drop "looping" from CLAUDE.md, and reword the on-screen `description` string ("…test the play, pause, stop, and loop features…"), which promises it too.

## Decision
A (implement) or B (remove) — recommended A.

## Behaviour
A: the button becomes active. B: the row loses a disabled icon.

## Public API
None.

## Tests
None (needs audio).

## Verify
`./gradlew :examples:test-audio:compileKotlinDesktop`

## Manual check
Desktop/Android: with loop on a track restarts at its end; off it stops.
