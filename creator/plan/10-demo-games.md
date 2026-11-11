<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Demo games

At least five complete games bundled with Creator, opened from the project list as a copy in the user's folder (the bundled original
never changes), each building on what the one before taught. They are the reference projects for Blocks, so the integration test
builds and plays every one, and each comes with a short tour of what to look at first, told by its screenplay.

| # | Game | What it introduces |
|---|---|---|
| 1 | Click the moving targets | One Actor type, one Scene, pointer input, a Timer, and Properties |
| 2 | Paddle and bricks (the Paddle game template, `03-blocks.md`) | Collision, a score Manager type, text, sounds, and winning and losing |
| 3 | Top-down space shooter | Spawning and removing Actors, keyboard and gamepad input, particles, and increasing difficulty |
| 4 | Side-scrolling platformer | Physics, sprites and animation, a following camera, and several levels as Scenes |
| 5 | Small adventure with menus | Title and pause menus, saved progress (persistence), Traits and custom Blocks, and some hand-written Kotlin next to the Blocks |

Original games in common genres, with their own art and sounds (licensed for reuse, so users may ship games made from them). More can
follow once these five work, such as one exported to every platform.

## How they are made

Each game is built 100% in Kubriko Creator: through its own UI and its assistant's tools, the screenplay included, and never by
editing the project's files by hand or with anything outside Creator (the hand-written Kotlin of game 5 is written in Creator too).
The games are made one after the other, each by an agent of its own driving Creator headlessly (its tools, and the end-to-end harness
under Xvfb, never on the user's Mac). **The moment that agent meets anything in Creator that could be better** (a missing Block or
tool, an awkward or slow step, a bug, an unclear message or hint, or something a beginner wouldn't find), **it stops working on the
game, makes that improvement in Creator first** (its tests, review, and commit, as any plan item gets), and only then continues the
game from where it left off, through the improved Creator. A game is done only once it was built this way from start to finish. The
improvements are the real output, and the game is their proof, so the next game's agent starts from a better Creator, and the order of
the table is the order of the work.

## Bundling

The finished games ship inside Creator's installers (`08-installing-creator.md`) and are copied out on first open; the project list
shows them in a section of their own, each with its tour.
