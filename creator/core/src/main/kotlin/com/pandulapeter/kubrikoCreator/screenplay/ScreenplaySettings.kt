/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.screenplay

import com.pandulapeter.kubrikoCreator.actors.DefinitionPaths
import com.pandulapeter.kubrikoCreator.foundation.JsonFileStore
import com.pandulapeter.kubrikoCreator.foundation.valueOrNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File

/**
 * How the project's screenplay is kept, in `.kubriko/screenplay.json`, a file of its own rather than `project.json`, so
 * that an older Kubriko Creator, which doesn't know it, just keeps reminding. A project keeping the defaults has no file.
 *
 * @param areRemindersShown Whether the health checks and the commit form remind about pages that don't keep up with the
 *   game, as they do for a new project.
 */
@Serializable
public data class ScreenplaySettings(
    @SerialName("reminders") val areRemindersShown: Boolean = true,
) {
    public companion object {
        public const val FILE: String = "${DefinitionPaths.CREATOR_DIRECTORY}/screenplay.json"

        /**
         * Where the settings of the project in [rootDirectory] are kept.
         */
        public fun store(rootDirectory: File): JsonFileStore<ScreenplaySettings> = JsonFileStore(File(rootDirectory, FILE), serializer())

        /**
         * The settings of the project in [rootDirectory]; the defaults when it has none, or they can't be read. Blocks.
         */
        public fun read(rootDirectory: File): ScreenplaySettings = store(rootDirectory).read().valueOrNull() ?: ScreenplaySettings()
    }
}
