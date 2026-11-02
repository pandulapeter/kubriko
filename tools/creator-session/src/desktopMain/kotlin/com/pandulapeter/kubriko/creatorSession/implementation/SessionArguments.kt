/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.creatorSession.implementation

import com.pandulapeter.kubriko.creatorSession.protocol.SessionMode

/**
 * The command line of a session process: `play <main class>` (or `edit`, which is refused).
 */
internal sealed interface SessionArguments {

    val mode: SessionMode

    /**
     * Edit mode, which this version no longer hosts: Kubriko Creator edits Scenes in its own window.
     */
    data object Edit : SessionArguments {
        override val mode get() = SessionMode.EDIT
    }

    data class Play(
        val mainClassName: String,
    ) : SessionArguments {
        override val mode get() = SessionMode.PLAY
    }

    companion object {

        fun parse(arguments: List<String>): SessionArguments = when (arguments.firstOrNull()) {
            "edit" -> Edit
            "play" -> Play(
                mainClassName = arguments.getOrNull(1) ?: throw IllegalArgumentException("Play mode needs a main class."),
            )
            else -> throw IllegalArgumentException("Unknown mode: ${arguments.firstOrNull()}. Expected play.")
        }
    }
}
