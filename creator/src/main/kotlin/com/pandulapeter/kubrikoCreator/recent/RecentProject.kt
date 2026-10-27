/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.recent

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File

/**
 * A project opened before, offered on the welcome screen.
 *
 * @param path The project's root folder.
 * @param lastOpened When it was last opened, in milliseconds since the epoch.
 */
@Serializable
internal data class RecentProject(
    @SerialName("path") val path: String,
    @SerialName("name") val name: String,
    @SerialName("lastOpened") val lastOpened: Long,
) {
    val directory get() = File(path)
}
