# Move `ControlsState` out of `ControlsContainer.kt` into `ui/ControlsState.kt`, and correct its `CLAUDE.md` description.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/demo-shader-animations/src/commonMain/kotlin/com/pandulapeter/kubriko/demoShaderAnimations/implementation/ui/ControlsContainer.kt`, `examples/demo-shader-animations/src/commonMain/kotlin/com/pandulapeter/kubriko/demoShaderAnimations/implementation/ui/ControlsState.kt` (new), `examples/demo-shader-animations/CLAUDE.md`

## Problem
- `internal enum class ControlsState { COLLAPSED, EXPANDED_CODE, EXPANDED_CONTROLS; }` (`ControlsContainer.kt:230-234`) sits at the bottom of a Composable's file but is also used by the state holder Impl.
- `examples/demo-shader-animations/CLAUDE.md` ("**Controls state machine.**") names the values `COLLAPSED`, `EXPANDED`, `CODE_VISIBLE`; the real ones are `COLLAPSED`, `EXPANDED_CODE`, `EXPANDED_CONTROLS`.
- The same file's "**Generic `ShaderAnimationsDemoManager<SHADER, STATE>`.**" paragraph says the updater lambda "avoids any cast", while `ControlsContainer.kt`'s `Controls` does five `@Suppress("UNCHECKED_CAST")` casts (`:184-225`).

## Fix
- Create `examples/demo-shader-animations/src/commonMain/kotlin/com/pandulapeter/kubriko/demoShaderAnimations/implementation/ui/ControlsState.kt` (license header, package `…implementation.ui`) and move the enum verbatim; delete it (and the two blank lines above it) from `ControlsContainer.kt`. Same package, so no import changes.
- CLAUDE.md: fix the three value names, and reword "this avoids any cast" to say the Manager itself needs no cast, while the controls UI casts the selected Manager to its concrete type (planned D51 removes those casts).
- Grep the repo for `ControlsState` path references.

## Behaviour
Verbatim move.

## Public API
None.

## Tests
The existing ones (none).

## Verify
`./gradlew :examples:demo-shader-animations:compileKotlinDesktop`

## Manual check
none
