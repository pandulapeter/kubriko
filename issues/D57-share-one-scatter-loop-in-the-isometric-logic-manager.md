# Share one rejection-sampling scatter loop for the isometric demo's NPCs, trees and bushes, after doing it in Tesselar first.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Planned
**Artifact:** unpublished (examples)
**Rebased:** re-checked on b31bb3f7 after the Planned plans landed (quoted snippets hold; line numbers may be off by a few).
**Files:** `examples/demo-isometric-graphics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoIsometricGraphics/implementation/logic/manager/LogicManager.kt`

## Problem
`LogicManager.loadModelsAndSetActors` repeats the same rejection-sampling loop three times with different targets and spacing: `while (npcPositions.size < 64 && npcAttempts < 10000) { … }` (:82), `while (treePositions.size < 256 && treeAttempts < 10000) { … }` (:110), `while (bushPositions.size < 256 && bushAttempts < 10000) { … }` (:134).

## Fix
Extract `private fun scatter(count: Int, maxAttempts: Int, isAcceptable: (SceneOffset) -> Boolean): List<SceneOffset>` (or a pure `internal` function taking a `Random` so a test can seed it). This is gameplay code of the Tesselar copy — Tesselar's own `gameplay/manager/LogicManager.kt` has since grown into a region-based world, so the loops no longer map one-to-one; a change here is Kubriko-only and survives a re-flatten only by hand.

## Decision
(a) dedupe here and accept the sync cost, or (b) leave it until the demo is re-synced from Tesselar — recommended (b).

## Behaviour
Same random draws in the same order per loop, so the same world for the same seed.

## Public API
None.

## Tests
A seeded-`Random` test of the extracted function (count reached, all accepted points satisfy the predicate).

## Verify
`./gradlew :examples:demo-isometric-graphics:desktopTest`

## Manual check
Isometric demo: NPCs, trees and bushes are scattered as before.
