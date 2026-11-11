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

**Order:** custom Blocks (on types and in the project, then parameters, results, and local values, then Kotlin bodies, then
sharing between projects), then Traits, then the rest. Each is generated into ordinary Kotlin, as now: no runtime interpreter. Both
build on how code is generated (`:creator:core`'s `actors.codegen`): a Block is one descriptor (`BlockKind`), its code a structured
`BlockCode` that names its imports and features, one emitter writes every Actor class, standalone or in a family
(`ActorClassEmitter`), and each kind of Event is a function of its own (`EventFunctions`). A custom Block is then one more member the
emitter writes, and a Trait one more feature `BlockCode` reports; their commands go into `:creator:commands`.

## Parent Actor types

What parent Actor types (`ActorTypeFamily`, `set_parent`) still lack:

- **"Only a parent":** a toggle for a type that is never placed (left out of the sidebar drag and the Scene editor's placing) and
  never created by addActor; addActor of a parent otherwise creates the parent itself.
- **Inherited initial values from the editor:** today changing one takes adding a Property of the same name and kind; the inherited
  row should offer it directly, with "back to the parent's".
- **Appearance:** a child inherits its parent's shape, color, size, and sprite until it sets its own (today it always has its own).
- **Deleting a parent** is rejected while it has children; offer instead to copy what they inherit into them.
- **Other Actors' Properties:** once Blocks can read another Actor's Property (`other.hp`), a parent's work for every child, and
  counting Actors of a type includes children.

## Custom Blocks

The user's own Blocks: a named list of Blocks with parameters and an optional result, shown in the Palette and on the Event's
card exactly like a built-in one, like GameMaker's scripts. They serve three needs with one feature: a long run of Blocks that
repeats (a "die" sequence of a sound, a burst, a score change, and a removal) becomes one readable Block; behavior Creator has no
Block for is written once, named, and reused instead of pasted as Kotlin Blocks; and the AI extends the Block set for the
project it works in. Each is generated into an ordinary Kotlin function, with no interpreter, so a custom Block costs what the
same Blocks written inline cost.

The ladder the editor, the docs, and the system prompt (`AssistantSystemPrompt`) keep: a built-in Block, then a custom Block the
project already has, then a new custom Block built from Blocks, then a custom Block with a Kotlin body, and a Kotlin Block only for
code that is truly used once. Kotlin stays the last resort, and when it is needed it arrives named, with parameters, and tested
(`12-rehearsals.md`), rather than as a verbatim snippet.

- **Where one lives** decides what it reaches:
  - **On an Actor type:** a method of its Actors, reaching the type's Properties, Timers, and look, inherited by children and
    overridable (the parent's runs through "Run the parent's Block"). Generated on the class (and the family's base class).
  - **On a Manager type:** game-wide (`Game.resetLevel()`), callable from any type through the Manager, as its Properties are now.
  - **In the project:** `.kubriko/blocks/<Name>.json` (Creator-owned, committed), for any Actor type: it acts on the Actor running
    it through what every generated Actor has (position, velocity, size, look, removal, which a generated interface every type
    implements gives it) and on Managers' Properties. What a type keeps for itself reaches it through a parameter or a
    function on that type. Generated into `GeneratedBlocks.kt` as top-level functions with that interface as their receiver.
- **The sentence:** each has one, with its parameters as fields ("Shake the camera by {strength} for {seconds} seconds"), a
  Palette category (one of the built-in ones, or the project's own), an icon, and a description the Palette and the AI's
  catalog show, so a custom Block reads like the rest and the Block card needs no special case.
- **Parameters:** number, text, toggle, key, color, sound, sprite, Actor type, and Scene, each with a default, so dropping one in
  works at once. A number parameter takes the shared value type (`02-blocks.md` → Random values: a number, a random range, a
  Property, an edge), so "Spawn a Drifter at {y}" is called with a random y. Generated as primitive parameters (an Actor type or
  Scene as the generated ID), so no call boxes or allocates.
- **Results:** a number, text, or toggle result, ended by a "Result" Block. A number or text result is one more source of the shared
  value type ("the distance to the Player"), and a toggle result a Condition ("If it is on the ground"), so a check written once
  is used everywhere.
- **Local values:** temporary number, text, and toggle values inside one Event or custom Block (generated as local `var`s), for
  calculations that don't belong in a Property.
- **The body** is Blocks by default, edited in the center like an Event, with the parameters and local values as values its
  Blocks take. A **Kotlin body** is the escape hatch: one function in the project's own sources
  (`src/commonMain/kotlin/<package>/blocks/<Name>.kt`, a row of `../CLAUDE.md` → Files and ownership: Shared, written by its
  command and then the user's), whose signature Creator writes from the parameters and keeps in step on a rename, and which the
  generated call reaches. A `return` in a Kotlin Block ends the custom Block it sits in, as it ends an Event today, so a result
  never changes what an existing Kotlin Block's `return` does.
- **Placement** follows from the body: a custom Block whose Blocks only work in some Events (`other` in a collision, a Manager
  type's texts) carries the union of their `PlacementProblem`s, so the Palette offers it only where it compiles. A custom Block that
  reaches itself through others is rejected, so a beginner never meets a stack overflow; Repeat is the loop.
- **From existing Blocks:** "Turn into a Block" on a selected run of Blocks (the editor's multi-select) moves them into a new custom
  Block, offers the values the user picks as parameters, and offers to replace the same run wherever else it appears, in one undo
  step. "Put the Blocks back" reverses it at one call or every one. Deleting a custom Block that is still called offers that instead.
- **Renames and references** follow as for Properties: a renamed custom Block or parameter changes every call and its Kotlin body
  (`KotlinIdentifiers`), find usages (`01-resources-and-search.md`) lists the calls, and a call to a deleted one shows on its card
  and in the health checks, as a missing choice does.
- **The AI:** commands (`:creator:commands`, each with its tool, validation, and inverse) to create, edit, and delete custom Blocks,
  their parameters, and their bodies; `get_block_catalog` lists the project's own beside the built-in ones; the digest names them
  in one line. The system prompt keeps the ladder above: before adding the same Blocks to a third place it makes a custom Block,
  and when it writes Kotlin it writes a Kotlin body with parameters and a test rather than a Kotlin Block, unless the code is used
  once.
- **Tested:** generator tests for each place one lives, every parameter kind, results as values and Conditions, and a Kotlin body;
  `EveryBlockTypes` calls one of each; `GeneratedCodeGoldenTest` pins them; the allocation rules hold for a call in `update`;
  `EvalSet` gains requests that should end in a custom Block ("make all three enemies explode the same way") and one that
  should end in a Kotlin body rather than a Kotlin Block.

**Sharing between projects:** "Add to my Blocks" copies a project custom Block (and its Kotlin body) into a library in Creator's
settings folder, which every project's Palette offers in a My Blocks category; using one copies it into the project, so a project
never depends on anything outside its folder and builds in IntelliJ as before. Only a custom Block that names nothing of its project
(no type, Manager, sound, sprite, or Scene of its own, which become parameters first) can be added, and each copy remembers the
version it came from, so a newer one in the library is offered as a diff, never applied silently. A shared collection beyond the
user's own (bundled with Creator, or from others) waits for demand (`13-later.md`).

## Kubriko Traits

A Traits section in the Actor type editor, each a toggle or a few fields with a sentence about what it does, generated into the
interfaces and overrides the examples write by hand. The plugins they need come and go with them (`PluginManagers`).

| Trait | In Creator |
|---|---|
| `Unique` | "Only one at a time": adding another removes the first. The Scene editor warns when a Scene has two. |
| `Visible` | Drawing order and layer in the appearance, and Blocks that change them; a "visible" toggle each Actor keeps (and a Block to show or hide it); all of them properties of the generated batched look (`core/.../actors/CLAUDE.md`). Clip to bounds only for a type that draws with code of its own, a plain `Visible`, since a look never reaches past its body. |
| `Dynamic.isAlwaysActive` | "Sleep while far off screen": generated Actors are always active, so a per-type choice for big levels. |
| `Identifiable` | A user-facing name per placed Actor, set in the Scene editor's inspector, that Blocks can refer to ("the Actor named Player"); the Actor's ID stays internal and stable when the name changes. |
| `Overlay` | "Draw on the screen": the Actor is drawn in screen space, for HUD pieces that are Actors. |
| `Group` | Compound Actor types: an Actor type made of other Actors placed relative to it (a prefab), added and removed together. |
| `PointerInputAware` | Pointer Events: clicked or touched (this Actor), pressed, released, and moved anywhere. |
| `GamepadInputAware` | Gamepad Events (button pressed and released, sticks) and conditions. |
| `RigidBody` (physics) | A Physics section: shape (from the shape), mass, friction, bounciness, fixed or moving; Blocks for forces and impulses. Collision Events then come from physics. |
| `ParticleEmitter` | Particle effects as an appearance or a Block ("burst of sparks"), with their settings: a burst with a count, speeds, colors, and a lifetime is one Block, where an explosion takes extra Actor types and Kotlin today. |
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

## Templates

**Platformer** and **top-down** starters (physics or collision, keyboard and gamepad input), built on the Traits above.
