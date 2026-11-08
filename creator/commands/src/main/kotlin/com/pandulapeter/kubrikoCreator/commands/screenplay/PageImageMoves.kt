/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.screenplay

import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.foundation.moveWithoutReplacing
import com.pandulapeter.kubrikoCreator.screenplay.PageImages
import com.pandulapeter.kubrikoCreator.screenplay.PathMove
import java.io.File
import java.nio.file.Files

/**
 * Moves the page [page]'s own images ([PageImages.ownImagesOf]) next to [newPage], renamed after it, as the page itself
 * was moved; one whose new name another file has, or that can't be moved, stays where it is, and its links still find it.
 *
 * @return The moves made.
 */
@EditorMutation
internal fun CommandContext.movePageImages(page: String, newPage: String): List<PathMove> {
    val root = project.rootDirectory
    return PageImages.ownImagesOf(root, page).mapNotNull { path ->
        val newPath = PageImages.movedImagePathOf(path, page, newPage)
        val source = File(root, path)
        val target = File(root, newPath)
        val isTaken = target.exists() && !Files.isSameFile(source.toPath(), target.toPath())
        if (newPath == path || isTaken || !source.moveWithoutReplacing(target)) return@mapNotNull null
        scenes.ownWrites.record(source)
        scenes.ownWrites.record(target)
        PathMove(path, newPath)
    }
}

/**
 * The page [page]'s own images ([PageImages.ownImagesOf]) that no other page shows or links to, which deleting it offers
 * to delete too. Reads the disk, so it runs off the main thread.
 */
public fun CommandContext.unusedImagesOf(page: String): List<String> {
    val others = screenplay.scanNames().filter { it != page }.mapNotNull { name -> screenplay.read(name)?.let { name to it } }.toMap()
    return PageImages.unusedImagesOf(project.rootDirectory, page, others)
}
