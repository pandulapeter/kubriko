/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.context

import com.pandulapeter.kubrikoCreator.actors.DefinitionPaths
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.screenplay.markdownTexts
import com.pandulapeter.kubrikoCreator.commands.screenplay.projectPathOf
import com.pandulapeter.kubrikoCreator.project.ProjectTree
import com.pandulapeter.kubrikoCreator.screenplay.PageImages
import com.pandulapeter.kubrikoCreator.screenplay.PageLinks
import com.pandulapeter.kubrikoCreator.screenplay.PageNaming
import com.pandulapeter.kubrikoCreator.screenplay.ScreenplaySettings
import java.io.File

/**
 * Works out what about the screenplay's images is worth a look: images among the pages no page or document shows or
 * links to, and images pages show whose file is gone.
 */
internal object PageImageChecks {

    /**
     * @param files The text of every page and document, by its path from the project's root.
     * @param imageFiles The image files among the pages, by their paths from the project's root.
     * @param exists Whether a file is at a path from the project's root.
     */
    fun of(files: Map<String, String>, imageFiles: Collection<String>, exists: (String) -> Boolean): List<HealthFinding> {
        val linked = HashSet<String>()
        val missing = ArrayList<HealthFinding>()
        files.toSortedMap().forEach { (path, text) ->
            val page = PageNaming.nameOf(path)
            PageLinks.linksOf(text).forEach { link ->
                val target = PageLinks.resolve(path, link.destination) ?: return@forEach
                linked += target
                if (page != null && link.isImage && !exists(target)) missing += HealthFinding(HealthFinding.Kind.PAGE_IMAGE_MISSING, RelationSubject.Page(page), usage = target)
            }
        }
        val unused = imageFiles.filter { it !in linked }.sorted().map { HealthFinding(HealthFinding.Kind.UNUSED_PAGE_IMAGE, RelationSubject.Image(it)) }
        return unused + missing.distinct()
    }
}

/**
 * What about the open project's screenplay is worth a look: its images ([PageImageChecks]) and its pages ([ScreenplayChecks]),
 * worked out again only once a page, a document, a type, or a Scene changed, since every change Kubriko Creator or anyone
 * else makes to a page's images changes a page too. Reads the disk.
 */
internal fun CommandContext.screenplayFindings(): List<HealthFinding> {
    val types = blockTypes?.types?.value.orEmpty()
    val sceneNames = scenes.names()
    val key = listOf(screenplay.revision.value, documents.revision.value, types, sceneNames, ScreenplaySettings.read(project.rootDirectory))
    return screenplayFindingsCache.get(key) {
        val root = project.rootDirectory
        val pages = screenplay.scanNames().mapNotNull { name -> screenplay.read(name)?.let { name to it } }.toMap()
        val subjects = LinkedHashMap<RelationSubject, String>()
        types.sortedBy { it.name.lowercase() }.forEach { subjects[RelationSubject.Type(it.name)] = DefinitionPaths.definitionPath(it) }
        sceneNames.sorted().forEach { scene -> subjects[RelationSubject.Scene(scene)] = projectPathOf(scenes.fileOf(scene)) }
        val exists = { path: String -> File(root, path).exists() }
        val folder = File(root, PageNaming.FOLDER)
        val images = if (!folder.isDirectory) emptyList() else ProjectTree.walk(folder).filter { it.isFile && it.extension.lowercase() in PageImages.EXTENSIONS }.map { it.relativeTo(root).invariantSeparatorsPath }.toList()
        PageImageChecks.of(markdownTexts(), images, exists) + ScreenplayChecks.of(pages, subjects, exists, ScreenplaySettings.read(root).areRemindersShown)
    }
}
