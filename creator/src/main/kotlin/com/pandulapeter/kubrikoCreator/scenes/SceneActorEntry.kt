/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.scenes

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One Actor of a Scene file, as Kubriko's serialization plugin writes it.
 *
 * @param typeId Which Actor type it is: the name of a block-built Actor type, or the type ID a hand-written one registers.
 * @param state The Actor's saved state, as JSON.
 */
@Serializable
internal data class SceneActorEntry(
    @SerialName("typeId") val typeId: String,
    @SerialName("state") val state: String,
)
