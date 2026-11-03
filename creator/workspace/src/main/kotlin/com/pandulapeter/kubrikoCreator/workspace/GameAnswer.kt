/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.workspace

import com.pandulapeter.kubriko.creatorSession.protocol.SessionMessage

/**
 * What became of a command sent to the running game.
 */
internal sealed interface GameAnswer {

    data class Answered(
        val message: SessionMessage,
    ) : GameAnswer

    /**
     * No game is running (a Scene being edited in a session window doesn't count).
     */
    data object NotPlaying : GameAnswer

    /**
     * The game runs on a Kubriko version whose session can't take commands.
     */
    data object Unsupported : GameAnswer

    data object NoAnswer : GameAnswer
}
