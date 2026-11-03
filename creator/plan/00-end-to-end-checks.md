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
The harness that does it exists (`endToEnd/` in `:creator`'s tests; `../CLAUDE.md` → Running and testing says how it runs). What
is left is settling `HANDOVER.md` → Not yet verified, mostly by deleting it, and running the checks nightly. Unit tests are
preferred to scenarios (`../CLAUDE.md` → Running and testing says which deserve one), so the scenarios stay few.

## Running it nightly

- A job in `Tests` that runs the scenarios under `xvfb-run -s "-screen 0 1920x1080x24"` after its `publish-check` job's publish
  (`KUBRIKO_CREATOR_E2E=true KUBRIKO_CREATOR_VERSION=0.0.0-check`), keeping `creator/build/end-to-end` as an artifact. It needs a
  session whose token can change `.github/workflows`, which the unattended plan agent's can't.

## Settling Not yet verified

One pass over the whole list, before the next plan item. Each bullet is, in this order of preference:

1. **Deleted**, when a unit test or snapshot already covers it, or it is a label, a menu entry, a field's validation, a layout,
   or another detail that can't really break unnoticed. Most bullets end here.
2. **Covered by a unit test** added now, when one can reach it and none does.
3. **Moved to `HANDOVER.md` → Before a release** as one line, when it needs something no agent has (a high refresh rate panel, a
   real Windows machine, a browser sign-in, an OS keychain prompt, a vendor's quota or terms).
4. **Checked by a scenario**, only for what is left: real builds, played games, and processes working together. Those are
   grouped into a handful of scenarios driven through the MCP tools (`set_project_settings`, `start_game`, `take_screenshot`, and
   the rest); no synthetic input on Creator's window is added for them. The likely ones: the template upgrades from version 7
   applied to the fixture and the game still building and playing, a chosen first Scene and a fixed aspect ratio showing in the
   played game, and a hand-broken `game.frameRate` failing the build with a message that names it.

Every bug found is fixed in its own commit with a test, and the section is gone at the end.
