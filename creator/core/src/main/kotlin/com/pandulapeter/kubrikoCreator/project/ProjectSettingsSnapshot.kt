/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.project

import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.scenes.SceneStore
import com.pandulapeter.kubrikoCreator.screenplay.ScreenplaySettings
import java.io.File

/**
 * Everything Project settings shows, read at once.
 *
 * @param identity The game's identity, or null if its build doesn't read one.
 * @param game The game's settings, or null if its build doesn't read them.
 * @param scenes The project's Scenes, which the game can start with.
 * @param screenplay How the project's screenplay is kept.
 */
public data class ProjectSettingsSnapshot(
    val identity: GameIdentity?,
    val game: GameSettingsReading?,
    val libraries: List<LibraryInUse>,
    val scenes: List<String>,
    val screenplay: ScreenplaySettings = ScreenplaySettings(),
) {
    public companion object {

        /**
         * The settings of the project in [rootDirectory], whose Block-built types are [definitions]. Reads its files, so
         * it is called off the main thread.
         */
        public fun read(rootDirectory: File, definitions: List<BlockTypeDefinition>, scenes: SceneStore): ProjectSettingsSnapshot =
            readSettings(rootDirectory, scenes).copy(libraries = LibraryInUse.read(rootDirectory, definitions, scenes))

        /**
         * The settings of the project in [rootDirectory] without its [libraries], which take reading every source.
         */
        public fun readSettings(rootDirectory: File, scenes: SceneStore): ProjectSettingsSnapshot = ProjectSettingsSnapshot(
            identity = GameIdentity.read(rootDirectory),
            game = GameSettings.read(rootDirectory),
            libraries = emptyList(),
            scenes = scenes.names(),
            screenplay = ScreenplaySettings.read(rootDirectory),
        )
    }
}
