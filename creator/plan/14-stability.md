<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Stability

What breaks, hangs, or loses work today when files are broken, a process misbehaves, or something fails halfway. Each item names
the code, why it matters, and the direction; none of it is built yet. The user put stability before new features, so this file
comes right after `01-large-projects.md`, before the rest of `02-ai-assistant.md`.

## Scene editor

- **Dragging an Actor without a saved position** (a hand-written type relying on its default, or any Actor of an invalid Scene) moves
  it on screen, but `move_actor` is rejected and nothing reloads, so the screen and the file disagree until the Scene is opened
  again (`ui/scenes/SceneCanvas.kt`). Direction: treat the press as a pan unless the Actor's state has a position.
- **Renaming a Scene ignores a failed move.** `SceneStore.move` doesn't check `renameTo` (Windows refuses while the game holds the file),
  yet `renameScene` still renames the goToScene Blocks and follows the open Scene to a file that doesn't exist. Direction: `Files.move`,
  and `rename_scene` failing (`Applied.failed`) before anything else changes; the folder rename stops at the first failure.
