/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One Event of an Actor type and the Blocks that run when it happens.
 *
 * @param key The key of a [EventType.KEY_PRESSED] Event; null for every other type.
 */
@Serializable
internal data class EventDefinition(
    @SerialName("type") val type: EventType,
    @SerialName("key") val key: KeyChoice? = null,
    @SerialName("blocks") val blocks: List<BlockDefinition> = emptyList(),
)
