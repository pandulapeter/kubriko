/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.actorTypes

import com.pandulapeter.kubrikoCreator.actors.AudioFiles

/**
 * What a Property naming another part of the project can be set to: the project's Actor types, and its sound and music
 * files (null while they aren't known yet).
 */
internal data class ReferenceChoices(
    val actorTypeNames: List<String> = emptyList(),
    val audioFiles: AudioFiles? = null,
)
