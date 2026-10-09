# Preload test-input's gamepad strings in its resource gate like every other on-screen string.

**Kind:** bug  ·  **Severity:** low  ·  **Platforms:** all (visible on web)  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/test-input/src/commonMain/kotlin/com/pandulapeter/kubriko/testInput/implementation/InputTestStateHolder.kt`

## Problem
`examples/test-input/src/commonMain/kotlin/com/pandulapeter/kubriko/testInput/implementation/InputTestStateHolder.kt:34`: `private fun areStringResourcesLoaded() = preloadedString(Res.string.description).value.isNotBlank()` gates the Showcase loading screen on `description` only, but `examples/test-input/src/commonMain/kotlin/com/pandulapeter/kubriko/testInput/implementation/ui/Gamepads.kt` shows eight more strings from the first frame (`gamepad_none` / `gamepad_none_web` are shown whenever no gamepad is connected): `gamepad_none`, `gamepad_none_web`, `gamepad_header`, `gamepad_unknown_name`, `gamepad_sticks`, `gamepad_triggers`, `gamepad_buttons`, `gamepad_buttons_none`. Every other module lists all its on-screen strings, so here the gamepad text can pop in blank-then-filled after the gate opens on platforms where resources load asynchronously (web).

## Fix
Append `&& preloadedString(Res.string.<key>).value.isNotBlank()` for the eight keys (same continuation-line style as the other modules), with their `Res.string` accessor imports. `preloadedString` reads the raw template, which is non-blank for the parameterized ones too.

## Behaviour
The Showcase's loading gate now also waits for these eight strings; afterwards the readout appears filled in on the first frame.

## Public API
None.

## Tests
None (composition-only).

## Verify
`./gradlew :examples:test-input:compileKotlinDesktop` and `./gradlew :examples:test-input:compileKotlinWasmJs`

## Manual check
Web Showcase with test examples enabled: open the Input test with no gamepad — "No gamepad connected…" appears together with the rest of the screen.
