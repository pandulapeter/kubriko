<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# creator (Kubriko Creator)

A standalone desktop app for making Kubriko games without IntelliJ, inspired by GameMaker 6.1: Actor types and Manager types are built from
Events and Blocks, Actors are placed in Scenes, and the game is played with one click. This file describes what exists; what is planned
lives in `plan/` (start with `plan/README.md`).

## Running and testing

- Run against this repository's libraries: `./gradlew :creator:run`, however it is started (the `[Desktop] Kubriko Creator` run
  configuration spells it out). `settings.gradle.kts` turns on `creator.localKubriko` for any build that runs `:creator:run` and puts the
  three Maven Local publish tasks first. `creator.localKubriko` publishes the libraries to Maven Local as `<library.version>-creator` (so
  they never shadow a real release) and bundles that version and `mavenLocal()` into `creator.properties`, so projects use them. Packaged
  builds, and `:creator:run -Pcreator.localKubriko=false`, bundle the released version instead.
- `./gradlew :creator:test` runs the unit tests (also in the `Tests` workflow). `KUBRIKO_CREATOR_INTEGRATION=true KUBRIKO_CREATOR_VERSION=<version>`
  additionally generates every template for real and compiles it, plus an Actor type and two Manager types using every Event and Block (and
  each other's Properties), for desktop and web, and resolves the desktop runtime classpath as playing does (compiling alone misses a
  library whose repository is missing).
- `KUBRIKO_CREATOR_BENCHMARK=true` times a 200-type project without Gradle (`CreatorBenchmarkTest`) and, with
  `KUBRIKO_CREATOR_VERSION` as above, builds it for real (`CreatorBuildBenchmarkTest`, `--no-build-cache`, more Gradle arguments
  through `KUBRIKO_CREATOR_BENCHMARK_ARGUMENTS`), printing each step's time, generated files touched, and classes compiled again.
- **Every change ships with solid unit tests**: controllers against fakes (`FakeProjectBuild`, `FakeSessionChannel`, `FakeBlockTypeRepository`,
  `FakeGitRepository`), file code against real temporary folders, git against real temporary repositories (JGit needs no installed git),
  sockets over real localhost connections. `StringResourcesTest` fails on any string or icon nothing uses, and on escaped apostrophes.
- UI changes are checked with a window-only screenshot (`screencapture -l <window id>`) before they are called done.

## Processes

