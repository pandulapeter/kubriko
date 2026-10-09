# Correct the `shouldDrawBorder` explanation in demo-content-shaders' `CLAUDE.md`.

**Kind:** docs  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/demo-content-shaders/CLAUDE.md`

## Problem
The paragraph "**`shouldDrawBorder` lambda on `ColorfulBox`.** Rather than storing a reference to a shared flag and reading it every frame (which would cause a capture), the border visibility check is delegated as a function reference `{ !state.value.isComicShaderEnabled }` evaluated inside `draw()`." is backwards: the code at `examples/demo-content-shaders/src/commonMain/kotlin/com/pandulapeter/kubriko/demoContentShaders/implementation/managers/ContentShadersDemoManager.kt:94` (`shouldDrawBorder = { !state.value.isComicShaderEnabled },`) *is* a capturing lambda (not a function reference); it is created once per box in `onInitialize`, not per frame, so it costs nothing per frame.

## Fix
Reword the paragraph to: each `ColorfulBox` receives one capturing `shouldDrawBorder` lambda (`{ !state.value.isComicShaderEnabled }`), created once at setup rather than per frame and evaluated inside `draw()`; this keeps `ColorfulBox` free of direct Manager dependencies. Keep the surrounding prose style.

## Behaviour
Docs only.

## Public API
None.

## Tests
None (docs).

## Verify
none (docs only)

## Manual check
none
