# Turn the isometric mini map's declaration comments into KDoc.

**Kind:** docs  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/demo-isometric-graphics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoIsometricGraphics/implementation/ui/MiniMap.kt`, `examples/demo-isometric-graphics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoIsometricGraphics/implementation/ui/MiniMapBuffer.kt`, `examples/demo-isometric-graphics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoIsometricGraphics/implementation/ui/MiniMapSampler.kt` (both from D42)

## Problem
Declarations are documented with `//` blocks: above `MiniMap` ("// 128 dp circular top-down minimap, …", originally :58–62), above `MiniMapBuffer` ("// Reusable sample storage …", :170–173), above `MiniMapSampler` ("// Double-buffered, allocation-free sampling …", :216–220), above `fun sample` ("// Returns true when the new sample differs from the previous one.", :228) and above `quantize` ("// Snaps to half-pixel steps …", :301). The code style requires KDoc (`/** … */`) for any comment documenting a declaration; `//` is for statements inside bodies.

## Fix
Convert exactly those five blocks to KDoc with the same wording (adjust "stateHolder.logicKubriko" to the D43 parameter name if still present). Leave statement comments inside bodies (`// Kept invisible but composed: …`, `// Circular culling …`, `// Use the model's root rotation …`, etc.) as `//`.

## Behaviour
Comments only.

## Public API
None.

## Tests
None.

## Verify
`./gradlew :examples:demo-isometric-graphics:compileKotlinDesktop`

## Manual check
none
