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
  (`numberValues` reads them, and `withValues` replaces every kind of value). A fixed number stays a bare JSON number, so files only
  change where a value is something else. `NumberValueCode` makes each one operand that allocates nothing, and every random
  number comes from the one `KubrikoWiring.random`, so a test can seed the whole game. A Block reads its numbers while none
  of its own locals is in scope (passing them to a member function, as Move in a direction does, or reading the one number
  first, as Move towards the nearest does), so a Property named like a local is still the Property; the function's name is
  reserved (format version 8, and Move with keys' by format version 11). A direction, a speed, an opacity, a scale, or a time takes no edge, which is a position.
- Every color a Block takes is a `ColorValue` (a color, or a color Property's value), as every number is a `NumberValue`:
  a fixed one stays a bare `"#RRGGBB"`, `ColorValueCode` makes each one `Color` operand (an inline class, so nothing
  allocates), and a deleted Property's becomes its starting color. Kubriko saves a `Color` through `SerializableColor`,
  so a Property kind's saved type can differ from its Kotlin type (`PropertyKind.savedType`). A color has no words, so a
  text's or Log Block's placeholder of one stays as written.
- Every value field of every Block and condition, whatever its kind, is reached through one exhaustive `when`
  (`withValues`, with a `ValueTransform` holding a method per kind), and every reader, replacer, rename, and delete of
  values is built on it, so a new field or a new kind of value is added in one place. A kind that is fixed or a
  Property's (colors, Actor types, texts, toggles, sounds, and music) is read and written through
  `FixedOrPropertySerializer`, and edited through `FixedOrPropertyField`.
- A sound or music Property names a file, as an Actor type reference Property names a type, so a file's renames and deletes
  reach its starting value and the values Scenes save for it too; `GeneratedAudio` skips the empty path such a Property
  holds while it names none, and preloads every one's starting file, while one a Scene or a Block sets later loads as it
  first plays.
- A text or toggle a Block sets, or a condition compares with, is a `TextValue` or a `ToggleValue`, so a Property can be
  copied into another or compared with another; a fixed one stays a bare string or boolean, and a deleted Property's
  becomes its starting value.
- The Actor type the Blocks adding an Actor and moving towards the nearest one take is an `ActorTypeValue` (a type by its
  name, still a bare name, or an Actor type reference Property's), so a spawner can keep what it spawns. A Property's
  type is known only while the game runs, so `GeneratedActorTypes` looks it up by name with a `when` over every type
  (`ActorTypeValueCode`), which allocates nothing; the counting condition and collision Events stay fixed, since what
  they need is generated per type, while the distance condition takes either, measuring through the registry
  (`distanceToNearestOf`) with a fixed type's name as a literal, so both go one way. Generated locals that sit beside a Block's values start with an underscore, which no
  Property's name can, so the values still read the Properties without reserving another name.
- An edge is the Scene settings' bounds, or the screen's while there are none, as the edge Event decides. Values read it
  through `GeneratedScenes.edge` and `randomBetweenEdges`, which read the settings once per call, so a Scene that finishes
  loading meanwhile can't mix its bounds with the screen's; the edge Event takes one snapshot in the Actor's own code, so a
  `GeneratedScenes` changed by hand (which Creator stops updating) still compiles for games that use no edge values. Neither
  goes through a nullable `SceneOffset`, which would box every frame. An x takes only horizontal edges and a y only vertical
  ones, and fields of whole numbers take none (they count rather than place). An edge is the Scene's even in an Add an Actor
  placing from this Actor: only the other coordinate is relative.
- Conditions combine with and and or as a condition of their own (`ConditionDefinition.Combined`), so if Blocks and texts
  get it alike; every condition helper recurses into it (`leaves`, `propertyReferences`), and each rule a single
  condition is held to (where it is allowed, a known key, a chance within 0 and 100) holds for every condition it
  combines.
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
- An Animate Block's value (scale, opacity, color, or position) is a few plain fields per value the type animates, started
  through a function (`AnimateCode`, names in `AnimationNaming`, reserved by format version 10), and stepped in `update` before
  the Events, so one an Event starts shows its start on that frame. Each value has one animation at a time, which the next
  Block of it starts over. The position moves by what its easing added since the frame before, so it adds up with the
  velocity instead of fighting it. A Kubriko class an Actor type may be named after that `BlockTypeNaming` doesn't reserve
  yet, such as `Scale`, is named in full rather than imported, which is why `com` is a reserved Property name: reserving
  a type name would need a migration that renames Actor types.
- Move with keys sets the velocity from the keys held, so it works only every frame (`onlyIn`), and its speed is never
  random, which would change on every frame. Its WASD are the letter keys as Compose names them: on the desktop and
  Android that follows the keyboard layout (on AZERTY, W and A steer where those letters are printed, away from the usual cluster), while the web
  (`KeyboardEvent.code`) and iOS (`GCKeyboard`) read the physical keys, so the keyboard plugin decides, not the Block.
- A Manager type's actorAdded and actorRemoved Events are told by the Actors (`ActorWatch`, which both generators read,
  so they agree): `onAdded` and `onRemoved` call the Manager's notify function, which only counts in an atomic array,
  since Kubriko calls them on a thread of its own, and the Manager's `onUpdate` runs the Event once per count (so never
  while paused or in the Scene editor). Removed means removed in any way while its Scene plays, never by a Scene change's
  `removeAll`, which would end every level with "all Bricks are gone": `GeneratedScenes` counts its Scene changes, and an
  Actor compares the count with the one it saw when it was added (`sceneChangesWhenAdded`), since its `onRemoved` comes
  long after the change was asked for. Added includes the Actors a Scene starts with, as GameMaker's create event does. The actorCount condition reads a count `GeneratedScenes` keeps per counted Actor type
  (`ActorCountCode`), so it costs the same however many Actors check it: an Actor adds itself in `onAdded` and takes
  itself away once a Block marks it removed (the `isRemoved` setter) or Kubriko removes it, whichever is first, through a
  guard (`ActorCountGuard`), since the two run on different threads. So in an actorRemoved Event the last Brick counts
  0, and in an actorAdded Event the Actor is counted. The atomics are named in full (importing them would make their
  names reserved type names), which is why `kotlin` is a reserved Property name of Manager types; format version 12
  reserves it with the Managers' counters, `sceneChangesWhenAdded`, and `actorCountGuard`.
- A Manager type's texts are drawn by `ManagerUiCode`. A text shown only while a condition holds (`shownWhen`) takes only
  conditions on Properties, whose numbers are fixed or Properties too (`ScreenTextDefinition.isShownWhenAllowed`), since
  the `Composable` follows Compose state and nothing else: a random number would change at every recomposition, and an
  edge or a count isn't state. Deleting what such a condition checks removes the text, as it removes an if Block with its
  section, unless the condition is an or still meaning something without it, which drops only that part
  (`ConditionDefinition.without`). The font classes are named in full rather than imported, like `Scale`, since reserving
  their names would need a migration renaming types.
- Keys reach a type in two ways (`KeyEventCode`): Kubriko tells a `KeyboardInputAware` about each key pressed and let go,
  which the pressed and released Events dispatch by key, while a held key is asked about in `update` (or a Manager's
  `onUpdate`), so a type with only held keys needs no Trait and, like every frame, never runs while paused. The removed
  Event runs from the setter of `isRemoved`, the first time a Block sets it, on that Block's thread and before Kubriko
  removes the Actor, so its Blocks can still use where it is; a Scene change's `removeAll` sets nothing, so it never runs
  for that.
- What isn't generated needs no plugin: `GeneratedCode` decides which Events and Blocks get code, and both generators,
  `PluginUse`, and `PluginManagers` ask it, so a Block left as a comment (or in an Event left out) adds no plugin.
- Property names the format raises reserved only for what generated Actor classes declare (`look`, the animation's and
  the Animate Block's members, `opacity`, and the movement functions) are free in Manager types
  (`PropertyNaming.validate` takes the kind), so their migrations rename only Actor types' Properties.
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
  Actor, and `GeneratedScenes` adds it back after each Scene's `removeAll`. Key presses arrive while paused, so Actors' key
  pressed Events return early then and Managers' still run, so a key can resume the game; released Events run while paused
  too, so what a press started ends (Kubriko reports a held key released when the game loses the focus too). An Actor
  pairs each release with a press it heard while the game ran (`KeyEventCode.appendPairedKeyDispatches`), so the Scene
  editor, whose game never runs, hears none. `GeneratedScenes`
  ignores a request for the Scene it is already loading, so goToScene every frame doesn't restart the load forever.
- The camera Blocks reach `GeneratedScenes` too (`CameraCode`), rather than a Manager of their own, so they reserve no new
  names. Blocks run on the game's thread and, in an added Event, on Kubriko's, where a removed Actor also stops the camera
  following it, so the camera's state changes only under one short lock. An Actor with camera Blocks is a
  `GeneratedScenes.CameraFollower`, passing itself, whose Scene stamp only it writes and sets to `NOT_IN_SCENE` before it
  lets the camera go, so neither an old Scene's Actor still updating nor a removed one moves the camera again; a new
  Scene is counted under the same lock, which stops the following and the shake. A shake holds
  still while the game isn't running while following goes on only as long as the Actor moves (Actors may be told to keep
  updating then, and a game may move the camera itself during a pause), a shake shakes around wherever something else moved
  the camera, and the zoom is the viewport's own, kept across Scenes until another Zoom the camera Block or a Scene fitting
  the view, which shows its whole bounds; a Scene's settings are added before its other Actors, so its fit comes before
  their added Events. `GeneratedScenes` keeps the view's aspect ratio from before a
  Scene fitted it (`fitsView`), which a Scene that doesn't fit the view gives back; a Scene's settings may be added before
  the last Scene's are removed, so an ending Scene unfits the view only while its settings are still the current ones.
- The size and scale Blocks call functions of the Actor's class (`SizeCode`) whose names, and the fields they keep, start
  with an underscore, which no Property's can, so they reserve no names. Set the size keeps the pivot at the same fraction
  of the size, so an Actor grows around its middle or its sprite's origin; Change the scale adds to each axis, so a placed
  Actor keeps its proportions. The collision mask follows the size and the scale in the update (`appendCollisionMaskUpdate`:
  a box's `size`, a circle's `radius`, both changed in place, since the collision plugin's grids hold the mask), so a
  collidable Actor whose Blocks may change either (a Kotlin Block too) is Dynamic even if nothing else makes it so. A box's or
  oval's size is followed only where it can change (`followsMaskSize`), and on a Kubriko older than 0.8.1, which can't
  resize a mask in place, only for the size and scale Blocks (`SizeCode.hasResizableMasks`), so such a project still builds.
- `GeneratedAudio` exists only while the game plays audio. It starts music on the next frame, since the music player exists only
  once the viewport is composed, and resumes it when the game is focused again, since `MusicManager` doesn't. A
  volume is a percent on the Block and a fraction in the call, left out at 100 so games using none generate what they
  did before (and a `GeneratedAudio` changed by hand still compiles), and `GeneratedAudio`'s `playSound` takes a volume
  unless the project's Kubriko predates it (0.8.0) and no Block plays a sound at less than full volume
  (`AudioCodeGenerator.takesSoundVolume`), so such a game still builds while hand-written code calling it with a volume
  keeps compiling elsewhere; the music's is set only when it changes, so playing
  it every frame allocates nothing, and a Scene's music plays at full volume.
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
  ellipse (`EllipseLook`), as an unevenly scaled one always looked, and collides as one (`CollisionMaskCode`): a round
  type that isn't square, or that the size Block or Kotlin may stretch, has an oval mask, and a square one picks the oval
  per Actor as it is created, only for one placed stretched, so most round Actors keep the exact, cheaper circle. The oval
  is named in full, since an Actor type may be called `EllipseCollisionMask`, and a Kubriko before 0.8.1, which has none,
  keeps every round Actor a circle as wide as its body unless the size Block needs an oval anyway. `look` is a reserved Property name (format
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
