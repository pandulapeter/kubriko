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
import com.pandulapeter.kubrikoCreator.commands.screenplay.markdownTexts
import com.pandulapeter.kubrikoCreator.project.ProjectTree
import com.pandulapeter.kubrikoCreator.screenplay.PageImages
import com.pandulapeter.kubrikoCreator.screenplay.PageLinks
import com.pandulapeter.kubrikoCreator.screenplay.PageNaming
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
 * What about the open project's screenplay images is worth a look ([PageImageChecks]), worked out again only once a page
 * or document changed, since every change Kubriko Creator or anyone else makes to a page's images changes one of them too.
 * Reads the disk.
 */
internal fun CommandContext.pageImageFindings(): List<HealthFinding> = pageImageFindingsCache.get(screenplay.revision.value to documents.revision.value) {
    val root = project.rootDirectory
    val folder = File(root, PageNaming.FOLDER).takeIf { it.isDirectory } ?: return@get emptyList()
    val images = ProjectTree.walk(folder).filter { it.isFile && it.extension.lowercase() in PageImages.EXTENSIONS }.map { it.relativeTo(root).invariantSeparatorsPath }.toList()
    PageImageChecks.of(markdownTexts(), images) { File(root, it).exists() }
}
