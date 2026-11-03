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
Unit tests, offscreen snapshots (`UiSnapshots`), and `ProjectTemplateIntegrationTest` each stop short of that: none opens a real
project in the running app, changes it, builds it, plays it, and looks at the result. This plan adds what does. Nothing else
starts until the harness and the first slice exist; the remaining slices are done before each later plan item, oldest first.

## The harness

- An opt-in suite (`KUBRIKO_CREATOR_E2E=true`, skipped with `Assume` otherwise, like the other opt-in tests in `../CLAUDE.md` →
  Testing) that starts the real Creator on a fresh copy of a fixture project, drives it, and asserts on what it produced: the
  project's files and git diff, the build's result, the console, Creator's window, and the played game's window.
- It drives Creator through the same commands its UI and its MCP tools use (`StartGame`, `PressKeys`, `TakeScreenshot`,
  `set_project_settings`, and the rest), over the MCP server where that is the realistic path. What only the window can show
  (a dialog's layout, a red error, a focus change) is checked in a screenshot of Creator's own window, and input to the window
  itself is synthetic only where no command exists.
- Screenshots are judged by what can be asserted without a person: pixels at known places (the bars of a fixed aspect ratio, a
  sprite's color where an Actor stands), sizes, and differences from the frame before. Every scenario also saves its images, so
  the agent running it can look at them, and does, before calling a change done.
- The fixture is a Breakout project kept in the repository (or built from the template in the test), at an old template version
  and with no format version, so the upgrades and the migration are part of what it covers. It is never the user's
  `~/Projects/breakout`.
- It runs on Linux under `xvfb-run` (as `showcase-publish-desktop.yml` already does for the Showcase), so the unattended plan-agent
  job and a nightly job in `Tests` can run it, and on macOS locally. Skiko's software rendering under Xvfb has to be confirmed
  first; if the played game can't draw there, that is solved before anything else.
- Scenarios are short and independent, each on its own copy, and a failure says which step and shows the last screenshots.

## Slices

Each slice turns one section of `HANDOVER.md` → Not yet verified into scenarios, runs them, fixes every bug they find (each in its
own commit with a test), and deletes the section's bullets. A bullet that needs something no agent has (a high refresh rate
panel, a real Windows machine, a browser sign-in, an OS keychain prompt, a vendor's quota or terms) moves to `HANDOVER.md` → Before
a release instead, worded as one line.

1. Project settings: Game and Identity, the newest, which proves the harness on upgrades, the dialog, and a played window.
2. The rest of `HANDOVER.md` → Not yet verified, one section per slice, in the order it lists them.
