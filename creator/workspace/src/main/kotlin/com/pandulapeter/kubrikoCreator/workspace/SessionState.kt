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

import com.pandulapeter.kubriko.creatorSession.protocol.SessionMode
import com.pandulapeter.kubrikoCreator.commands.PlayTarget

/**
 * The state of the workspace's session process.
 */
public sealed interface SessionState {

    /**
     * No session process runs.
     */
    public data object Idle : SessionState

    /**
     * Gradle is compiling the project and starting the session process, or the game on [target].
     */
    public data class Starting(
        public val mode: SessionMode,
        public val target: PlayTarget = PlayTarget.DESKTOP,
    ) : SessionState

    /**
     * The session process is up and connected, or the game is up on [target], which only the desktop's session reaches.
     */
    public data class Running(
        public val mode: SessionMode,
        public val target: PlayTarget = PlayTarget.DESKTOP,
    ) : SessionState
}

/**
 * Where this session's game plays, or null when none does.
 */
public val SessionState.playTarget: PlayTarget?
    get() = when (this) {
        SessionState.Idle -> null
        is SessionState.Starting -> target
        is SessionState.Running -> target
    }
