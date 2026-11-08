/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors.codegen

import com.pandulapeter.kubrikoCreator.actors.GeneratedNames
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.INDENT
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.stringLiteral

/**
 * The Manager that declares one atlas of every sprite the Actor types are drawn as, so that Kubriko draws the Actors
 * showing any of them in one go: the generated looks show their sprite through it. Generated only into projects with
 * sprites ([PluginManagers.usesSprites]), so that the others don't depend on the sprites plugin. It runs in the Scene
 * editor too, where the sprite plugin's Manager is Kubriko Creator's, which reads the project's images from disk.
 */
internal object SpritesCodeGenerator {

    const val SPRITES_NAME = GeneratedNames.SPRITES

    /**
     * @param sprites The resource accessors of the sprites, packed in this order.
     */
    fun generate(packageName: String, sprites: Collection<String>) = GeneratedFile.withHeader("", buildString {
        appendLine("package $packageName.${ActorTypeCodeGenerator.GENERATED_PACKAGE}")
        appendLine()
        listOf(
            "com.pandulapeter.kubriko.Kubriko",
            "com.pandulapeter.kubriko.helpers.extensions.get",
            "com.pandulapeter.kubriko.manager.Manager",
            "com.pandulapeter.kubriko.sprites.SpriteManager",
            "$packageName.resources.Res",
            "$packageName.resources.allDrawableResources",
        ).forEach { appendLine("import $it") }
        appendLine()
        appendLine(
            """
            |/**
            | * Loads the sprites the Actor types are drawn as onto shared images (as few as fit them), so that Kubriko draws the
            | * Actors showing any of them together. Until they are loaded, those Actors draw nothing.
            | */
            |class $SPRITES_NAME : Manager() {
            |
            |    override fun onInitialize(kubriko: Kubriko) {
            |        kubriko.get<SpriteManager>().loadAtlas(SPRITES.mapNotNull { Res.allDrawableResources[it] })
            |    }
            |
            |    private companion object {
            |        // Looked up by name rather than through their accessors, so the game still compiles once an image is gone.
            |        val SPRITES = listOf(
            """.trimMargin(),
        )
        sprites.forEach { appendLine("$INDENT$INDENT$INDENT${stringLiteral(it)},") }
        appendLine("$INDENT$INDENT)")
        appendLine("    }")
        appendLine("}")
    })
}
