/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.template

import com.pandulapeter.kubrikoCreator.actors.ActorTypeCodeGenerator
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeCodeGenerator

/**
 * What the [TemplateStep]s of one upgrade check share: where the template's files are in the project, and how to read them.
 *
 * @param reader A project file's text with Unix line breaks, or null if it doesn't exist.
 * @param projectName The name of the project's folder, which names the game when no title does.
 */
internal class TemplateContext(
    val packageName: String,
    sourceDirectory: String,
    private val reader: (String) -> String?,
    projectName: () -> String,
) {
    val packagePath = "$sourceDirectory/${packageName.replace('.', '/')}"
    val gamePath = "$packagePath/$GAME_FILE"
    val entryPointPath = "$packagePath/$ENTRY_POINT_FILE"
    val sceneLoaderPath = "$packagePath/$SCENE_LOADER_FILE"
    val mainPath = "${sourceDirectory.replace("commonMain", "desktopMain")}/${packageName.replace('.', '/')}/$MAIN_FILE"
    val pagePath = "${sourceDirectory.replace("commonMain", "wasmJsMain").removeSuffix("/kotlin")}/resources/$PAGE_FILE"
    val wiringImport = "import $packageName.${ActorTypeCodeGenerator.GENERATED_PACKAGE}.${ManagerTypeCodeGenerator.WIRING_NAME}"

    /**
     * The game's name: the desktop window's title, the web page's, or the project's, as the files are before any change.
     */
    val gameName by lazy { read(mainPath)?.let(GameIdentityUpgrade::windowTitleOf) ?: read(pagePath)?.let(GameIdentityUpgrade::pageTitleOf) ?: projectName() }

    fun read(path: String) = reader(path)

    private companion object {
        const val GAME_FILE = "Game.kt"
        const val ENTRY_POINT_FILE = "GameEntryPoint.kt"
        const val SCENE_LOADER_FILE = "SceneLoader.kt"
        const val MAIN_FILE = "Main.kt"
        const val PAGE_FILE = "index.html"
    }
}
