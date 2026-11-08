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

import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.project.ProjectTree
import com.pandulapeter.kubrikoCreator.screenplay.PageImages
import com.pandulapeter.kubrikoCreator.screenplay.PageLinks
import com.pandulapeter.kubrikoCreator.screenplay.PageNaming
import java.io.File

/**
 * Works out what about the screenplay's images is worth a look: images among the pages no page shows or links to, and
 * images pages show whose file is gone.
 */
internal object PageImageChecks {

    /**
     * @param pages Every page's text, by its name.
     * @param imageFiles The image files among the pages, by their paths from the project's root.
     * @param exists Whether a file is at a path from the project's root.
     */
    fun of(pages: Map<String, String>, imageFiles: Collection<String>, exists: (String) -> Boolean): List<HealthFinding> {
        val linked = HashSet<String>()
        val missing = ArrayList<HealthFinding>()
        pages.toSortedMap().forEach { (name, text) ->
            PageLinks.linksOf(text).forEach { link ->
                val path = PageLinks.resolve(PageNaming.pathOf(name), link.destination) ?: return@forEach
                linked += path
                if (link.isImage && !exists(path)) missing += HealthFinding(HealthFinding.Kind.PAGE_IMAGE_MISSING, RelationSubject.Page(name), usage = path)
            }
        }
        val unused = imageFiles.filter { it !in linked }.sorted().map { HealthFinding(HealthFinding.Kind.UNUSED_PAGE_IMAGE, RelationSubject.Image(it)) }
        return unused + missing.distinct()
    }
}

/**
 * What about the open project's screenplay images is worth a look ([PageImageChecks]). Reads the disk.
 */
internal fun CommandContext.pageImageFindings(): List<HealthFinding> {
    val root = project.rootDirectory
    val folder = File(root, PageNaming.FOLDER).takeIf { it.isDirectory } ?: return emptyList()
    val pages = screenplay.scanNames().mapNotNull { name -> screenplay.read(name)?.let { name to it } }.toMap()
    val images = ProjectTree.walk(folder).filter { it.isFile && it.extension.lowercase() in PageImages.EXTENSIONS }.map { it.relativeTo(root).invariantSeparatorsPath }.toList()
    return PageImageChecks.of(pages, images) { File(root, it).exists() }
}
