<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# End-to-end checks

Creator is written by AI, so the AI verifies it in the real app, the way a person would, instead of leaving checks to the user.
The harness that does it exists (`endToEnd/` in `:creator`'s tests; `../CLAUDE.md` → Running and testing says how it runs), and
`HANDOVER.md` → Not yet verified is settled: what unit tests and snapshots cover was deleted, the gaps got unit tests, what needs
hardware or an account went to Before a release, and what needs a played game went into one scenario.

## Left

- Run `UpgradingAndPlayingTest` headless (never on the user's display) and look at its images. It upgrades the fixture from template
  version 7 and plays it: a Scene of sprite-drawn Gold Bricks chosen as the first Scene, a fixed aspect ratio, a Log Block's line in
  the console, the build naming the game from `gradle.properties`, a hand-written change to `Game.kt` reaching the running game, and
  a hand-broken `game.frameRate` failing the build with a message naming it. Fix what it finds, each in its own commit with a test,
  then delete this file.
