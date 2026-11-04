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

import com.pandulapeter.kubrikoCreator.actors.codegen.ActorTypeCodeGenerator
import com.pandulapeter.kubrikoCreator.actors.codegen.ManagerTypeCodeGenerator
import com.pandulapeter.kubrikoCreator.project.GameSettings

/**
 * What the [TemplateStep]s of one upgrade check share: where the template's files are in the project, and how to read them.
 *
 * @param reader A project file's text with Unix line breaks, or null if it doesn't exist.
 * @param exists Whether a project file exists, for one that isn't text.
 * @param projectName The name of the project's folder, which names the game when no title does.
 */
internal class TemplateContext(
    val packageName: String,
    sourceDirectory: String,
    private val reader: (String) -> String?,
    private val exists: (String) -> Boolean = { reader(it) != null },
    projectName: () -> String,
) {
    val packagePath = "$sourceDirectory/${packageName.replace('.', '/')}"
    val gamePath = "$packagePath/$GAME_FILE"
    val entryPointPath = "$packagePath/$ENTRY_POINT_FILE"
    val sceneLoaderPath = "$packagePath/$SCENE_LOADER_FILE"
    val mainPath = "${sourceDirectory.replace("commonMain", "desktopMain")}/${packageName.replace('.', '/')}/$MAIN_FILE"
    val webMainPath = "${sourceDirectory.replace("commonMain", "wasmJsMain")}/${packageName.replace('.', '/')}/$MAIN_FILE"
    val androidActivityPath = "${sourceDirectory.replace("commonMain", "androidMain")}/${packageName.replace('.', '/')}/$ANDROID_ACTIVITY_FILE"
    val iosViewControllerPath = "${sourceDirectory.replace("commonMain", "iosMain")}/${packageName.replace('.', '/')}/$IOS_VIEW_CONTROLLER_FILE"
    val pagePath = "${sourceDirectory.replace("commonMain", "wasmJsMain").removeSuffix("/kotlin")}/resources/$PAGE_FILE"
    val webFontPath = "${sourceDirectory.replace("commonMain", "wasmJsMain").removeSuffix("/kotlin")}/composeResources/font/${WebFontUpgrade.FONT_FILE}"
    val wiringImport = "import $packageName.${ActorTypeCodeGenerator.GENERATED_PACKAGE}.${ManagerTypeCodeGenerator.WIRING_NAME}"

    /**
     * The game's name: the desktop window's title, the web page's, or the project's, as the files are before any change.
     */
    val gameName by lazy { read(mainPath)?.let(GameIdentityUpgrade::windowTitleOf) ?: read(pagePath)?.let(GameIdentityUpgrade::pageTitleOf) ?: projectName() }

    /**
     * The Scene the game starts with, as `Game.kt` names it before any change, or the default one when it names none.
     */
    val firstScene by lazy { read(gamePath)?.let(GameSettingsUpgrade::firstSceneOf) ?: GameSettings.DEFAULT_FIRST_SCENE }

    fun read(path: String) = reader(path)

    fun exists(path: String) = exists.invoke(path)

    private companion object {
        const val GAME_FILE = "Game.kt"
        const val ENTRY_POINT_FILE = "GameEntryPoint.kt"
        const val SCENE_LOADER_FILE = "SceneLoader.kt"
        const val MAIN_FILE = "Main.kt"
        const val PAGE_FILE = "index.html"
        const val ANDROID_ACTIVITY_FILE = "MainActivity.kt"
        const val IOS_VIEW_CONTROLLER_FILE = "MainViewController.kt"
    }
}
