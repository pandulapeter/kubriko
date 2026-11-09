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

- `actors` is the model, `actors.codegen` turns it into Kotlin, `actors.store` reads and writes `.kubriko/`; the model never
  imports the other two (`PackageLayeringTest`), so what both need, such as `GeneratedNames` and `DefinitionPaths`, is model.
- Generated Kotlin compiles for every combination of Events and Blocks (unknown collision targets are left out, Kotlin Blocks
  get their own `run { }` scope inside a function per kind of Event, so a `return` ends only that Event's Blocks, and their
  leading import lines, like Kotlin UI's (`KotlinUi`), become the file's imports; misplaced Blocks and unchosen choices
  become comments) and follows the engine's no-allocation rules.
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
  `<Name>Base` with each Event as an `open` hook, and every type a final class that keeps its state and holds its look, since
  Kubriko registers saveable types as `T : Editable<T>`, which a subclass can't implement again. Names ending in `Base` are
  reserved. The topmost type's base class is written by the same `ActorClassEmitter` as a type without a family, so the two
  can't drift, and each generated file imports only what its own classes use. A regeneration builds each family's scopes and
  features once (`GeneratedFamilies`), since every member's file needs all of them, so a wide family costs its size rather than its square.
- `composeKeys` has one entry per Compose `Key` constant, so Creator stops compiling rather than generating code that doesn't
  when Compose drops one.
- `KubrikoWiring.createManagers` creates only the plugin Managers the game uses (`PluginManagers`), so a game that doesn't use a
  plugin doesn't depend on it. Manager types' Events don't run in the Scene editor. A Manager with key Events adds itself as an
  Actor, and `GeneratedScenes` adds it back after each Scene's `removeAll`. Key presses arrive while paused, so Actors' key Events
  return early then and Managers' still run, so a key can resume the game. `GeneratedScenes` ignores a request for the Scene it
  is already loading, so goToScene every frame doesn't restart the load forever.
- `GeneratedAudio` exists only while the game plays audio. It starts music on the next frame, since the music player exists only
  once the viewport is composed, and resumes it when the game is focused again, since `MusicManager` doesn't.
- Generated Actor types are `Batched`, so the engine draws runs of them in one call: each final class's `look` (its shape in
  its color, or its sprite) is a property whose getter brings it up to date with the Actor (the color, the sprite's region)
  whenever Kubriko reads it, so a color set by a Kotlin Block or the Scene editor shows without a hook in every setter, and a
  family's base class needn't know its children's looks. Every appearance feature to come (sprite sheets and frames, tinting,
  drawing order, layer, the visible toggle) is a property of the look; a type that draws with code of its own (a Kotlin Block
  in a drawing Event) becomes a plain `Visible`, which ends the run it sits in. A circle fills a body that isn't square as an
  ellipse (`EllipseLook`), as an unevenly scaled one always looked, while its collision mask stays a circle as wide as the
  body, since Kubriko has no elliptic mask. `look` is a reserved Property name (format
  version 3 renamed existing ones). `PreviewActor` builds the same look classes, so the Scene editor's preview and the game
  match.
- Every Actor type's sprite is a region of one atlas that `GeneratedSprites` declares (`SpriteManager.loadAtlas`, which splits
  it into pages when it doesn't fit), loaded in the Scene editor too (through Creator's `SpriteManager`), so sprites draw
  nothing until all of them are loaded. A sprite sheet joins it whole (`AnimatedSprite.fromAtlas`). The generated code relies
  on the library's batched API, which is why opted-in projects are raised to Creator's own Kubriko version.
- Sprites are found through `Res.allDrawableResources[accessor]` rather than their accessor, so a deleted image doesn't break the
  build; images in qualified folders aren't offered, since the sprite plugin doesn't scale them back.
- `BlockTypeStore` writes only files whose code changed, never writes over a generated file changed by hand, and deletes the
  generated files of types that no longer exist unless they were changed by hand; the console names both.
