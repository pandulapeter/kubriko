<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Kubriko Creator tool - API
This module defines `CreatorEntryPoint`, the opt-in contract through which a game hands Kubriko Creator what it cannot discover on its own: the
`SerializationManager` that knows the game's placeable Actor types, and the Managers those Actors need.

Kubriko Creator generates the implementation and its service registration for the projects it manages. Projects that are never opened in Kubriko
Creator do not need this dependency.

## Public artifact
The artifact for this module has the following ID:
`io.github.pandulapeter.kubriko:tool-creator-api`
