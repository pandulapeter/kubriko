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
  such as `onlyIn`, and the `PlacementProblem`s saying why a Manager type or another Event can't have it), and its emission in `BlockCodeGenerator`, which returns its lines with what they need
  (`BlockCode`: the members the class declares, its imports, and the Manager types it reaches), so every lookup, import, and
  Trait of the generated class follows from the code itself. Every `when` over Blocks names each one, so the
  compiler then names every remaining place (references, validation, notation, the editor row, its icon), and
  `theIntegrationProjectUsesEveryBlockAndCondition` fails until `EveryBlockTypes` (whose generated code `GeneratedCodeGoldenTest`
  pins) uses it.
- Every number a Block takes is a `NumberValue` (a number, a random number between two, a number Property's value, an edge
  of the Scene plus an offset, or a random number between two edges), one
  type every field shares rather than a random variant of each Block, so a new kind of number reaches every Block at once
  (`numberValues` reads them and `withNumberValues` replaces them, and `NumberValueFieldsTest` keeps the two agreeing). A fixed number stays a bare JSON number, so files only
  change where a value is something else. `NumberValueCode` makes each one operand that allocates nothing, and every random
  number comes from the one `KubrikoWiring.random`, so a test can seed the whole game. A Block reads its numbers while none
  of its own locals is in scope (passing them to a member function, as Move in a direction does, or reading the one number
  first, as Move towards the nearest does), so a Property named like a local is still the Property; the function's name is
  reserved (format version 8). A direction, a speed, or an opacity takes no edge, which is a position.
- An edge is the Scene settings' bounds, or the screen's while there are none, as the edge Event decides. Values read it
  through `GeneratedScenes.edge` and `randomBetweenEdges`, which read the settings once per call, so a Scene that finishes
  loading meanwhile can't mix its bounds with the screen's; the edge Event takes one snapshot in the Actor's own code, so a
  `GeneratedScenes` changed by hand (which Creator stops updating) still compiles for games that use no edge values. Neither
  goes through a nullable `SceneOffset`, which would box every frame. An x takes only horizontal edges and a y only vertical
  ones, and fields of whole numbers take none (they count rather than place). An edge is the Scene's even in an Add an Actor
  placing from this Actor: only the other coordinate is relative.
- Flow stays in each Event's flat Block list (if, else, repeat, and end mark sections), so every Block keeps its plain index
  (`BlockNesting` works out the depths).
- It reads like the examples' hand-written code (`Dynamic`, `Collidable`, and Manager lookups only where needed) and behaves like
  a careful game's (`ActorClassCode`): movement steps at most 33 ms at a time, so a hitch can't move a fast Actor through a thin
  one; the remove Blocks set `isRemoved` at once, which `update`, collisions, and moveTowardsNearest skip until Kubriko removes
  the Actor, so a Brick can't score twice; Dynamic types are `isAlwaysActive`, since a Scene can be larger than the screen; edge
  Events use the bounding box, so pivots, rotation, and scale don't move the edges; collision branches put descendants first.
- Staying inside the Scene is the Actor type's setting (`staysInsideScene`), not what the edge Event does (format version 7 turned
  it on for every type with an edge Event and every member of a family with one, which the base class pushed back, so games
  kept behaving the same). The edge Event runs when an edge starts being reached past (never for where the Actor was added),
  so one that moves on doesn't run it every frame; one that stays inside is pushed back each time, so for it the Event runs on
  every frame it reaches past, as before. The leaving Event runs once the Actor is wholly outside after having been inside,
  so an enemy added off screen flies in first, and never for one that stays inside, even when a frame moves it wholly
  outside before the push. In a family whose members differ in staying inside, the base class asks each Actor through an
  `open val` (`SceneContainment.SOME`). What the classes keep about the bounds (`SceneBoundsCode`) are reserved Property names.
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
  family's base class needn't know its children's looks. Every appearance feature (tinting a sprite, and those to come:
  drawing order, layer, the visible toggle) is a property of the look; every Actor's `opacity` (a `Float`
  the opacity Blocks set, reserved by format version 9 and never saved, so each Actor starts opaque) is applied on top of
  its color by the look, to a sprite that isn't tinted too. It isn't the color's alpha, which Compose keeps in 1/255
  steps, so a fade by small steps would be lost, and a translucent color on an untinted sprite would start to show in
  existing games; Set the color keeps the color's alpha; a type that draws with code of its own (a Kotlin Block
  in a drawing Event) becomes a plain `Visible`, which ends the run it sits in. A circle fills a body that isn't square as an
  ellipse (`EllipseLook`), as an unevenly scaled one always looked, while its collision mask stays a circle as wide as the
  body, since Kubriko has no elliptic mask. `look` is a reserved Property name (format
  version 3 renamed existing ones), and so are the animation's members (format version 4, through the same
  `ReservedPropertyMigration`). `PreviewActor` builds the same look classes, so the Scene editor's preview and the game
  match.
- Every Actor type's sprite is a region of one atlas that `GeneratedSprites` declares (`SpriteManager.loadAtlas`, which splits
  it into pages when it doesn't fit), loaded in the Scene editor too (through Creator's `SpriteManager`), so sprites draw
  nothing until all of them are loaded. A sprite sheet joins it whole (`AnimatedSprite.fromAtlas`). The generated code relies
  on the library's batched API, which is why opted-in projects are raised to Creator's own Kubriko version.
- How a sprite is cut into frames is the sprite's, not a type's (`SpriteSheet`, in its `.kubriko/sprites` file), so
  `BlockTypeStore` reads the sheets at every regeneration and counts them in what it compares, and a change to one
  regenerates every type drawn as that sprite. A type drawn as a sheet is `Dynamic`, its update stepping the frames before
  any Event, so a Set the frame Block shows exactly that frame. In a family the base class runs every member's Blocks but
  only a member's final class knows its sprite, so the base class (of any family with a member drawn as a sheet) declares
  the speed and looping fields and `open` no-op hooks (`setFrame`, `stepAnimation`) that such a member overrides; the
  animation Blocks become a comment in a type (or family) with no sheet. A sheet's origin is the type's, not each Actor's:
  the final class sets the body's pivot from it as it is created, so every Actor, placed or added, follows the origin and
  its own size (Creator never sets a pivot by hand, and resizing an Actor puts its saved pivot back in the middle).
- Sprites are found through `Res.allDrawableResources[accessor]` rather than their accessor, so a deleted image doesn't break the
  build; images in qualified folders aren't offered, since the sprite plugin doesn't scale them back.
- `BlockTypeStore` writes only files whose code changed, never writes over a generated file changed by hand, and deletes the
  generated files of types that no longer exist unless they were changed by hand; the console names both.
