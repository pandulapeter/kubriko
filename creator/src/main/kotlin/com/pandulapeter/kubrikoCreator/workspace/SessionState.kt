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

/**
 * The state of the workspace's session process.
 */
internal sealed interface SessionState {

    data object Idle : SessionState

    /**
     * Gradle is compiling the project and starting the session process.
     */
    data class Starting(
        val mode: SessionMode,
    ) : SessionState

    /**
     * The session process is up and connected.
     */
    data class Running(
        val mode: SessionMode,
    ) : SessionState
}
