<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# actors (the Block model and code generation)

- Generated Kotlin compiles for every combination of Events and Blocks (unknown collision targets are left out, Kotlin Blocks
  get their own `run { }` scope inside a function per kind of Event, so a `return` ends only that Event's Blocks, misplaced
  Blocks and unchosen choices become comments) and follows the engine's no-allocation rules.
- A new Block is its `BlockDefinition` subclass, its `BlockKind` entry (every fact that is the same for each Block of the kind,
  such as `needsActor` and `onlyIn`), and its emission in `BlockCodeGenerator`, which returns its lines with what they need
  (`BlockCode`: the members the class declares, its imports, and the Manager types it reaches), so every lookup, import, and
  Trait of the generated class follows from the code itself. Every `when` over Blocks names each one, so the
  compiler then names every remaining place (references, validation, notation, the editor row, its icon), and
  `theIntegrationProjectUsesEveryBlockAndCondition` fails until `EveryBlockTypes` (whose generated code `GeneratedCodeGoldenTest`
  pins) uses it.
- Flow stays in each Event's flat Block list (if, else, repeat, and end mark sections), so every Block keeps its plain index
  (`BlockNesting` works out the depths).
- It reads like the examples' hand-written code (`Dynamic`, `Collidable`, and Manager lookups only where needed) and behaves like
  a careful game's (`ActorClassCode`): movement steps at most 33 ms at a time, so a hitch can't move a fast Actor through a thin
  one; the remove Blocks set `isRemoved` at once, which `update`, collisions, and moveTowardsNearest skip until Kubriko removes
  the Actor, so a Brick can't score twice; Dynamic types are `isAlwaysActive`, since a Scene can be larger than the screen; edge
  Events use the bounding box, so pivots, rotation, and scale don't move the edges; collision branches put descendants first.
- Timers are two `Long` fields each, counted down in `update`, rather than Kubriko's `Timer`, whose duration is fixed, so starting
  one never allocates; no Property may end like those fields. Manager Properties are Compose state (`mutableFloatStateOf` for
  numbers, so nothing boxes).
- `BlockScope` serves the generator, validation, and the editor. A Manager type is reached through a field named after it
  with `Manager` after it (`BlockScope.managerField`), so no Property may end with `Manager` and no Manager type may be called
  `Viewport`, `KeyboardInput`, or `Sprite`.
- Parent Actor types are real Kotlin inheritance (`ActorFamilyCodeGenerator`): the family's topmost type gets an abstract
  `<Name>Base` with each Event as an `open` hook, and every type a final class that keeps its state and draws itself, since
  Kubriko registers saveable types as `T : Editable<T>`, which a subclass can't implement again. Names ending in `Base` are
  reserved. The topmost type's base class is written by the same `ActorClassEmitter` as a type without a family, so the two
  can't drift, and each generated file imports only what its own classes use.
- `composeKeys` has one entry per Compose `Key` constant, so Creator stops compiling rather than generating code that doesn't
  when Compose drops one.
- `KubrikoWiring.createManagers` creates only the plugin Managers the game uses (`PluginManagers`), so a game that doesn't use a
  plugin doesn't depend on it. Manager types' Events don't run in the Scene editor. A Manager with key Events adds itself as an
  Actor, and `GeneratedScenes` adds it back after each Scene's `removeAll`. Key presses arrive while paused, so Actors' key Events
  return early then and Managers' still run, so a key can resume the game. `GeneratedScenes` ignores a request for the Scene it
  is already loading, so goToScene every frame doesn't restart the load forever.
- `GeneratedAudio` exists only while the game plays audio. It starts music on the next frame, since the music player exists only
  once the viewport is composed, and resumes it when the game is focused again, since `MusicManager` doesn't.
- Sprites are found through `Res.allDrawableResources[accessor]` rather than their accessor, so a deleted image doesn't break the
  build; images in qualified folders aren't offered, since the sprite plugin doesn't scale them back.
- `BlockTypeStore` writes only files whose code changed, never writes over a generated file changed by hand, and deletes the
  generated files of types that no longer exist unless they were changed by hand; the console names both.
