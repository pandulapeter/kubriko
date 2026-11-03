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

import com.pandulapeter.kubrikoCreator.commands.PlayTarget

/**
 * What a source of places to play found: its [kind], if the project is built for it and this computer can play it, and the
 * places of it there now, which may be none.
 */
internal data class FoundPlaces(
    val kind: PlayTarget.Kind?,
    val targets: List<PlayTarget>,
) {
    companion object {
        val NONE = FoundPlaces(kind = null, targets = emptyList())
    }
}
