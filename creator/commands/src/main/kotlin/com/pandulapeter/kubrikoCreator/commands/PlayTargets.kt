/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands

/**
 * Where the game can be played right now, and where Play plays it.
 *
 * @param available The desktop first, then every other place the project is built for and this computer can run it on.
 * @param chosen The one of [available] the user chose last, or the desktop when that one isn't there now.
 * @param configured The kinds the project is built for that this computer can play, whether or not a place of them is
 *   there now (an Android target with the SDK but no device), so that the way to set one up can be offered.
 */
public data class PlayTargets(
    val available: List<PlayTarget>,
    val chosen: PlayTarget,
    val configured: Set<PlayTarget.Kind> = setOf(PlayTarget.Kind.DESKTOP),
)
