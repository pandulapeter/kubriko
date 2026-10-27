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

/**
 * The part of the open Actor type the AI just worked on, which the editor selects and lights up.
 *
 * @param key Changes with every step, so that the same part lights up again when it is worked on again.
 * @param eventIndex The Event, or null for the Actor type as a whole.
 * @param blockIndex The Block of that Event, or null for the Event as a whole.
 */
internal data class ActorTypeHighlight(
    val key: Any,
    val eventIndex: Int?,
    val blockIndex: Int?,
)
