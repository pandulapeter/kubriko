<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Onboarding

The first ten minutes after someone downloads Creator: everything it needs is fetched for them with visible progress, there are games to open
and learn from right away, and a project they already have in version control is one URL away. Comes after `09-export-and-distribution.md`,
since it starts from Creator's own installer, and after the Block set in `05-blocks.md`, which the demo games are built from.

Today Creator runs from this repository with a JDK already installed, git needs no installation (JGit), and Gradle distributions are
downloaded by the wrapper inside a build, with nothing to show for it but the console.

## First-run setup

- **One setup screen** on first launch, before the project list, listing what Creator needs and what each costs: a JDK 21, the Gradle
  distribution the templates use, and optionally the Android SDK (only for Android export and Android targets). Each has its size, a progress
  bar with speed and time left, and a cancel button; required parts start at once, optional ones on a click.
- **Everything into Creator's own data folder**, never the system: the JDK and Gradle unpacked there, the Tooling API pointed at them, nothing
  on `PATH` changed. A JDK 21 or Gradle that is already installed can be chosen instead.
- **Downloads that survive real networks:** resumed after a dropped connection or a restart, checksums verified before unpacking, mirrors or a
  manual file as the fallback, proxy settings read from the OS (and editable), and a clear error that says what to do.
- **The Android SDK** through its command-line tools, with the licenses shown and accepted in Creator (`09-export-and-distribution.md`), and
  only the packages the templates need.
- **Warming up:** after the downloads, the starter template is built once in the background, so the user's first project doesn't wait for
  Gradle to fill its caches.
- **Settings → Components** shows the same list later: versions, disk use, update, repair, and remove.
- **Tested** against a fake download server (slow, interrupted, wrong checksum, offline), like the rest of Creator's controllers.

## Demo games

At least five complete games bundled with Creator, opened from the project list as a copy in the user's folder (the bundled original never
changes), each building on what the one before taught. They are the reference projects for Blocks, so the integration test builds and plays
every one, and each comes with a short tour of what to look at first.

| # | Game | What it introduces |
|---|---|---|
| 1 | Click the moving targets | One Actor type, one Scene, pointer input, a Timer, Properties |
| 2 | Paddle and bricks (the Paddle game template, `05-blocks.md`) | Collision, a score Manager type, text, sounds, winning and losing |
| 3 | Top-down space shooter | Spawning and removing Actors, keyboard and gamepad input, particles, increasing difficulty |
| 4 | Side-scrolling platformer | Physics, sprites and animation, a following camera, several levels as Scenes |
| 5 | Small adventure with menus | Title and pause menus, saved progress (persistence), Traits and functions (`04-reuse-and-traits.md`), and some hand-written Kotlin next to the Blocks |

Original games in common genres, with their own art and sounds (licensed for reuse, so users may ship games made from them). More can follow
once these five work, such as one exported to every platform.

## Opening a project from version control

- **Clone** on the project list: a URL (HTTPS or SSH), a folder, and a progress bar, through JGit, so no git installation is needed; GitHub
  sign-in and private repositories use the same keychain credentials as `07-git.md`'s remotes.
- After cloning, the project opens like any other: a Kubriko project that isn't opted in is offered the opt-in (`10-project-upkeep.md`), and one
  that isn't a Kubriko project at all is explained rather than opened half-way.
- Submodules and Git LFS are reported if the repository uses them; decide whether to support them.
