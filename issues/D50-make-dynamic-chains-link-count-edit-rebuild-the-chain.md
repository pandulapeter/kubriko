# Make editing `DynamicChain`'s `linkCount` in the scene editor rebuild the whole chain, and settle what `linkCount` counts.

**Kind:** bug  ·  **Severity:** low  ·  **Platforms:** desktop (scene editor)  ·  **Class:** Planned
**Artifact:** unpublished (examples)
**Rebased:** on 70de96c6 after the Now plans landed.
**Files:** `examples/demo-physics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPhysics/implementation/actors/DynamicChain.kt`, `examples/demo-physics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPhysics/implementation/managers/PhysicsDemoManager.kt`, `examples/demo-physics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPhysics/implementation/PhysicsDemoStateHolderImpl.kt`, `examples/demo-physics/src/commonMain/composeResources/files/scenes/scene_physics_test.json`, `examples/demo-physics/src/desktopTest/kotlin/com/pandulapeter/kubriko/demoPhysics/implementation/actors/DynamicChainTest.kt`, `examples/demo-physics/CLAUDE.md`

## Problem
- `@set:Exposed(name = "linkCount") var linkCount = state.linkCount set(value) { field = value; chainLinks = generateLinks() }` (`DynamicChain.kt:44-49`) replaces `chainLinks`, but `private val joints = chainLinks.mapIndexedNotNull { … }` (:51–64) and `override val actors = chainLinks + joints` (:67) were computed once at construction. After an edit the chain draws (and sizes its body from) links that are not in the scene, have no joints and receive no physics/`update`; the old links stay in the scene.
- `generateLinks()` centres with `state.linkCount` (:98) instead of the current `linkCount`.
- `(0..linkCount)` creates `linkCount + 1` links and `linkCount` joints, while `examples/demo-physics/CLAUDE.md` (:43–44) says "`linkCount` `ChainLink` … and `linkCount - 1` `JointToBody`"; odd counts are also centred off by `LinkDistance / 2` (integer `/ 2`).
- `Group.actors` is read by the engine when the group is added, so a group cannot change its children in place.

## Fix
D15 (`save()` round-trips, covered by `DynamicChainTest.saveReturnsTheStateTheChainWasRestoredFrom`) landed in 8f5d8ed1. Options:
1. **Drop the `@Exposed` setter** (make `linkCount` a `val`); a different length is made by deleting and re-adding a chain or editing the JSON. Simplest, no editor behaviour to invent.
2. Rebuild through the editor: the setter cannot replace the actor itself, so this needs the scene editor to re-create an actor from `save().copy(linkCount = value).restore()` after an `@Exposed` edit — a scene-editor (published `tool-scene-editor`) behaviour change, out of this lane.
3. Make the chain re-add its children: on change, `actorManager.remove(oldLinks + oldJoints)` and `add(new…)` with `actors` turned into a mutable backing list — works only while added, and fights the Group contract.

Separately decide the count semantics: (a) `linkCount` = number of links (`0 until linkCount`, `linkCount - 1` joints, float centring `LinkDistance * (linkCount - 1) / 2f`) — changes every existing chain by one link and re-centres odd ones, so re-save `scene_physics_test.json` (23 → 24 and 21 → 22 to keep today's chains) and adjust `DynamicChainTest` (it round-trips 20 and 21); or (b) keep today's `linkCount + 1` and only fix `CLAUDE.md`.

## Decision
- Setter: options 1 / 2 / 3 — **recommended 1** (drop `@Exposed`).
- Semantics: (a) / (b) — **recommended (b)** (no data migration; document "`linkCount + 1` links, `linkCount` joints").

## Behaviour
With 1+(b): the property disappears from the editor's inspector; nothing else changes.

## Public API
None.

## Tests
Extend `DynamicChainTest`: `actors` holds `linkCount + 1` links and `linkCount` joints (pure; no Kubriko instance needed).

## Verify
`./gradlew :examples:demo-physics:desktopTest`

## Manual check
Desktop scene editor: select a chain — `linkCount` is no longer editable (option 1), chains in the shipped scene are unchanged.
