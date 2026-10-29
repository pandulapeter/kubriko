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

The benchmark of `01-foundation.md` times Creator's own work without Gradle, so nothing yet shows how a build grows with the
project. Compiling stays on the edit loop for hand-written types even after step 5, so before step 1 the benchmark gets a run
that builds its project for real (behind `KUBRIKO_CREATOR_BENCHMARK=true`, through the same Tooling API connection and
`kubrikoCreatorClasspath` task as the Scene editor), timing:

- A cold build, and a warm one with nothing changed (Gradle's fixed cost per build).
- One Block added to a leaf Actor type, and a Property added to one (a change to its generated class's API).
- A change to the base type of a family of ten, which every type in the family compiles against.
- One Actor moved in a Scene, which regenerates for the Scenes' music and so must not cause a compile at all.
- Loading the new classes and creating a 1,000-Actor Scene from them, apart from the build.

With these, the build should also show which generated files a change touches. A change to one type should leave the API of
shared generated files (`GeneratedActorTypes`, `KubrikoWiring`, `GeneratedScenes`) unchanged, so Kotlin's incremental compilation
recompiles only that type and what uses it. If a shared file's API changes every time, its dependents recompile on every edit, and
the generator should be fixed before steps 1 to 3 are measured. The baseline goes in a table here, like the one in
`01-foundation.md`; steps 1 to 4 are measured against it and add a row each, and a target is set from the first numbers (a warm
type change at 200 types within a few seconds is the expectation step 3 already states).

## Steps, cheapest first

1. **Keep Gradle warm:** one Tooling API connection and daemon per workspace for every task (check that nothing opens a second one),
   started when the project opens, with a classpath build in the background right away, so the first Scene opens from a warm build.
2. **Configuration cache and parallel builds in projects:** `org.gradle.configuration-cache=true` and `org.gradle.caching=true` in
   the template's `gradle.properties` (a template upgrade), and the init script made configuration-cache compatible (it passes
   `--no-configuration-cache` today).
3. **Compile only what's needed:** the classpath task compiles the desktop target only; check that it doesn't also process resources
   for other targets or run the web toolchain. Generated code is a few small files, so Kotlin's incremental compilation should keep a
   type change at a few seconds once the daemon is warm.
4. **Reload without reopening:** after a type change, compile in the background and swap the Scene editor to the new classes when the
   build finishes (new class loader, `SceneView` created again from the open Scene, camera and selection kept), instead of an
   "outdated" notice and a manual reload. The notice stays only while a build is failing.
5. **Draw what doesn't need the project's classes:** block-built Actor types are known without compiling (shape, color, size, sprite),
   so the Scene editor can show a changed type's Actors from its definition right away (a preview Actor drawn by Creator) while the
   compile runs, then swap in the real classes. Hand-written types keep their last compiled look until then.
6. **Hot reload in the game:** Compose Hot Reload in the play session, so Block and Kotlin changes apply to a running game without
   a restart (moved here from `13-later.md`). Generated code is shaped for it: no static state that a reload would lose.

## Playing

- `start_game` and the Play button reuse the warm daemon and the classpath the Scene editor already built when nothing changed since.
- A "play from this Scene" button that starts the game in the open Scene: the session passes the Scene's name, and the template's
  `main` hands it to `createManagers(firstScene = …)` instead of the first one (a template upgrade).
