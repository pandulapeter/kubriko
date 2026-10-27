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

import java.io.File

/**
 * Finds the files of a project that belong in the resource tree. The tree is a view of plain files, so this is the
 * only place that decides what counts as a Scene, a sprite, or a sound.
 */
internal object ResourceScanner {

    private val ignoredDirectoryNames = setOf("build", ".gradle", ".git", ".idea", ".kotlin", ".kubriko", "kotlin-js-store", "node_modules")
    private val spriteExtensions = setOf("png", "webp", "jpg", "jpeg", "svg")
    private val soundExtensions = setOf("wav")
    private val musicExtensions = setOf("mp3", "ogg")

    /**
     * @param hiddenDirectory A folder, relative to [rootDirectory], whose files are left out: the generated code of
     *   block-built Actor types, which the Actor types themselves stand for.
     */
    fun scan(rootDirectory: File, hiddenDirectory: String? = null): Map<ResourceGroup, List<ProjectResource>> = rootDirectory.walkTopDown()
        .onEnter { it == rootDirectory || (it.name !in ignoredDirectoryNames && it.relativeTo(rootDirectory).invariantSeparatorsPath != hiddenDirectory) }
        .filter { it.isFile }
        .mapNotNull { file ->
            val relativePath = file.relativeTo(rootDirectory).invariantSeparatorsPath
            classify(relativePath)?.let { group -> ProjectResource(group, file, relativePath) }
        }
        .sortedBy { it.relativePath }
        .groupBy { it.group }

    internal fun classify(relativePath: String): ResourceGroup? {
        val segments = relativePath.split('/')
        val extension = relativePath.substringAfterLast('.', "").lowercase()
        val isComposeResource = "composeResources" in segments
        return when {
            extension == "kt" && "src" in segments -> ResourceGroup.KOTLIN
            !isComposeResource -> null
            extension == "json" && "scenes" in segments -> ResourceGroup.SCENES
            extension in spriteExtensions -> ResourceGroup.SPRITES
            extension in soundExtensions -> ResourceGroup.SOUNDS
            extension in musicExtensions -> ResourceGroup.MUSIC
            else -> null
        }
    }

    /**
     * The source set a file belongs to (`desktopMain` for `src/desktopMain/kotlin/Main.kt`), which tells apart files of
     * the same name, or null outside a source folder.
     */
    fun sourceSetOf(resource: ProjectResource) = resource.relativePath.split('/').let { segments ->
        segments.indexOf("src").takeIf { it >= 0 }?.let { segments.getOrNull(it + 1) }
    }
}
