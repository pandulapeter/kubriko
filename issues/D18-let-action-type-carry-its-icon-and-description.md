# Let `ActionType` carry its icon and content-description resources instead of two parallel `when` blocks.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/demo-physics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPhysics/implementation/ui/ActionType.kt`, `examples/demo-physics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPhysics/implementation/ui/PhysicsDemoOverlay.kt` (after D17)

## Problem
The action button picks its icon and its content description with two separate `when (selectedActionType.value)` blocks (originally `PhysicsDemoManager.kt:137-141` → `Res.drawable.ic_shape/ic_chain/ic_explosion` and `:143-148` → `Res.string.shape/chain/explosion`; after D17 they sit in `PhysicsDemoOverlay`). `internal enum class ActionType { SHAPE, CHAIN, EXPLOSION, }` (`ActionType.kt:12-16`) holds nothing. The code style says enum entries that reach a Composable store their `StringResource`.

## Fix
- `internal enum class ActionType(val icon: DrawableResource, val contentDescription: StringResource) { SHAPE(Res.drawable.ic_shape, Res.string.shape), CHAIN(Res.drawable.ic_chain, Res.string.chain), EXPLOSION(Res.drawable.ic_explosion, Res.string.explosion), }` (same entry order — `changeSelectedActionType` cycles by ordinal; trailing comma).
- In `PhysicsDemoOverlay`: `icon = actionType.icon`, `contentDescription = stringResource(actionType.contentDescription)`; drop the six accessor imports there.

## Behaviour
Same resources per state, same cycling order.

## Public API
None.

## Tests
The existing ones (none).

## Verify
`./gradlew :examples:demo-physics:compileKotlinDesktop`

## Manual check
Cycle the physics action button: icons and accessibility labels are unchanged.
