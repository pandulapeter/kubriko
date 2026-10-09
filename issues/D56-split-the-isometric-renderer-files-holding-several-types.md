# Split the isometric renderer files that hold several top-level types, after doing it in Tesselar first.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Planned
**Artifact:** unpublished (examples)
**Rebased:** on 70de96c6 after the Now plans landed.
**Files:** `examples/demo-isometric-graphics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoIsometricGraphics/implementation/renderer/data/CuboidKeyframe.kt`, `examples/demo-isometric-graphics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoIsometricGraphics/implementation/renderer/data/KeyframedColor.kt` (new), `examples/demo-isometric-graphics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoIsometricGraphics/implementation/renderer/data/animation/CuboidAnimationInterpolation.kt`, `examples/demo-isometric-graphics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoIsometricGraphics/implementation/renderer/volumetric/actor/VolumetricCuboidRenderer.kt`

## Problem
- `CuboidKeyframe.kt` holds `data class CuboidKeyframe` (:17) and `data class KeyframedColor` (:61).
- `CuboidAnimationInterpolation.kt` holds `internal class SortedFloatTrack` (:24), `internal class SortedColorTrack` (:31) and `class PreparedCuboidTracks internal constructor` (:38).
- `VolumetricCuboidRenderer.kt` is exactly 500 lines, at the code style's limit.

## Fix
These are `renderer/` files of the flattened Tesselar copy, kept in sync by hand; splitting them only here makes every later re-flatten a manual merge. Tesselar has already diverged: its `CuboidKeyframe.kt` holds only `CuboidKeyframe` (KeyframedColor already split out) and its `VolumetricCuboidRenderer.kt` is 1 722 lines. So: **do not split here independently** — either (a) re-flatten the current Tesselar renderer into this module (a sync, which brings the splits with it), or (b) split only `KeyframedColor` here to match Tesselar's layout and leave the rest until the next sync.

## Decision
(a) full re-sync from Tesselar (separate, larger task), (b) mirror Tesselar's `KeyframedColor.kt` split only, or (c) leave as is until the next sync — recommended (c), with (b) as a cheap alternative. Sync cost: every split here that Tesselar lacks must be redone on the next flatten.

## Behaviour
Verbatim moves only.

## Public API
None.

## Tests
None.

## Verify
`./gradlew :examples:demo-isometric-graphics:compileKotlinDesktop`

## Manual check
none
