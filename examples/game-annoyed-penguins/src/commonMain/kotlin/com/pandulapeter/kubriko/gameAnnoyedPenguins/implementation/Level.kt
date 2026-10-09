/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation

import kotlinx.collections.immutable.toImmutableList

/**
 * A playable level, identified independently of the label the menu shows for it.
 */
internal enum class Level(
    val sceneFileName: String,
) {
    LEVEL_1("level_1.json"),
    LEVEL_2("level_2.json"),
    LEVEL_3("level_3.json"),
    ;

    companion object {
        val AllLevels = entries.toImmutableList()
    }
}
