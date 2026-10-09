# Delete `StaticCircle`'s unused `viewportManager` and make the physics state holder's `physicsDemoManager` private.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/demo-physics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPhysics/implementation/actors/StaticCircle.kt`, `examples/demo-physics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPhysics/implementation/PhysicsDemoStateHolderImpl.kt` (after D14)

## Problem
- `examples/demo-physics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPhysics/implementation/actors/StaticCircle.kt:39-43`: `private lateinit var viewportManager: ViewportManager` and `override fun onAdded(kubriko: Kubriko) { viewportManager = kubriko.get() }` — nothing reads it (grep the file).
- `val physicsDemoManager by lazy {` in `PhysicsDemoStateHolderImpl` is public but only read inside the Impl (grep `physicsDemoManager` repo-wide).

## Fix
Remove the property and the `onAdded` override with their now-unused imports (`Kubriko`, `get`, `ViewportManager`); change `val physicsDemoManager` to `private val`.

## Behaviour
No readers; `Actor.onAdded` defaults to `Unit`.

## Public API
None.

## Tests
The existing ones (none).

## Verify
`./gradlew :examples:demo-physics:compileKotlinDesktop`

## Manual check
none
