<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Reuse, Traits, and bigger games

What GameMaker had for games past a few objects: parent objects, scripts, and settings per object (persistent, visible, depth), plus
Kubriko's own Traits. Without them, 20 enemy types are 20 copies of the same Blocks, and a growing game leaves Creator for Kotlin.
Every addition lands with its command, AI tool, generator tests, and a case in the integration test's every-Block types, as in
`02-blocks.md`.

**Order:** parent types first (they change the Actor type file and what every type reference means), then functions, then Traits, then
the rest. Each is generated into ordinary Kotlin, as now: no runtime interpreter.

## Parent Actor types

An Actor type can name a parent (`parent` in its JSON), like GameMaker's parent objects. Chains are allowed (Enemy → FlyingEnemy →
Bat), cycles are rejected.

- **What a child gets:** the parent's Properties (it may change their initial values, not their kind), its appearance unless the child
  sets its own, and its Events. A child's Event of the same kind (same key, other Actor type, or Timer) replaces the parent's; the new
  "Run the parent's Event" Block (GameMaker's `event_inherited`) runs the parent's Blocks at that point.
- **A parent stands for its children:** a collision Event with Enemy fires for any Enemy's child, moveTowardsNearest Enemy and (later)
  counting Enemies include children, and Blocks that read another Actor's Property (`other.hp`) work for every child. addActor of a
  parent creates the parent itself, which an "only a parent" toggle forbids (such a type is also left out of placing).
- **Generated code:** each type stays one flat, self-contained class (the parent's Properties and Events generated into it), not a Kotlin
  `open class` hierarchy, which `Editable<T>` and the serializable `State` make awkward. Every type with children also gets an interface
  (`EnemyFamily`, a reserved suffix) that the type and all its descendants implement, declaring the shared Properties; collision
  `collidableTypes`, `is` checks, and lookups use it. Kubriko's collision matching uses `KClass.isInstance`, so interfaces work; check
  that it holds on Wasm before relying on it.
- **Editing:** the Actor type editor shows inherited Properties and Events greyed, with "override" and "back to the parent's"; renaming or
  deleting a parent follows its children (deleting asks whether to copy what they inherit into them).
- **Scenes** are unaffected: every Actor names its concrete type.

## Functions

Named Block lists with parameters (number, text, toggle) and an optional result, called by a "Run function" Block, like GameMaker's
scripts:

- **On an Actor type:** methods of its Actors, inherited by children and overridable (the parent's runs through "Run the parent's
  function"). Generated as Kotlin functions on the class (and the family interface).
- **On a Manager type:** game-wide ones (`Game.resetLevel()`), callable from any type through the Manager, as Properties are now.
- **Local values:** temporary number, text, and toggle values inside one Event or function (generated as local `var`s), for
  calculations that don't belong in a Property.

## Kubriko Traits

A Traits section in the Actor type editor, each a toggle or a few fields with a sentence about what it does, generated into the
interfaces and overrides the examples write by hand. The plugins they need come and go with them (`PluginManagers`).

| Trait | In Creator |
|---|---|
| `Unique` | "Only one at a time": adding another removes the first. The Scene editor warns when a Scene has two. |
| `Visible` | Drawing order and layer in the appearance; a "visible" toggle each Actor keeps (and a Block to show or hide it); clip to bounds. |
| `Dynamic.isAlwaysActive` | "Keep moving off-screen": today a generated Actor that leaves the viewport stops updating (Kubriko puts far-away Actors to sleep), which surprises users; decide the default per template. |
| `Identifiable` | A name per placed Actor, set in the Scene editor's inspector, that Blocks can refer to ("the Actor named Player"). |
| `Overlay` | "Draw on the screen": the Actor is drawn in screen space, for HUD pieces that are Actors. |
| `Group` | Compound Actor types: an Actor type made of other Actors placed relative to it (a prefab), added and removed together. |
| `PointerInputAware` | Pointer Events: clicked or touched (this Actor), pressed, released, and moved anywhere. |
| `GamepadInputAware` | Gamepad Events (button pressed and released, sticks) and conditions. |
| `RigidBody` (physics) | A Physics section: shape (from the shape), mass, friction, bounciness, fixed or moving; Blocks for forces and impulses. Collision Events then come from physics. |
| `ParticleEmitter` | Particle effects as an appearance or a Block ("burst of sparks"), with their settings. |
| `Shader` | Scene effects, as Actors placed in a Scene with the built-in shaders' settings. |
| `Disposable`, `Serializable`, `Editable` | Generated as needed; never shown. |

## More for bigger games

- **Acting on many Actors:** "For each Actor of a type" (GameMaker's `with`), running Blocks with that Actor as `other`; "the
  nearest", "a random", and "the one named" Actor of a type as Block targets.
- **List Properties:** lists of numbers or texts (an inventory, a path), with Blocks to add, remove, read, and count; maps later.
- **Keeping Actors between Scenes:** a "persistent" Trait (GameMaker's persistent objects): the Actor survives goToScene
  (`GeneratedScenes` keeps it like Managers' Actors).
- **Saving the game:** Blocks that save and load Manager Properties through the persistence plugin.
- **Creating with values:** addActor that sets the new Actor's Properties.
