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
`05-blocks.md`.

**Order:** functions, then Traits, then the rest. Each is generated into ordinary Kotlin, as now: no runtime interpreter.

## Parent Actor types

What they still lack (`../CLAUDE.md` → Commands → Parent Actor types):

- **"Only a parent":** a toggle for a type that is never placed (left out of the sidebar drag and the Scene editor's placing) and
  never created by addActor; addActor of a parent otherwise creates the parent itself.
- **Inherited initial values from the editor:** today changing one takes adding a Property of the same name and kind; the inherited
  row should offer it directly, with "back to the parent's".
- **Appearance:** a child inherits its parent's shape, color, size, and sprite until it sets its own (today it always has its own).
- **Deleting a parent** is rejected while it has children; offer instead to copy what they inherit into them.
- **Other Actors' Properties:** once Blocks can read another Actor's Property (`other.hp`), a parent's work for every child, and
  counting Actors of a type includes children.
- **Imports per file** rather than per family (some generated files import what they don't use).

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
| `Dynamic.isAlwaysActive` | "Sleep while far off screen": generated Actors are always active, so a per-type choice for big levels. |
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
