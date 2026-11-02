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
The process Kubriko Creator starts to run a game. Kubriko Creator adds this module to the game's own desktop classpath through a
Gradle init script, at the game's Kubriko version, so games never declare it.

`play <main class>` runs the game through its own main function; `edit` (the Scene editor, which Kubriko Creator now has in its own window) is
answered with a failure.

The session connects to Kubriko Creator on the localhost port given in the `kubriko.creator.port` system property and streams the
game's log entries to it, one JSON message per line. It also carries out the commands Kubriko Creator sends back on that connection
(`SessionCommand`): pressing keys, capturing the game's window, bringing it to the front, and waiting until the game has drawn its first Actors.

The protocol's types (`SessionProtocol`, `SessionMessage`, `SessionCommand`, `SessionMode`) are common code, so the module builds for every
Kubriko target, though the session itself only runs on Desktop. The protocol only grows, so a session can be older or newer than Kubriko Creator: each side skips what it doesn't know, a command the session
doesn't know is answered with a failure, and both use only what the lower of their `SessionProtocol.VERSION`s has.

## Public artifact
The artifact for this module has the following ID:
`io.github.pandulapeter.kubriko:tool-creator-session`
