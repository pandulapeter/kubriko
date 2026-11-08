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
import com.pandulapeter.kubrikoCreator.screenplay.MentionIndex
import com.pandulapeter.kubrikoCreator.screenplay.PageImages
import com.pandulapeter.kubrikoCreator.screenplay.PageNaming
import java.io.File

/**
 * The project's Markdown as last read: every page's and document's text, and the images among the pages.
 *
 * @param markdown The text of every page and document, by its path from the project's root.
 * @param images The image files among the pages, by their paths from the project's root.
 */
public class ScreenplayTexts(
    public val markdown: Map<String, String>,
    public val images: List<String>,
) {
    /**
     * Every page's text, by its name.
     */
    public val pages: Map<String, String> = markdown.mapNotNull { (path, text) -> PageNaming.nameOf(path)?.let { it to text } }.toMap()

    /**
     * Which pages mention what.
     */
    public val mentionIndex: MentionIndex by lazy { MentionIndex.of(pages) }
}

/**
 * The project's Markdown ([ScreenplayTexts]), read again only once a page or document changed: every write Kubriko Creator
 * makes to a page, a document, or an image among the pages raises their revision, and so does a change from outside. Reads
 * the disk.
 */
public fun CommandContext.screenplayTexts(): ScreenplayTexts = screenplayTextsCache.get(screenplay.revision.value to documents.revision.value) {
    val root = project.rootDirectory
    val folder = File(root, PageNaming.FOLDER)
    val images = if (!folder.isDirectory) emptyList() else ProjectTree.walk(folder).filter { it.isFile && it.extension.lowercase() in PageImages.EXTENSIONS }.map { it.relativeTo(root).invariantSeparatorsPath }.toList()
    ScreenplayTexts(markdownTexts(), images)
}

/**
 * Which pages mention what ([MentionIndex]), as [screenplayTexts] last read them. Reads the disk.
 */
public fun CommandContext.mentionIndex(): MentionIndex = screenplayTexts().mentionIndex
