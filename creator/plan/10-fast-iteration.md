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

The edit-and-see loop is the product. Today, opening a Scene compiles the whole project through Gradle (tens of seconds, more as the
project grows), and any Actor type change marks the open Scene outdated until it is compiled again; playing the game is a full build
and launch. Measure first: a benchmark project (the Breakout, and a generated one with 200 Actor types and 5,000-Actor Scenes) and the
time of each step, kept in the integration test's output, so every change here is a measured one.

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
   a restart (moved here from `07-later.md`). Generated code is shaped for it: no static state that a reload would lose.

## Playing

- `start_game` and the Play button reuse the warm daemon and the classpath the Scene editor already built when nothing changed since.
- A "play from this Scene" button that starts the game in the open Scene: the session passes the Scene's name, and the template's
  `main` hands it to `createManagers(firstScene = …)` instead of the first one (a template upgrade).
