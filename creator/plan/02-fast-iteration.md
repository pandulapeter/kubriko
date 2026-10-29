<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Fast iteration

The edit-and-see loop is the product, and what decides whether a beginner prefers Creator to writing code. Today, opening a Scene
runs a Gradle build, and any Actor type change marks the open Scene outdated until it is built again; playing the game is a full
build and launch. On Breakout these builds take about a second with a warm daemon and five with a cold one, so the cost is mostly
in waiting at all: steps 4 and 5 remove the wait from the editor entirely. Measure first, with the benchmark project of
`01-foundation.md` and the console timings it adds, so every change here is a measured one. This comes right after the foundation,
before more features.

## Measuring the build

Exists (`benchmark/CreatorBuildBenchmarkTest`, `KUBRIKO_CREATOR_BENCHMARK=true` and `KUBRIKO_CREATOR_VERSION`): the benchmark
project of `01-foundation.md` built for real through the Tooling API, the init script, and Creator's own arguments (now shared,
`SessionInitScript.gradleArguments`), reporting for each step the generated files it touched and the classes Kotlin wrote again.
It passes `--no-build-cache`, since every run makes the same changes and a build cache restores them instead of compiling (the
user's `~/.gradle/gradle.properties` turns it on; the first numbers of this benchmark were cache hits).
`KUBRIKO_CREATOR_BENCHMARK_ARGUMENTS` adds Gradle arguments, to measure a change to the build before making it.

Baseline on 2026-10-29 (M-series Mac, warm Gradle and Kotlin daemons; the first run after they start is several times slower
throughout: a 17 s cold build and 7 to 10 s per type change), with the property below as the second column:

| Step | Build | With unsafe multiplatform IC |
|---|---|---|
| Cold build (a project never built) | 7.3 s | 7.2 s |
| Warm build, nothing changed | 0.2 s | 0.2 s |
| A Block added to a leaf Actor type | 5.5 s (all 253 classes) | 0.7 s (only that type) |
| A Property added to a leaf Actor type | 5.5 s | 0.6 s (only that type) |
| A Block added to the base of a family of ten | 5.4 s | 0.7 s (the type and its base) |
| A Property added to the base of a family of ten | 5.5 s | 1.4 s (the family and its users) |
| One Actor moved in a Scene | 0.5 s (no compile) | 0.5 s |
| Reload (`--rerun-tasks`: everything from scratch) | | 5.3 s, then 0.15 s for the next build |
| Loading the classes / creating a 1,000-Actor Scene | 11 ms / 0.4 to 0.6 s | |

What it shows:

- **The generator is already shaped for incremental compilation:** a change to one type rewrites only that type's file, and
  the shared files (`GeneratedActorTypes`, `KubrikoWiring`, `GeneratedScenes`) don't change. A Scene change compiles nothing.
- **Kotlin doesn't compile incrementally anyway:** every change to `commonMain` (where all generated code lives) is a full
  rebuild, with the reason "Incremental compilation might be incorrect (KT-62686)" in Kotlin's build report. A change only in
  `desktopMain` stays incremental. JetBrains' fix is the opt-in "separate compilation" of Kotlin 2.5.0 (Creator is on 2.4.21).
  Until then, the internal `kotlin.internal.incremental.enableUnsafeOptimizationsForMultiplatform=true` turns incremental
  compilation of common code back on (the second column). Checked by hand: platform code using a changed common declaration is
  still recompiled (a renamed function fails the build, a new defaulted parameter recompiles the caller). What KT-62686 guards
  against wasn't reproduced; `expect`/`actual` pairs are the suspected case, which generated code never has but hand-written code
  may. **Done, with the user's agreement** (2026-10-29, `IncrementalCompilationPolicy`): Creator's builds pass it (only
  Creator's, through `gradleArguments`, so a project's own builds keep Kotlin's default), except in a project whose sources
  declare an `expect` (`ExpectDeclarations`), and the build after a failed one, after Reload, or after classes that couldn't be
  loaded runs with `--rerun-tasks`, which compiles everything from scratch. (Turning the property off doesn't do that: it isn't
  an input of the compile task, which stays up to date.) Remove it all with Kotlin 2.5's separate compilation.
- Every classpath build also packs `desktopJar` (about 0.23 s); step 3 can point the classpath at the class folders instead.
- Creating a 1,000-Actor Scene from the classes takes 0.4 to 0.6 s, which step 4 pays on every swap and step 5 should hide.

## Steps, cheapest first

1. **Keep Gradle warm. Done** (2026-10-29): one Tooling API connection per workspace serves every task (the only other one,
   `GradleWrapperInstaller`, runs when a project is created), and the workspace builds and loads the classpath as soon as it opens,
   so the first Scene opens without a build: the cold build (7.3 s at 200 types, 2.9 s for Breakout seen in the app) moves out of
   the first Scene's way. Classpath builds don't overlap, play waits for the one running, and a cancelled build returns only once
   Gradle stopped it, so none of them starts a second daemon. One does still: a Scene opened while the game plays, since the play
   session holds its daemon for as long as the game runs (Playing, below, would end that by launching the game from the
   classpath). Not a benchmark row: the benchmark runs Gradle directly, and this changes when the build runs, not how long.
   **Kotlin's compile daemon is warmed too** (2026-10-29): the build at opening starts it only when something needs
   compiling, so once the classes are loaded, the init script's `kubrikoCreatorWarmUp` compiles one generated file in a
   compilation of its own beside the desktop target's, seeing its code and classpath, so the compiler, its plugins, and the
   incremental path all run. Measured on Breakout from the command line with a fresh Kotlin daemon: the first change after an
   up-to-date build took 2.9 to 3.2 s without it and 1.0 to 1.5 s with it (the second change about 1 s either way); the
   warm-up itself takes 1.2 to 2.3 s, in the background. In the app (fresh Kotlin daemon, the Gradle daemon possibly warm
   from the command line runs): the warm-up ran right after opening, nothing in the console, and the first change built in
   3.0 s; the same change without the warm-up wasn't timed in the app. A cold Gradle daemon still costs its own JIT
   warm-up on the first few builds, which this doesn't touch.
2. **Configuration cache in projects. Done** (2026-10-29): the template's `gradle.properties` turns on
   `org.gradle.configuration-cache` (`org.gradle.caching` was on already), template version 6 adds both to existing projects,
   Creator no longer passes `--no-configuration-cache`, and the init script works with it (the Kubriko version is read while
   configuring). Gradle fingerprints every `-P` value and the init scripts' paths, so the classpath file and the script keep
   theirs between builds and launches. Measured at 200 types, without and with the cache: a warm build 138 to 91 ms, a Block
   added to a leaf type 653 to 530 ms, a Property 600 to 520 ms, one Actor moved 401 to 297 ms, the next build after Reload
   125 to 62 ms; cold builds and Reload are compile time, unchanged. Play builds configure again once per workspace opened,
   since the session's port is chosen then. `org.gradle.parallel` is left out: the template is one Gradle project, which it
   wouldn't speed up.
3. **Compile only what's needed. Done** (2026-10-29): the classpath task compiles the desktop target only, and no longer packs
   `desktopJar`: it hands Creator the folders the jar is packed from (the jar task's `source`; a file the jar would put elsewhere
   is copied to a folder of its own), and Creator reads their class files into memory when it loads them (`ProjectClassLoader`),
   which is what copying the jar did, so a rebuild can't mix two versions of a class. Measured at 200 types (two runs):
   a Block or Property added to a leaf type 530 to about 295 ms, to the base of a family 550 to 300 ms, one Actor moved 297 to
   90 ms; loading the classes went from 12 to 67 ms, so a Scene opened after a change gains about 180 ms. Play builds still
   pack the jar, which the game's own process needs no more than the editor does (Playing, below).
4. **Reload without reopening. Done** (2026-10-29): a type or source change builds the open Scene again in the background and
   swaps its classes when they are ready (`SceneEditingController.update`), the new `SceneView` creating its Actors while the
   old one stays on screen and then taking over its camera; the selection stays. A change during the build builds once more
   after it instead of cancelling it. A failed build keeps the old classes, with the notice and Reload, until a build works.
   Seen in the app on Breakout: a Brick's shape changed on disk showed on screen 1.9 s after the file was written (a 0.3 s
   build; the rest is the watcher's one-second poll, regenerating, and creating the Actors), zoom and selection kept; a
   source that doesn't compile showed the notice and its error, and removing it recovered. A Scene swapped in while Creator
   was in the background (a change made in another app) stayed empty until it was focused again, since a Kubriko instance
   culls, and so draws, its Actors only on ticks, which `viewportFrames()` stops without the focus; the Scene editor's
   instance now ticks without it (not yet seen in the app). **Found:** the first change after
   opening built for 10 s, since the warm-up build at open compiled nothing (it was up to date) and so never started Kotlin's
   compile daemon. Starting that daemon at open belongs to step 1, and is done (above).
5. **Draw what doesn't need the project's classes:** block-built Actor types are known without compiling (shape, color, size, sprite),
   so the Scene editor can show a changed type's Actors from its definition right away (a preview Actor drawn by Creator) while the
   compile runs, then swap in the real classes. Hand-written types keep their last compiled look until then. **Done**
   (2026-10-29, seen working in the app with a build kept failing on purpose): every build knows the looks (`ActorTypeLook`: shape, color, size, sprite file) of
   the types it compiled, taken once the changes are written; the open Scene's `previews` are the types whose look changed
   since the shown classes were built, or that are new, updated as soon as a type changes in memory; and `SceneView` draws
   their Actors as `PreviewActor`s (Creator's own, from the Actor's saved state and the look, selected and moved like the
   real ones) until classes built with those looks are swapped in. A new type dropped into the Scene shows right away too.
   Behavior that isn't the look (Properties shown differently, a parent's drawing) waits for the classes, as before.
6. **Hot reload in the game. Done** (2026-10-29, no library change): projects already have Compose Hot Reload, which Compose
   Multiplatform bundles; play builds pass `kubrikoCreatorHotReload`, and the classpath task then also writes how Hot Reload's
   run task would start the game (its JetBrains Runtime, its argument file, its reload task, and the session libraries), so
   Creator starts the game with Hot Reload's agent (DevTools off), from Hot Reload's own copy of the classes. Every change
   while it plays builds the project, and `GameReloader` compares the built classes' `ClassShape`s (superclass, interfaces,
   instance fields, and the content of a class with mutable static state or enum constants) with the ones the game runs:
   only code changed runs the reload task, anything else starts the game again from the same Scene, naming the type in the
   console. Found first by hand: reloading a class with a new field leaves it null in live objects, and the game window died
   with an error dialog on the next draw; `KubrikoWiring.isLoggingEnabled` is the only mutable static state generated code
   has. Seen in the app on Breakout: a Log Block's message changed and pressed in 0.2 s with the game's state kept, the
   Paddle made a circle restarting the game with a console line, and a reload after that restart. A project without Hot
   Reload's tasks or a JetBrains Runtime plays as before.

## Playing

- **Done** (2026-10-29, seen working on the scratch Breakout): `start_game` and the Play button build the classpath (0.0 to
  0.5 s when nothing changed, with the configuration cache) and start the game themselves from what the build says
  (`GameLaunch`, `GameProcess`) instead of through Gradle, which packed the jar and held a daemon for as long as the game
  ran; the game window showed 1.1 to 1.4 s after the click. The game runs from a copy of the project's class files.
- **Done** (2026-10-29, seen working): Play from this Scene, in the Scene editor's header (and `start_game`'s `scene`),
  through template version 7 (`KubrikoWiring.firstSceneOverride`, set by `Main.kt` from a system property). Seen: the
  version 7 upgrade's diff and Apply, a version 6 project starting with its first Scene with a console note, and after the
  upgrade, `level2` starting the game with only its Ball and Paddle.
