<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Version control

Kept to what a beginner needs, while the project stays an ordinary repository other tools can use at the same time.

- **Change markers** in the sidebar for new and changed Actor types, Scenes, and files.
- **Restore** an Actor type or Scene from any commit in the history; show what a commit changed.
- **Branches:** create and switch from a menu. Merging is left to other tools at first.
- **Checkpoints:** offer to commit before risky operations (Convert to code, deleting an Actor type, upgrading Kubriko, AI changes).
- **Remotes:** publish to a new GitHub repository, push, and pull (fast-forward only, explaining when histories diverged), credentials in the OS
  keychain. A generated GitHub Actions workflow builds every platform (`10-export-and-distribution.md`).
- **Conflicts:** never write conflict markers into Creator's JSON; stop before changing anything and explain which files clash. Since every
  Scene Actor has a stable ID, a Scene conflict can later be shown, and resolved, Actor by Actor (`01-large-projects.md`).
- Projects inside a larger repository (not at its root) are currently left to other tools; decide whether to support them.
