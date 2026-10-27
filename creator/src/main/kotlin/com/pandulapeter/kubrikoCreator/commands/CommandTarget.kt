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
 * What a command reads or changes, so that the editor can show it while the AI works on it.
 */
internal sealed interface CommandTarget {

    /**
     * An Actor type or Manager type.
     *
     * @param eventIndex The Event the command is about, if it is about one.
     * @param blockIndex The Block of that Event the command is about, if it is about one.
     */
    data class BlockType(
        val name: String,
        val eventIndex: Int? = null,
        val blockIndex: Int? = null,
    ) : CommandTarget

    data class Scene(
        val name: String,
    ) : CommandTarget

    data object VersionControl : CommandTarget

    data object Game : CommandTarget
}
