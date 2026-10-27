/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

/**
 * What a block-built type becomes: an Actor type, whose Actors are placed in Scenes, or a Manager type, one instance
 * of which holds game-wide state (like the score) for the whole game.
 */
internal enum class BlockTypeKind {
    ACTOR,
    MANAGER,
}
