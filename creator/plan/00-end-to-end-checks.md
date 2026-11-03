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
is left is turning `HANDOVER.md` → Not yet verified into its scenarios, and running it nightly. Nothing else starts until the first
slice is done; the remaining slices are done before each later plan item, oldest first.

## Running it nightly

- A job in `Tests` that runs the scenarios under `xvfb-run -s "-screen 0 1920x1080x24"` after its `publish-check` job's publish
  (`KUBRIKO_CREATOR_E2E=true KUBRIKO_CREATOR_VERSION=0.0.0-check`), keeping `creator/build/end-to-end` as an artifact. It needs a
  session whose token can change `.github/workflows`, which the unattended plan agent's can't.

## Slices

Each slice turns one section of `HANDOVER.md` → Not yet verified into scenarios, runs them, fixes every bug they find (each in its
own commit with a test), and deletes the section's bullets. A bullet that needs something no agent has (a high refresh rate
panel, a real Windows machine, a browser sign-in, an OS keychain prompt, a vendor's quota or terms) moves to `HANDOVER.md` → Before
a release instead, worded as one line.

1. Project settings: Game and Identity, the newest, which proves the harness on upgrades, the dialog, and a played window. The
   dialog and the upgrade's review are opened from Creator's window, which no tool does, so this slice adds the harness's
   synthetic input: clicks and keys through `java.awt.Robot` on Creator's window, found in a screenshot (under Xvfb, which has
   no window manager, the window opens at the same place every time).
2. The rest of `HANDOVER.md` → Not yet verified, one section per slice, in the order it lists them.
