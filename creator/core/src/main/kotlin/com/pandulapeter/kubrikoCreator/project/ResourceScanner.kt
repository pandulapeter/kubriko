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

import com.pandulapeter.kubrikoCreator.screenplay.PageNaming
import java.io.File

/**
 * Finds the files of a project that belong in the resource tree. The tree is a view of plain files, so this is the
 * only place that decides what counts as a Scene, a sprite, or a sound.
 */
public object ResourceScanner {

    private const val CREATOR_DIRECTORY = ".kubriko"
    private val spriteExtensions = setOf("png", "webp", "jpg", "jpeg", "svg")
    private val soundExtensions = setOf("wav")
    private val musicExtensions = setOf("mp3", "ogg")
    private val buildFileNames = setOf("build.gradle.kts", "build.gradle")

    /**
     * @param hiddenDirectory A folder, relative to [rootDirectory], whose files are left out: the generated code of
     *   block-built Actor types, which the Actor types themselves stand for.
     * @param snapshot The files as [ProjectWatcher] just found them, taken with the same [hiddenDirectory], which are
     *   used instead of walking the project again; null to walk it.
     */
    public fun scan(
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

    private fun Sequence<String>.groupResources(rootDirectory: File): Map<ResourceGroup, List<ProjectResource>> {
        val paths = toList()
        val modules = paths.mapNotNullTo(HashSet()) { path -> path.takeIf(::isBuildFile)?.let(::parentOf) }
        return paths.mapNotNull { relativePath ->
            classify(relativePath)?.let { group ->
                val file = File(rootDirectory, relativePath)
                val kotlinPackage = if (group == ResourceGroup.KOTLIN) KotlinPackages.of(file, relativePath) else null
                ProjectResource(group, file, relativePath, kotlinPackage, moduleOf(relativePath) { it in modules })
            }
        }
            .sortedBy { it.relativePath }
            .groupBy { it.group }
    }

    /**
     * The Gradle module [relativePath] (a file or a folder of the project in [rootDirectory]) belongs to, as
     * [ProjectResource.module] names it: the nearest folder holding it, or the folder itself, that has a build file.
     * Modules are told by their files rather than asked from Gradle, so the tree never waits for a build, and a module
     * that `settings.gradle.kts` doesn't include (`buildSrc`, an included build) is one too, as it is in an IDE.
     */
    public fun moduleOf(rootDirectory: File, relativePath: String): String = moduleOf(relativePath) { folder ->
        buildFileNames.any { File(File(rootDirectory, folder), it).isFile }
    }

    internal fun isBuildFile(relativePath: String) = relativePath.substringAfterLast('/') in buildFileNames

    private inline fun moduleOf(relativePath: String, isModule: (String) -> Boolean): String {
        var folder = relativePath.trimEnd('/')
        while (folder.isNotEmpty() && !isModule(folder)) folder = parentOf(folder)
        return folder
    }

    private fun parentOf(path: String) = path.substringBeforeLast('/', "")

    /**
     * Whether [relativePath] is a Markdown file of the project that isn't one of the screenplay's pages.
     */
    public fun isDocument(relativePath: String): Boolean =
        relativePath.substringAfterLast('.', "").equals(PageNaming.EXTENSION, ignoreCase = true) && !relativePath.startsWith(PageNaming.FOLDER + "/")

    internal fun classify(relativePath: String): ResourceGroup? {
        val segments = relativePath.split('/')
        val extension = relativePath.substringAfterLast('.', "").lowercase()
        val isComposeResource = "composeResources" in segments
        return when {
            isDocument(relativePath) -> ResourceGroup.DOCUMENTS
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
    public fun sourceSetOf(resource: ProjectResource): String? = resource.relativePath.split('/').let { segments ->
        segments.indexOf("src").takeIf { it >= 0 && it + 1 < segments.lastIndex }?.let { segments[it + 1] }
    }
}
