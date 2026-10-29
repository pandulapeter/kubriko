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
  may. **Waiting for the user's decision**: pass it (only in Creator's builds, through `gradleArguments`, so a project's own
  builds are unaffected, at the cost of a full rebuild whenever the two alternate), or wait for Kotlin 2.5.
- Every classpath build also packs `desktopJar` (about 0.23 s); step 3 can point the classpath at the class folders instead.
- Creating a 1,000-Actor Scene from the classes takes 0.4 to 0.6 s, which step 4 pays on every swap and step 5 should hide.

## Steps, cheapest first

1. **Keep Gradle warm:** one Tooling API connection and daemon per workspace for every task (check that nothing opens a second one),
   started when the project opens, with a classpath build in the background right away, so the first Scene opens from a warm build.
2. **Configuration cache and parallel builds in projects:** `org.gradle.configuration-cache=true` and `org.gradle.caching=true` in
   the template's `gradle.properties` (a template upgrade), and the init script made configuration-cache compatible (it passes
   `--no-configuration-cache` today).
3. **Compile only what's needed:** the classpath task compiles the desktop target only (measured: it runs no web or other
   target's tasks), but it packs `desktopJar` every time. A type change is a full rebuild until incremental compilation of
   common code works (see Measuring the build); with it, about 0.7 s at 200 types.
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
