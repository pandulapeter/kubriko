<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Kubriko Creator tool - Session
The process Kubriko Creator starts to run a game or edit its Scenes. Kubriko Creator adds this module to the game's own desktop classpath through a
Gradle init script, at the game's Kubriko version, so games never declare it.

- `edit <scene folder> [scene file]` opens the Scene editor with the Actor types of the game's `CreatorEntryPoint` (see `tool-creator-api`).
- `play <main class>` runs the game through its own main function.

Either way, the session connects to Kubriko Creator on the localhost port given in the `kubriko.creator.port` system property and streams the
game's log entries to it, one JSON message per line. In play mode it also carries out the commands Kubriko Creator sends back on that connection
(`SessionCommand`, protocol version 2): pressing keys, capturing the game's window, and bringing it to the front.

## Public artifact
The artifact for this module has the following ID:
`io.github.pandulapeter.kubriko:tool-creator-session`
