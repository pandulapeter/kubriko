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
 * The command line of a session process: `edit <scene folder> [scene file]` or `play <main class>`.
 */
internal sealed interface SessionArguments {

    val mode: SessionMode

    data class Edit(
        val sceneFolderPath: String,
        val sceneFileName: String?,
    ) : SessionArguments {
        override val mode get() = SessionMode.EDIT
    }

    data class Play(
        val mainClassName: String,
    ) : SessionArguments {
        override val mode get() = SessionMode.PLAY
    }

    companion object {

        fun parse(arguments: List<String>): SessionArguments = when (arguments.firstOrNull()) {
            "edit" -> Edit(
                sceneFolderPath = arguments.getOrNull(1) ?: throw IllegalArgumentException("Edit mode needs a scene folder."),
                sceneFileName = arguments.getOrNull(2),
            )
            "play" -> Play(
                mainClassName = arguments.getOrNull(1) ?: throw IllegalArgumentException("Play mode needs a main class."),
            )
            else -> throw IllegalArgumentException("Unknown mode: ${arguments.firstOrNull()}. Expected edit or play.")
        }
    }
}