- **Opening** a project never waits for Gradle (`GradleWorkspaceOpener`): it only checks for a build or settings file and reads the
  root project's name from the settings (`ProjectInspector`); the desktop module is asked from Gradle in the background (the
  `GradleProject` model configures every task), and the first build waits for it, reporting a project without one in the console.
  The Tooling API connection is made on first use, which is right away: the workspace builds the classpath and loads the
  project's classes in the background as soon as it opens (`SceneEditingController.prepare`), so the Gradle daemon is warm
  and the first Scene opens without a build. Once they are loaded, the init script's `kubrikoCreatorWarmUp` compiles one
  generated file in a compilation of its own next to the desktop target's code (`WorkspaceController.warmUpCompiler`, silent,
  never clean, and not a failed build when it fails), since the build at opening starts Kotlin's compile daemon only when
  something needed compiling, and a cold daemon made the first change seconds slower. Builds of the classpath never overlap, and a play session waits for the one
  running (`classpathBuildLock`), since a build started while another runs needs a daemon of its own; a cancelled build
  returns only once Gradle has stopped it, for the same reason. **Playing** builds the classpath too (into a file of its own,
  `SessionInitScript.playClasspathFileOf`, so the Scene editor's builds don't touch it; a fraction of a second when nothing
  changed), whose `launch.` lines say how `kubrikoCreatorSession` would start the game (`GameLaunch`: the `run` task's Java
  and JVM arguments, its working folder and main class, and the module's own files followed by the session library's own
  resolution, which may raise library versions), and Creator starts that process itself (`GameProcess`), so no Gradle
  daemon is held while the game runs. The game runs from a copy of the project's class files (in `playDirectoryOf`,
  deleted when it ends), since the Scene editor rebuilds the classes meanwhile; resources are read where they are. A game
  that exits with an error shows its exit code and last lines in the console; stopping it destroys the process. Only a
  build without `launch.` lines falls back to the session task. **Hot reload:** play builds pass `kubrikoCreatorHotReload`,
  and the classpath task then also writes `launch.hot` lines (`HotReloadLaunch`) from Compose Hot Reload's run task, which
  Compose Multiplatform gives every project: its JetBrains Runtime, its `@argfile` (`JavaArgumentFile` reads it), its
  reload task, and the session libraries. Such a game starts with Hot Reload's agent and DevTools off, from Hot Reload's
  own copy of the classes (so without Creator's). While it plays, every type, source, or Scene change builds the project
  and `GameReloader` (in `GameBuilds`, which also prepares launches) compares the built classes' `ClassShape`s with the
  game's: only code changed runs the reload task, while a changed superclass, interface, instance field, or class with
  static state (which a reload would leave null or reset) restarts the game from the same Scene. A game waits for the one
  before it to end, since Hot Reload's pid file is shared. Play from this Scene (the Scene editor's header, and
  `start_game`'s `scene`) passes `kubriko.creator.scene`, which template version 7's `Main.kt` hands to
  `KubrikoWiring.firstSceneOverride`; an older project gets a console note that it may start with its first Scene. Each workspace runs in a scope of its own (a child of the app's), which
  `close()` cancels once the pending writes are done (`BlockTypesController.awaitWrites`), and quitting waits for that; an
  opening that is cancelled closes what it made. Builds of the classpath report their time in the console, and so does opening
  a Scene.
- **Creator** works on files and Gradle only. It drives the project's own build through the Gradle Tooling API (`gradle/GradleProjectConnection`)
  with `src/main/resources/kubriko-creator-session.init.gradle.kts`, which adds three tasks to every Compose desktop module without touching the
  project: `kubrikoCreatorSession` (runs the module's classpath plus `tool-creator-session` at the project's Kubriko version, in `play` or `edit`
  mode, reporting back over a versioned JSON-lines protocol on a localhost port, `SessionServer`), `kubrikoCreatorWarmUp` (above), and `kubrikoCreatorClasspath` (compiles and
  writes the Kubriko version and runtime classpath to a file, with the folders the module's jar is packed from in place of the jar,
  found through the jar task's `source`, so the jar isn't packed; a file the jar puts elsewhere than its folder would is
  copied to `build/kubrikoCreator/moved`). Every build's arguments come from `SessionInitScript.gradleArguments`; the script and the classpath file keep their paths
  (`kubriko-creator` in the temporary folder: the script named after its content, the classpath file after the project's path),
  since Gradle reuses a configuration cache only for the same init scripts and `-P` values, and the script works with the
  configuration cache, which projects turn on themselves (template version 6). How it
  compiles from `IncrementalCompilationPolicy`: common code compiles incrementally through Kotlin's internal
  `kotlin.internal.incremental.enableUnsafeOptimizationsForMultiplatform` (KT-62686: otherwise any change to `commonMain`, where generated code
  lives, compiles everything again), except in projects whose sources declare an `expect` (`ExpectDeclarations`), and the build after a failed
  one, a Reload, or classes that couldn't be loaded runs with `--rerun-tasks`, from scratch. Projects' own builds keep Kotlin's default.
- **Scene editing** (`scenes/`, `ui/scenes/`): Creator's own Scene editor, never a window of its own and not the library's
  `tools/scene-editor`. When the project's Kubriko version equals Creator's, Creator loads the project's classes into a class
  loader whose parent is its own (Kubriko, Compose, and Kotlin shared; project jars copied and the class files of project folders read first, `ProjectClassLoader`, so rebuilds can replace them), finds the
  project's `CreatorEntryPoint` through `ServiceLoader`, and `SceneView` creates the Scene's Actors from the file's states with the
  project's `SerializationManager` and Managers in a Kubriko instance of its own that never runs (one Actor per entry, kept by
  the Actor's ID), drawn by `KubrikoViewport` in the center pane (`SceneCanvas`). The header's Actors button shows the Scene's
  Actors in a list beside it (`SceneActorList`: in their order, by type and ID, filtered by what is typed, lazily composed),
  where a click selects one and centers the camera on it (`SceneView.centerOn`). A click selects the Actor on top (`SceneHitTest`:
  lowest drawing order, then the later one; the Scene settings only where nothing else is), looking only at the Actors near
  it (`SceneBoundsGrid`, a uniform grid of their bounds, updated in place when Actors only move or change and built again on
  the next click after Actors are added, removed, or reordered), dragging it moves it live and dispatches
  `move_actor` when let go, dragging elsewhere or scrolling pans, Ctrl/Cmd+scroll and the header zoom (`SceneFraming` shows the
  Scene settings' bounds, or every Actor), Delete or Backspace dispatches `remove_actor`, and an Actor type dragged from the sidebar
  (`SidebarDragState`, only while a Scene is open) is placed where it is dropped (`place_actor`). The inspector beside it
  (`SceneInspector`) shows the selected Actor's position, size, rotation in degrees, and scale (`ActorBodyValues`, read from the live
  Actor, so defaults its state leaves out show), and for block-built types its color and Property values (the type's where the
  state has none), set through `set_actor_values`; the Scene settings through `set_scene_settings`; with nothing selected, a hint
  and a way to the Scene settings (added if the Scene has none). Every change is a command, so it
  is undone like any other, and after each the Scene's Actors are read again from `SceneStore` (which parses the Scene in the
  background while the project builds, so the first command doesn't parse it on the main thread) and `SceneView.load` applies only what changed
  (`ScenePatch`: unchanged Actors stay, one whose state differs only in position is moved, a changed one is created again,
  new ones at the end are added, removed ones go; a new order creates the whole Scene again), creating Actors
  off the main thread. A project on another Kubriko version
  gets a notice instead (`SceneEditingState.Unsupported`), and so does a Scene whose project didn't build or whose entry point
  couldn't be loaded (`SceneEditingState.Failed`, with Try again), rather than the pane just closing. Compose resources are read
  through Creator's own class loader there, which can't see the project's, so `SpriteLoadingEntryPoint` swaps the project's
  `SpriteManager` for one whose `imageLoader` (`ProjectSpriteLoader`) reads the sprite files from disk, matching resources by ID
  (`drawable:<accessor>`, which is what Compose resources are equal by). The loaded classes (`loading`, the build and
  loading of the current sources, which every Scene opened meanwhile waits for) are kept when the Scene
  closes (selecting an Actor type, or another Scene), so the next Scene opens without building, until a type or a source
  changes (`markOutdated`, also during that build), Reload, or the workspace closes; a failed build or another Kubriko
  version is shown by the Scenes waiting for it, and the next Scene builds again; released classes go a few seconds after their editor leaves
  composition. A change while a Scene is open builds it again in the background (`update`, the header says "Updating…"):
  the new classes are swapped in when they are ready, and a change during that build builds once more after it, rather than
  cancelling it. The editor's new `SceneView` creates its Actors while the old one is still shown and then takes over its
  camera (`rememberSceneView`, `SceneCamera`), and the selection is kept per Scene, so a swap only changes the Actors. A
  failed build keeps the old classes and says so in the header, with Reload, until a build works. Until newer classes are
  swapped in, the Actors of block-built types whose look (`ActorTypeLook`: shape, color, size, sprite) changed since the
  shown classes were built, or that are new, are drawn by Creator itself from that look and their saved state
  (`SceneEditingState.Open.previews`, `PreviewActor`): every build records the looks it compiled once the changes are
  written, and the previews follow the types in memory (`onActorTypesChanged`), so a change shows before it is even
  written. The Scene editor's instance ticks without the focus (`shouldPauseOnFocusLoss = false`), since a Scene swapped
  in while Creator is in the background would otherwise draw nothing until it is focused. Creator no longer starts `tool-creator-session` in Edit mode.
- **Kubriko dependencies** (`project/KubrikoBuildFiles`): after every regeneration of an opted-in project's code (so also when it is opened,
  and when it is created), Creator makes its build depend on exactly the Kubriko libraries its sources name (`KubrikoArtifact`: the package
  after `com.pandulapeter.kubriko.` in imports and fully qualified names, generated code included; the engine and `tool-creator-api` always),
  adding `kubriko-<name>` catalog entries and `implementation(libs.kubriko.<name>)` lines in sorted order and removing unused ones (a
  catalog entry stays while anything else in the build file refers to it; libraries it doesn't know, like the debug menu, are never
  touched). It also raises the catalog's `kubriko` version to Creator's own (one newer than Creator's is kept and reported once), and adds
  or removes its Maven Local block in `settings.gradle.kts` with `useMavenLocal`, and a JitPack block (limited to
  `com.github.umjammer`) with the audio plugin, whose desktop library needs JLayer from there. The edits are line-based (`VersionCatalog`,
  `BuildScript`) and leave every other line alone; files without the template's shape (a `kubriko` version, a `commonMain.dependencies`
  block) are left alone entirely. Changes are reported in the console. Projects that aren't opted in are never changed.

## Commands

Every change to a project goes through a command (`commands/`), so the user interface, undo and redo, and the AI share one
path. `CommandRegistry.editor` lists them all; the AI tool list (`ToolDefinition`: name, description, input schema) is generated
from it, each input schema from the input class's serial descriptor (`JsonSchema`, with `@Description` on every property).

- **Kinds:** `ReadCommand` (reports, never recorded), `WriteCommand` (`validate` returns a `Rejection` with a user-facing
  `StringResource`; `apply` returns `Applied` with the inverse `Invocation`, or none for git operations, which can't be undone),
  and `ActionCommand` (build, play; run outside the change lock).
- **`CommandExecutor`** (one per workspace): applies changes one at a time, keeps the undo and redo history (consecutive changes
  with the same `mergeKey`, like typing into one field, are one step; a step that no longer validates is dropped), and decodes
  AI tool calls (`call(name, JsonObject)`). UI callbacks use `dispatch`, which starts undispatched, so in-memory changes show
  before it returns. Rejections of dispatched changes and of undo steps go to the console.
- **Enforced by tests** (`CommandRegistryTest`): every `EditorCommand` class is registered, tool names are snake_case, every
  part of every schema is described, and nothing outside `commands/` opts in to `@EditorMutation`, the `RequiresOptIn` marker on
  every controller and store function that changes the project. A new mutation is marked, so only a command can call it.
- **Addressing:** Actor types, Manager types, and Scenes by name, Events, Blocks, and texts by index, and Scene Actors by their ID. The Event, Block, and
  Property commands take either kind of type (their `actorType` input names an Actor type or a Manager type). `commands/CommandTesting.kt`
  (`assertRoundTrip`) checks that a command's inverse reverts it and the inverse of the inverse makes it again.
- **Properties** (number, text, toggle) are values each Actor keeps: generated as `@Exposed` fields kept in `State`, so the
  Scene editor edits them per Actor, changed by the setNumber / changeNumber / setText / setToggle Blocks, and shown by Log
  Blocks as `{name}` (whole numbers without ".0"). A Block may leave its Property unchosen and generates a comment until it
  names one of its kind. Scenes keep the values: renaming moves them (and the Blocks and placeholders), removing keeps them
  (loading ignores them, and undo brings them back), and adding drops a leftover value of another kind, which wouldn't load.
  A Property's kind can't change.
- **Flow** stays in each Event's flat Block list, as GameMaker's actions do: if (with a condition: a Property, a key held,
  the velocity, a chance, or whether the game is paused), else, repeat, and end Blocks mark sections, so every Block keeps its plain index. `BlockNesting`
  works out each Block's depth for the editor's indentation and the generator alike; an if or repeat without an end runs to
  the end of the Event, and an else or end that belongs to no section shows a warning and generates a comment.
- **Keys** (`KeyChoice`): key pressed Events and held-key conditions take any key Compose knows on every platform
  (`composeKeys`, one entry per `Key` constant, so Creator stops compiling rather than generating code that doesn't when
  Compose drops one), saved by the constant's name (`Spacebar`; older projects' `space`, and any case, are read too). A key
  that isn't one is rejected and left out of the generated code. `KeyChoiceButton` opens them all, common ones first,
  filtered by what is typed (`matchingKeys`), or chosen by pressing one.
- **Timers** are named, as many as needed, like GameMaker's alarms but not capped: the startTimer (seconds, once or repeating)
  and stopTimer Blocks, and a timerFinished Event per Timer, each naming it (`TimerNaming`: a lowercase identifier). A Timer
  exists once a Block or an Event names it; an Actor type shares its Timers with its whole family, so a child's Block can start
  its parent's (`BlockScope.timerNames`). The editor's Timer menu offers the Timers in use and names a new one in place, and the
  Event's menu renames one (`rename_timer`, through the whole family). Older projects' numbers are read as `timer1` to `timer8`
  (`TimerNameSerializer`), which keeps their generated fields and hooks. Generated as two `Long` fields per Timer that is used
  (`<name>RemainingInMilliseconds` and `<name>PeriodInMilliseconds`, which no Property may end like), counted down at the start
  of `update`, rather than Kubriko's `Timer`, whose duration is fixed, so starting one never allocates.
- **Other Actor types** in Blocks: addActor spawns one through its own `metadata.instantiate(position)` (so with its size
  and initial Property values, at a position relative to the Actor or the Scene), and moveTowardsNearest aims the velocity at
  the closest Actor of a type (a loop over `allActors` by index, comparing squared distances, skipping removed Actors). Like collision Events, they follow renames, are rejected for
  types that don't exist, and generate a comment for a type deleted later.
- **Manager types** (`ManagerTypeDefinition`, in `.kubriko/managers`) hold game-wide state, like the score: Properties, Events
  (gameStarted, everyFrame, keyPressed, timerFinished), and texts shown on top of the game (`ScreenTextDefinition`: placeholders,
  one of nine places, size, color). Both kinds are a `BlockTypeDefinition` and share one namespace, the edits (`BlockTypeEdits.kt`),
  the Block editor, and the commands; Blocks and conditions that need an Actor (`BlockKind.needsActor`, the velocity condition) and
  Actor-only Events are rejected in a Manager and hidden from its palette. Each one becomes a `Manager` subclass whose Properties are
  Compose state (`mutableFloatStateOf` for numbers, so nothing boxes), whose texts are `BasicText`s in its `Composable`, followed by
  its Kotlin UI (`kotlinUi`, `set_kotlin_ui`, edited in a dialog): verbatim Compose statements whose leading import lines become the
  file's imports (`KotlinUi`), with other Manager types, `actorManager`, `viewportManager`, and `stateManager` looked up only when
  the code names them. The Manager is
  `KeyboardInputAware` and adds itself as an Actor when it has key pressed Events (as the examples' Managers do; `GeneratedScenes`
  adds it back after each Scene's `removeAll`). Its
  Events don't run in the Scene editor (`isInSceneEditor`), nor every frame while the game is paused. The generated
  `KubrikoWiring.createManagers(isInSceneEditor, firstScene)` creates all of them, plus `GeneratedScenes` and the plugin Managers the
  game uses (`PluginManagers`: `CollisionManager` when a collision Event involves an Actor type, `KeyboardInputManager` for key Events
  and held-key conditions, `MusicManager` and `SoundManager` when the game plays audio; not in the Scene editor, which has its own;
  and `SpriteManager` when an Actor type is drawn as a sprite, also in the Scene editor, which has none); the template passes it to `Kubriko.newInstance` in `Game.kt`
  (with the first Scene) and to the Scene editor in `GameEntryPoint.kt`.
- **Scenes and pausing** in Blocks: goToScene and restartScene go through the generated `GeneratedScenes` Manager
  (`ScenesCodeGenerator`, written into every project), which reads `files/scenes/<name>.json` from the project's resources in
  the background, then removes every Actor and adds the Scene's (the first Scene, loaded when the game starts, keeps what the
  Managers added before it) and logs a Scene it can't read. Manager types keep their Properties. goToScene is rejected for a
  Scene that doesn't exist (`BlockScope.sceneNames`, known to commands and the editor but not to the generator) and generates a
  comment until one is chosen. pauseGame pauses, resumes, or toggles through `StateManager.updateIsRunning`; key presses still
  arrive while paused, so Actors' key pressed Events return early then (as Wallbreaker's `Ball` does) and Managers' still run, so a
  Manager's key can resume the game.
- **Audio** in Blocks: playSound, playMusic (looping or once), and stopMusic, with files named by their path from `composeResources`
  (`files/sounds/hit.wav`, as `Res.getUri` takes it; `AudioFiles` lists the project's, only those under `files`, and a Block naming
  one that doesn't exist is rejected, an unchosen one generates a comment). They go through the generated `GeneratedAudio` Manager
  (`AudioCodeGenerator`), written (and in `KubrikoWiring`, with the plugin's Managers) only while the game plays audio
  (`PluginManagers.usesAudio`: an audio Block, or a Scene whose settings have music), and deleted otherwise, so the audio plugin
  comes and goes with it. It looks the URIs of every file it plays up once and preloads them, plays nothing in the Scene editor,
  starts music on the next frame (the music player only exists once the viewport is composed), stops the previous music for
  new music, and resumes the music when the game is focused again, since `MusicManager` pauses it but doesn't resume it. Because a
  Scene's music decides whether the game plays audio, `BlockTypeStore` reads the music of every Scene in
  `composeResources/files/scenes` next to the source folder, and Scene changes (on disk or by a command) regenerate the code;
  regenerating leaves a file whose content didn't change untouched.
- **Sprites** as an appearance: an Actor type's `sprite` (`set_appearance`) names a bitmap right in a `drawable` folder of
  `composeResources` by its path (`drawable/ball.png`; `SpriteFiles` lists them, only those whose names make a resource accessor;
  qualified folders are left out, since the sprite plugin doesn't scale them back). It is drawn stretched over the body with
  `FilterQuality.None`, and the shape is still the collision mask's (the color is hidden then, and unused). The generated code finds
  it through `Res.allDrawableResources[accessor]` rather than its accessor, so a deleted image doesn't break the build, and reaches
  `SpriteManager` in `onAdded`. Choosing a sprite sizes the Actor type to the image (`SpriteFiles.sizeOf`), unless a size is given;
  the inverse always carries the old size, so undo never resizes.
- **Scene settings** are an Actor, not a separate file: the generated `SceneSettings` (`SceneSettingsCodeGenerator`,
  written into every project and always in `GeneratedActorTypes`) is `Unique`, `Editable`, and placed like any other Actor. Its body
  is the Scene's bounds, which it fills with its background color behind every other Actor (`drawingOrder` at the maximum) and which
  edge Events use instead of the screen's edges (through `GeneratedScenes.settings`, which it sets while it is in the Scene); a
  Scene without one keeps the screen's edges. Its camera start moves the camera when it is added, except in the Scene editor, and
  its music (a path, empty for none) plays when the Scene starts, unless it is the music playing already; a Scene without music
  leaves the music alone. The
  view stays `Dynamic`, so a window of another size shows more or less around the bounds. `set_scene_settings` changes it (adding one
  at the end of the Scene if it has none), editing its state as JSON (`SceneSettingsState`; colors are saved as Compose's packed
  value); otherwise it is placed and edited in the Scene editor, from its palette and inspector.
- **Reaching a Manager:** the Property Blocks and conditions have an optional `manager`, and Log Blocks and texts show `{Manager.name}`
  placeholders (`PropertyPlaceholders`). Kotlin Blocks and Kotlin UI follow renames too (`KotlinIdentifiers`: identifiers outside
  strings and comments, string templates included; an own Property bare or after `this.`, another Manager's after its field, type names
  bare), though, knowing no grammar, a local variable of the same name is renamed with them. `BlockScope` is what a type's Blocks can refer to, for the generator, validation, and the
  editor's Property choices alike: an Actor looks the Managers it uses up in `onAdded` (a field named after the type, unless a
  Property has that name), a Manager through `manager<T>()`, and a Manager naming itself means its own Properties. Renaming a
  Manager type or one of its Properties follows every Block, placeholder, and text of every type.
- **Template upgrades** (`template/ProjectUpgrader`): the template files belong to the user, so `.kubriko/project.json` records the
  `templateVersion` they have. When it is older than Creator needs, the workspace shows a notice, and its dialog shows each change as a
  diff (`LineDiff`), offers to commit first, and applies it (`upgrade_project`; `get_project_upgrade` for the AI) only to a file that
  still looks like the template's. Files changed too much are listed with what to add by hand, and a file that already uses
  `KubrikoWiring` counts as done, however it got there. Version 2 is the Manager wiring; version 3 drops the template's `SceneLoader`
  for `createManagers(firstScene = …)` and deletes `SceneLoader.kt` if it is still the template's (`resources/upgrades` keeps that
  copy; one changed by hand stays). Version 4 drops the plugin Managers from `Game.kt` (and their imports, unless still used),
  since `KubrikoWiring` creates them, so a game that doesn't use a plugin doesn't depend on it. Version 5 has the desktop `Main.kt`
  set `KubrikoWiring.isLoggingEnabled` from the `kubriko.creator.logging` system property, which only the play session sets (the
  init script), so Log Blocks (`if (KubrikoWiring.isLoggingEnabled) Logger.log(...)`) cost nothing in a released game. Version 6 turns Gradle's
  configuration cache and build cache on in `gradle.properties` (keeping a property set to any value already, and creating
  the file if there is none). Version 7 has the desktop `Main.kt` also set `KubrikoWiring.firstSceneOverride` from the
  `kubriko.creator.scene` system property (null in a released game), which `createManagers` loads instead of `firstScene`,
  for playing from a Scene. A change's `after` is null for a file it deletes.
- **Parent Actor types** (`ActorTypeFamily`, `set_parent`): an Actor type may name a `parent`, like GameMaker's parent objects
  (cycles and a parent named nowhere count as none). A child gets its ancestors' Properties (one of its own with the same name
  and kind only changes the initial value; another kind is rejected) and Events (one of its own of the same kind, key, other
  Actor type, or Timer replaces the parent's; the runParentEvent Block runs the parent's there), and a parent stands for its
  children in collision Events and moveTowardsNearest. Its editor shows the parent menu, the inherited Properties, and the
  inherited Events greyed, each with a button that adds its own replacement (starting with runParentEvent). A parent with
  children can't be deleted; renames follow. Names ending in `Base` are reserved. **Generated as real Kotlin inheritance**
  (`ActorFamilyCodeGenerator`): the topmost type of a family gets an abstract `<Name>Base` with everything the family needs
  (`ActorTypeFeatures` over the whole family: fields, lookups, Timers, update, key and collision dispatch) and each Event as an
  `open` hook (`onEveryFrame()`, `onCollisionWithWall(other: Wall)`, ...) holding its own code; a type with a parent and children
  gets a base class of its own that overrides its Events' hooks; and every type gets a final class that keeps its state, draws
  itself, and passes its collision mask to the base (so a child may have another shape). Kubriko registers saveable types as
  `T : Editable<T>`, which a subclass can't implement again, hence the split. Other types name a family by its base class
  (`BlockScope.classOf`). Imports cover the whole family, so some may go unused in one file.
- **Folders** (`commands/folders`): Actor types, Manager types, and Scenes live in folders, which are real directories:
  a type in `.kubriko/actors/enemies` (a type's `folder` is where its file is, kept out of the JSON; names stay unique across
  folders, since generated code stays flat in one package), and `files/scenes/levels/1-1.json`, whose Scene is named by its path
  (`levels/1-1`, `SceneStore.nameOf`; goToScene and every Scene command take that path). `create_folder`, `rename_folder` (moving
  everything in it, Scenes through renames), and `delete_folder` (only an empty one, so undo is exact) take a group
  (`FolderGroup`), `move_to_folder` moves a type, and `rename_scene` renames or moves a Scene: goToScene Blocks follow, the open
  Scene follows, and the console names hand-written Kotlin that still names the old Scene (like the template's `Game.kt`). The
  sidebar shows the three groups as trees (`folderTreeSection`, `folderTreeRows`): folders first, each with a menu (new folder,
  rename, delete when empty), and types and Scenes dragged into a folder or onto the group's header (`SidebarDragState`, which
  also carries Actor types into the open Scene). Clicking the selected type or Scene again, or empty space in the
  sidebar, selects nothing (`onSelectionCleared`: the open Scene closes and the workspace hint shows). Scene folder names follow Scene name rules (no spaces). The workspace keeps the
  focus when nothing in it is focused (`historyShortcutTarget`), so Cmd/Ctrl+Z always reaches the undo history.
- **Scenes** are edited as files (`scenes/SceneStore`, `SceneDocument`), written with one Actor per line so they diff and merge,
  and kept parsed in memory while the file's length and modification time are the ones it was read or written with:
  Actors are `typeId` + JSON state + `id`, which Kubriko's serialization ignores (it reads entries with `ignoreUnknownKeys`), so
  games load the files unchanged. Parsing gives an Actor without an ID, or with an earlier one's, an ID from its index (`i3`), the
  same every time until a write keeps it, so existing Scenes change only when a command writes them; new Actors get a short random
  one (`SceneDocument.newId`), or the one `place_actor`, `restore_actor`, or new Scene settings are given (how the Scene editor
  selects what it just placed, and how undoing a removal brings the same Actor back). Positions live in
  `body.position`, and placing works for block-built Actor types only (their state is known without loading classes). Renaming an
  Actor type renames its Actors in every Scene. A command that changes the open Scene shows it again (or closes it if deleted); a
  changed Actor type marks it outdated, since its classes must be compiled again, but regenerating code for changed Scenes doesn't.
  `set_actor_values` changes one Actor's body (a new size moves the pivot to the middle), color, and Property values
  (`ActorStateEdits`; colors as Compose's packed value, rotations in radians) and is undone by `set_actor_state`, which replaces
  a whole state (and is the AI's way to hand-written types' other values). `move_actor_order` moves an Actor in the Scene's list, the
  order Actors are added in (so, of two with the same drawing order, the later is drawn on top); the inspector's header sends
  the selected one to the start or the end.
- **At scale:** `read_scene` returns a page (`offset`, `count`, 100 by default; `total` and `nextOffset`), narrowed to an Actor
  type (not its children) or to a rectangle of positions, or a summary (Actors per type and the bounds of their positions); the raw
  `content` only comes with a page holding the whole Scene. `list_actor_types` pages the same way, narrowed to a folder and its
  folders, and lists the folders (`folders`, empty ones too). The batch commands are each one undo step: `place_actors`,
  `remove_actors`, and `restore_actors` (inserted one after another, so its inverse from `remove_actors` lists the original indices
  ascending), `set_actors_values` (`ActorValueChanges` per Actor, validated and applied as `set_actor_values` does), and
  `set_actors_state`. A batch names no Actor twice and at least one.

## AI connection (MCP)

`mcp/McpToolServer` serves `CommandRegistry.editor.tools` as MCP tools over Streamable HTTP (Ktor CIO, the official Kotlin MCP SDK)
on `127.0.0.1:<port>/mcp`. Every request needs the token, as `Authorization: Bearer <token>` or, for clients that can't send
headers, `?token=` (compared in constant time); the SDK's DNS rebinding protection limits hosts to localhost. Tool calls run
`CommandExecutor.call` of the open project on the main dispatcher, so AI changes enter the same undo history; text results are
JSON, a result's image (a screenshot) becomes MCP image content, rejections are `isError` results in the user's words, and every
call is logged. Read tools are marked read-only, `WriteCommand.isDestructive` ones destructive. `McpController` (app-level,
created in `main`) keeps `McpSettings` (off by default, port 7781, a random token, connected clients, command overrides) in
`mcp.json` in the settings folder and restarts the server when they change; a taken port shows as `Failed`.

- **One-click clients** (`mcp/clients`): `AiClient` lists the supported tools (Claude Code, Codex, Gemini CLI), each a CLI with
  `mcp add` / `get` / `remove` at user scope; Codex can't send headers, so it gets the token in the URL. `AiClientSetup` is one way
  to run a tool: its program with its default configuration (which apps such as the Claude app use too, so it is always listed,
  run as `command claude` past an alias of the same name that runs something else), the user's replacement command, and each
  shell alias that runs the program (`claude-personal` → "Claude Code (personal)"), found by parsing the shell's `alias` listing
  (`ShellAliases`: zsh, bash, and fish quoting). Setups are connected separately and remembered by id (`claudeCode@claude-personal`). `AiClientsController`
  finds the setups, checks which are installed and connected, connects (turning the server on first) and disconnects, and connects them again
  when the port or token changes. `ShellProcessRunner` runs them through the user's shell as an interactive login shell, like a
  terminal (an app started from the Finder lacks the shell's PATH, and aliases such as `claude-personal` live in `.zshrc`),
  dropping the shell's job control notices; the command is user-editable. A tool counts as connected only when its output names
  the server, since a wrapper alias can succeed without doing anything.
- **Apps with a configuration file** (`AiConfigFileClient`: Claude Desktop, Cursor) are connected by editing their JSON file
  (`McpConfigFile`): only `mcpServers.kubriko-creator` changes, the rest keeps its values, order, and indentation, the file is
  replaced atomically through a link and keeps its permissions (a new one is owner-only), and a file that isn't valid JSON is never
  written. An app counts as installed once its configuration folder exists. Claude Desktop only starts stdio servers, so its entry
  runs `mcp-remote` through the full path of the `npx` the user's shell finds (`ProcessRunner.locate`), with that folder on the
  PATH, and passes the token through the environment; without npx the row asks for Node.js. Cursor gets the URL and header.
- **The panel** (toolbar → AI connection): a Connect / Disconnect / Install row per client, the on/off switch, the address
  with the token for other MCP clients, Advanced (port, token, commands), and the call log.
- **Playing the game:** `start_game` (optionally in a `scene`), `press_keys`, `wait`, `take_screenshot`, and `stop_game` drive a play session through
  `SessionCommand`s Creator sends back on the session's connection, answered by id: keys go to the AWT event
  listeners Kubriko's keyboard input reads, screenshots come from the game's `SkiaLayer` (no OS permission), and every step first
  brings the window to the front, since Kubriko pauses unfocused games, and waits up to two seconds for the focus to arrive
  (keys aren't pressed without it; `press_keys` fails instead). Unexpected exceptions in a tool are `isError` results that name
  the exception. `start_game` also waits (up to five seconds) until the game has drawn Actors, so a screenshot right after it isn't
  empty; that needs protocol 3 and an entry point that reports its Kubriko instances (`getRunningInstances`), and is skipped otherwise.
  Sessions of any protocol version run, using what the lower of the two versions has (`SessionController.request` takes the
  version a command needs); for protocol 1 (older Kubriko) the game tools say the version is too old.
- **Showing the AI's work:** every command has a `title` and a `target` (`CommandTarget`: an Actor type with an optional Event
  and Block index, a Scene, version control, or the game), asked before and after it runs. `CommandExecutor.aiSteps` records each
  AI tool call as a running, then finished (or rejected) `AiStep`. The workspace shows them in `AiActivityBar` (step number, the
  step before, the current one pulsing while it runs, a Follow switch), follows the AI to the Actor type and Event it works on,
  and lights up the finished step's target (`rememberFlashColor`) in the editor and the sidebar (`ui/AiFollowing.kt`).
- Tests talk to a real server on localhost through the SDK's client, run real shell commands, and play a fake session.

## Assistant (ACP)

The in-app assistant's agents are Agent Client Protocol agents (`ai/assistant`, the official Kotlin SDK, `com.agentclientprotocol:acp`).
`AcpAgent` is one agent process (`AgentCommand`, started through an `AgentLauncher`: `ProcessAgentLauncher` in the app) hosting many
`AgentConversation`s (ACP sessions). It starts on first use and initializes with protocol version 1, advertising terminal sign-in
(`_meta["terminal-auth"]`) but neither file system nor terminal access, since Creator's own tools make every change. A conversation
streams the agent's `SessionUpdate`s (from prompts and between them) as `updates`; `prompt` suspends until the turn ends, `cancel`
sends `session/cancel`, and permission requests go to its `PermissionHandler` (`Reject` by default). A process that ends unasked is
started again and its conversations loaded again with `session/load` (the replayed history left out of `updates`), or marked lost
where the agent can't load them; one that exits within 30 seconds of starting more than three times in a row is `Failed` until it
is used again, and one without conversations is only started again when used. A process that exits before answering `initialize`, or
can't be started, fails with `AgentStartException` (its exit code and last standard error lines, also kept in `errorOutput`). Tests run
`FakeAgent`, built on the SDK's agent side, over in-process pipes (`FakeAgentLauncher`).

## Packages (`com.pandulapeter.kubrikoCreator`)

| Package | What it holds |
|---|---|
| (root) | `AppController` (screens, creating/opening/closing projects, recent projects), `CreatorConfiguration` (bundled versions), `settingsDirectory`. |
| `project` | Project inspection (`ProjectInspector`: the build files and name, and the desktop module through `ModuleNode`), `ResourceScanner` (sidebar groups), `ProjectWatcher` (1 s poll of each file's `FileStamp` in `.kubriko/actors`, Scenes, and sources, excluding generated code; files as Creator left them, which `BlockTypeStore` and `SceneStore` note in the workspace's `OwnWrites`, don't count as changed, so Creator's own writes don't reload anything), `KubrikoBuildFiles` (Kubriko dependencies and version, with `KubrikoArtifact`, `VersionCatalog`, and `BuildScript`; each source file's libraries kept by length and modification time), `ExpectDeclarations` (whether the sources declare an `expect`, kept the same way). |
| `actors` | The Block model: `BlockTypeDefinition` (`ActorTypeDefinition`, `ManagerTypeDefinition`) / `PropertyDefinition` / `EventDefinition` / `BlockDefinition` (JSON in `.kubriko/actors` and `.kubriko/managers`, hex colors, no empty fields, `@Description`s that the tool schemas and the Block catalog carry), `PropertyKind` and `PropertyNaming`, `BlockTypeNaming`, `BlockKind` (palette + which Events and kinds allow a Block), `BlockScope`, pure edits (`BlockTypeEdits.kt`), `BlockCodeGenerator` (Blocks to statements) with `ActorTypeCodeGenerator`, `ManagerTypeCodeGenerator` (classes, the registry, and `KubrikoWiring`), `PluginManagers` (which plugins the types use), `ScenesCodeGenerator` (`GeneratedScenes`), `SceneSettingsCodeGenerator` (`SceneSettings`), and `AudioCodeGenerator` (`GeneratedAudio`, with `AudioFiles`), `SpriteFiles`, `GeneratedFile` (checksummed header), `BlockTypeStore` (writes definitions; `regenerate` turns the types in memory into code, writing only files whose code changed, which it knows without reading them while their length and modification time are the ones it left; skips hand-edited files and reports them; keeps each Scene's music the same way), `ActorTypeFamily` (built once per regeneration and passed to every scope, since building it costs more than generating a type). |
| `commands`, `commands/actorTypes`, `commands/managerTypes`, `commands/scenes`, `commands/git`, `commands/project` | The command layer (see Commands): one object per command, with its `Input`. |
| `ai/assistant` | The in-app assistant's ACP client (see Assistant): `AcpAgent`, `AgentConversation`, `AgentLauncher`. |
| `mcp`, `mcp/clients` | The AI connection (see AI connection): `McpToolServer`, `McpController`, `McpSettingsStore`, one-click clients. |
| `workspace` | `WorkspaceController` (resources, builds, routing watcher changes, the `CommandExecutor`), `ConsoleLog`, `SessionController` (the session process, its messages, commands to a running game), `WorkspaceRunner` (the `ProjectRunner` behind the build, play, and game tools), `BlockTypesController` (in-memory edits; writes queued as `PendingWrite`s and written in batches, 100 ms after the last change, a type saved several times written once, then one regeneration and build file sync per batch; `awaitWrites`, which builds, git commands (`CommandContext.awaitWrites`), and closing wait for; reload from disk unless a write is pending), `GradleWorkspaceOpener`. |
| `scenes` | `SceneEditingController`, `SceneView`, `ActorTypeLook` and `PreviewActor`, `SceneHitTest` and `SceneBoundsGrid`, `ScenePatch`, `SceneFraming`, `ActorBodyValues`, `ActorStateEdits`, `ProjectClasspath`, `ClassLoaderEntryPointLoader`, `SpriteLoadingEntryPoint` and `ProjectSpriteLoader`, `SceneStore` and `SceneDocument` (Scene files by name), `SceneSettingsState`. |
| `git` | `JGitRepository` (status, commit all, history, discard, create), `ChangeGrouping` (a type's JSON + generated Kotlin = one change), `GitController`. |
| `template` | `ProjectGenerator` (renders `resources/templates/<id>/`, listed in `files.txt`, `.template` suffix, `dot-` prefix for dot files; regenerates the block-built types; runs Gradle's `wrapper` task; first git commit), naming and form rules, `ProjectUpgrader` and `LineDiff` (template upgrades). |
| `recent` | `RecentProjectsStore` (platform settings folder). |
| `ui`, `ui/actorTypes`, `ui/scenes`, `ui/git`, `ui/mcp`, `ui/components`, `ui/theme` | Compose UI (`ui/scenes`: the Scene editor's pane, canvas, and the sidebar drag). `BlockTypeEditor` follows GameMaker's object window: a one-line header (`BlockTypeHeader`: the name, renamed in place through `rename_actor_type` / `rename_manager_type`, and for an Actor type the type it is based on and the ones based on it, which open on a click), appearance (texts on screen, for a Manager type) and Properties, Event column, the selected Event's Blocks, Block palette. |
| `window` | JetBrains Runtime title bar the content extends under, and the 0.85 interface scale (both from the user's Campfire app). |

## Conventions specific to Creator

- **Every editor change is a command** with its AI tool and tests (validation, apply, and the inverse round trip); the UI
  dispatches commands rather than calling controllers. Undo and redo: toolbar buttons naming the step, Ctrl+Z / Ctrl+Shift+Z /
  Ctrl+Y (Cmd on macOS).
- **Beginner-friendly by default:** the Block palette shows one category at a time (`BlockCategory`, tabs like GameMaker's that wrap
  onto a second row, without the ones a Manager has no Blocks in),
  Blocks are added by a click (at the end, which the Block list scrolls to) or dragged (from the palette or by a Block's handle) to where they go
  (`ListDragState`, an in-window drag, dropped through `add_block` / `move_block`), and Events are dragged to reorder them
  (`move_event`; the selected Event stays selected). Both lists drop into a gap found from the rows the lazy list has laid out
  and scroll while an item is held near their edges (`lazyListDropTarget`); `ListItemKeys` follows Blocks and Events between
  versions (equal ones keep their keys when moved, edited ones the key of their place), so moves animate (`animateItem`, the dragged
  one drawn above the rest, `rememberDraggedKey`) and a new one flashes in its accent, and each Event's Blocks and each type's Events get a list of their own. Each Block is a card indented by its
  section, the console starts closed and opens for problems and what the game logs, the sidebar hides asset groups nothing
  uses yet and collapses Kotlin files, and New project keeps the package under More options.
- Desktop-sized UI from the start: `ui/theme` (`CreatorTheme`: Kubriko's colors with `CreatorTypography`, Public Sans in four
  weights at sizes that make up for the 0.85 interface scale, with SemiBold titles and Medium labels; the Showcase keeps the shared
  typography), `ui/components` (`CompactTextField`, which scrolls code sideways instead of wrapping it, `IconActionButton` and
  `WithTooltip`, `PanelHeader`, and `ColorPickerDialog`: hue, saturation, brightness, and opacity sliders (`GradientSlider`) and a hex code,
  ported from Tesselar's model editor, with its logic in `ColorPickerValue` and `HsvColor`, and `checkerboard()` behind what is
  translucent; every color field's swatch opens it, and hex codes are `#RRGGBB` or `#AARRGGBB`), vector icons in
  `composeResources/drawable`, everything in `strings.xml` with plain apostrophes.
- **Friendly, and told as a film:** Actor types are the cast (theater masks), Manager types the crew (a director's chair), and
  Scenes the scenes being shot (a clapperboard), never scenery. Each kind of thing keeps one `Accent` (`ui/components`, with a
  light and a dark shade) everywhere it shows: the cast, the crew, the Scenes, Events, and every Block category, whose Blocks
  are colored chips in the palette and cards with an `accentEdge` in the Event. `AccentIcon` (an icon on a tile of its
  accent's wash) marks headers, `PanelHeader` and `SidebarItem` take an `accent`, Play is green, and the empty workspace
  (`WorkspaceHint`) introduces the cast, the crew, and the scenes.
- Generated Kotlin reads like the examples' hand-written Kubriko code: an Actor type is `Dynamic` only when it moves or has work every
  frame (a Kotlin Block counts), looks up only the Managers its Blocks use, is `Collidable` only when a collision Event (its own or another
  type's) involves it, and builds its collision mask with the body's scale and rotation, centered on the body wherever its pivot is (the Scene editor creates an edited Actor again from its state, so the mask
  follows a new size too).
- Generated Actors behave like a careful hand-written game's (`ActorClassCode`, shared by both Actor generators): movement
  steps at most 33 ms at a time, so a hitch can't move a fast Actor through a thin one; the remove Blocks set `isRemoved`
  right away, which `update`, collisions, and moveTowardsNearest skip until Kubriko removes the Actor after the frame (so a
  Brick can't score twice); Dynamic types are `isAlwaysActive`, since a Scene can be larger than the screen; edge Events use
  the bounding box, so pivots, rotation, and scale don't move the edges; collision branches put descendants before their
  ancestors (`is Bat` before `is EnemyBase`); the mask follows the body's rotation (boxes) or size (circles) every frame;
  Bounce off it is one `bounceOff(other)` function per class; and Manager screen texts keep their `TextStyle`s in fields.
  `GeneratedScenes` ignores a request for the Scene it is already loading, so goToScene in an Event that runs every frame
  doesn't restart the load forever.
- Generated Kotlin must compile for every combination of Events and Blocks (unknown collision targets are left out, Kotlin Blocks get their own
  `run { }` scope, Blocks outside their Event become comments) and follow the engine's no-allocation rules.
- Nothing under `engine/`, `plugins/`, or `tools/` may depend on `creator`. Library changes Creator needs follow the public API rules (confirm first).
- **Library releases:** what Creator needs from the libraries reaches projects only through a Kubriko release, since projects
  resolve Kubriko from Maven Central and opted-in ones use Creator's own version, so a Creator release follows each Kubriko release
  that changes its library side, and `scene-editor-api` and `creator-api` promise no binary compatibility across versions. A session
  older than Creator keeps working with the features it lacks hidden, offered by protocol version rather than refused.
  `CreatorEntryPoint` only gains members with default implementations, and nothing goes into a published artifact before the
  Creator feature that uses it exists.
- Generated code, `.kubriko/`, and Scenes are owned as described in `plan/README.md` → Files and ownership.
