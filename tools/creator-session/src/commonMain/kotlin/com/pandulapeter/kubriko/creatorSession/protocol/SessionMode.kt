/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.creatorSession.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * What a session process hosts.
 */
@Serializable
enum class SessionMode {

    /**
     * The Scene editor, with the project's placeable Actor types.
     */
    @SerialName("edit")
    EDIT,

    /**
     * The game itself, started through its own main function.
     */
    @SerialName("play")
    PLAY,
}
