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

Seeing inside the running game, for the user and the AI alike, and retiring the debug menu once that covers it. The session
(`tools/creator-session`) grows commands for it; each is offered only to sessions that report the capability, so projects on an
older Kubriko keep working (`SessionProtocol`'s rules).

## Game time

- **Pause, step, and slow down** the running game through the session protocol: pausing and single-stepping through its
  `TickSource`, and a time scale applied to every tick's delta.
- **A game tool for it**, so the agent can watch an effect that is over in half a second: slow the game or pause it, step it a frame
  at a time, and take screenshots, without changing the project. Today the agent changes Blocks to slow an effect down and changes
  them back, which leaves the game wrong if the turn stops in between.
- Buttons for the same next to Play.

## The debug agent

- In `tools/creator-session`, answering the session protocol: the Actor tree, live Property values (through `@Exposed`), and
  `MetadataManager`'s metrics, and editing a Property while paused. It reads the running `Kubriko` instances from `CreatorEntryPoint`.
- **Panels in Creator:** Actors, Inspector, and a frame-time graph, next to Play; the console already shows the logs.
- **For the AI:** a game tool reading Actors' positions and states from the running game, used in place of screenshots and logs where
  those say enough, which the system prompt and `take_screenshot`'s description say.

## Other targets

Android through `adb reverse`, and the web through a WebSocket variant of the protocol, for the games Play starts there, which then get
the game tools and the console the desktop has.

## IntelliJ users

Adding the agent to a debug build, and attaching Creator to a game they started themselves.

## Retiring the debug menu

1. Ship `tools/debug-overlay` (FPS, frame time, Actor count, and recent logs) beside the debug agent.
2. Deprecate `tool-debug-menu`, `-api`, and `-noop` for one release, pointing to both replacements; move the Showcase to the overlay
   first.
3. Remove them in a later release.

## Order

Game time first, then the debug agent with its panels and AI tool, the other targets, IntelliJ users, and the debug menu last.
