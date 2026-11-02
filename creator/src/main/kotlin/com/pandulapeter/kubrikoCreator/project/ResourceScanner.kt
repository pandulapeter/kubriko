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

    private const val CREATOR_DIRECTORY = ".kubriko"
    private val spriteExtensions = setOf("png", "webp", "jpg", "jpeg", "svg")
    private val soundExtensions = setOf("wav")
    private val musicExtensions = setOf("mp3", "ogg")

    /**
     * @param hiddenDirectory A folder, relative to [rootDirectory], whose files are left out: the generated code of
     *   block-built Actor types, which the Actor types themselves stand for.
     * @param snapshot The files as [ProjectWatcher] just found them, taken with the same [hiddenDirectory], which are
     *   used instead of walking the project again; null to walk it.
     */
    fun scan(
        rootDirectory: File,
        hiddenDirectory: String? = null,
        snapshot: ProjectSnapshot? = null,
    ): Map<ResourceGroup, List<ProjectResource>> = if (snapshot == null) {
        ProjectTree.walk(rootDirectory, skipped = setOf(CREATOR_DIRECTORY), hiddenDirectory = hiddenDirectory)
            .filter { it.isFile }
            .map { it.relativeTo(rootDirectory).invariantSeparatorsPath }
            .groupResources(rootDirectory)
    } else {
        snapshot.stamps.keys.asSequence()
            .filter { path -> path.split('/').dropLast(1).none { it == CREATOR_DIRECTORY } }
            .groupResources(rootDirectory)
    }

    private fun Sequence<String>.groupResources(rootDirectory: File) = mapNotNull { relativePath ->
        classify(relativePath)?.let { group -> ProjectResource(group, File(rootDirectory, relativePath), relativePath) }
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
