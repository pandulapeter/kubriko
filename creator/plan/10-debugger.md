<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Debugger

- **Debug agent** in `tools/creator-session`, answering the session protocol: Actor tree, live property values (through `@Exposed`), logs,
  `MetadataManager` metrics, pause, single-step through a `TickSource`, editing a property while paused. It reads the running `Kubriko`
  instances from `CreatorEntryPoint` (added in 0.8.0, `00-library-release.md`), and reaches projects with a later
  release, offered only to sessions that report the capability.
- **Panels in Creator:** Actors, Inspector, Logs, and a frame-time graph, next to Play.
- **Other targets:** Android through `adb reverse`, web through a WebSocket variant of the protocol.
- **IntelliJ users** can add the agent to a debug build and attach Creator to a game they started themselves.

## Retiring the debug menu

1. Ship `tools/debug-overlay` (FPS, frame time, Actor count, recent logs) and the debug agent.
2. Deprecate `tool-debug-menu`, `-api`, and `-noop` for one release, pointing to both replacements; move the Showcase to the overlay first.
3. Remove them in a later release.
