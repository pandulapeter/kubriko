/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.resources

import com.pandulapeter.kubrikoCreator.actors.ResourceKind
import com.pandulapeter.kubrikoCreator.actors.SpriteFiles
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.LookupProblem
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.SimilarNames
import com.pandulapeter.kubrikoCreator.project.ProjectTree
import java.io.File

/**
 * The sprites, sounds, and music of the project as the last resource scan found them, by their paths from
 * `composeResources`, or null if they aren't known.
 */
internal fun CommandContext.knownResourcePaths(): List<String>? {
    val sprites = spriteFiles()?.paths ?: return null
    val audio = audioFiles() ?: return null
    return sprites + audio.sounds + audio.music
}

/**
 * Why there's no sprite, sound, or music file at [path] to change, or null if there is one, or the files aren't known.
 */
internal fun CommandContext.validateExistingResource(path: String): Rejection? {
    val known = knownResourcePaths() ?: return null
    return if (ResourceKind.of(path) == null || path !in known) {
        SimilarNames.rejection(LookupProblem.RESOURCE_MISSING, LookupProblem.RESOURCE_MISSING_SIMILAR, path, known, path)
    } else {
        null
    }
}

/**
 * Why [path] can't be a new sprite, sound, or music file of the project, as the last resource scan knows them: it names
 * no such file, its name breaks the rules, or the game would mix it up with a file there, apart from [except] (the one
 * being renamed). Reads nothing.
 */
internal fun CommandContext.validateNewResource(path: String, except: String? = null): Rejection? {
    if (ResourceKind.of(path) == null) return Rejection(CommandProblem.RESOURCE_PATH_INVALID, path)
    ResourceKind.validate(path)?.let { return Rejection(it) }
    return if (knownResourcePaths().orEmpty().any { it != except && areMixedUp(it, path) }) Rejection(CommandProblem.RESOURCE_TAKEN, path) else null
}

/**
 * Whether the game would mix up the files at [first] and [second]: one path differing only in capital letters, which a
 * Mac or Windows takes for the same file, or two sprites with the same resource accessor.
 */
internal fun areMixedUp(first: String, second: String): Boolean {
    if (first.equals(second, ignoreCase = true)) return true
    val firstAccessor = SpriteFiles.accessorOf(first) ?: return false
    return SpriteFiles.accessorOf(second)?.equals(firstAccessor, ignoreCase = true) == true
}

/**
 * Every file of the project whose path from its `composeResources` folder is [path] (one per source set that has one),
 * found on disk, since what the last scan knows may be a second old. Blocks.
 */
internal fun CommandContext.resourceFilesAt(path: String): List<File> = resourceFiles().filter { (relative, _) -> relative == path }.map { it.second }

/**
 * Whether a file the game would mix up with [path] is on disk, apart from those at [except]. Blocks.
 */
internal fun CommandContext.isResourceTakenOnDisk(path: String, except: String? = null): Boolean =
    resourceFiles().any { (relative, _) -> relative != except && areMixedUp(relative, path) }

/**
 * The file [file] (at [path] from its `composeResources` folder) becomes at [newPath].
 */
internal fun movedResourceFile(file: File, path: String, newPath: String): File {
    val composeResources = generateSequence(file) { it.parentFile }.drop(path.count { it == '/' } + 1).first()
    return File(composeResources, newPath)
}

private fun CommandContext.resourceFiles(): List<Pair<String, File>> {
    val marker = "/${ResourceKind.COMPOSE_RESOURCES}/"
    return ProjectTree.walk(project.rootDirectory)
        .filter { it.isFile }
        .mapNotNull { file ->
            val path = "/" + file.relativeTo(project.rootDirectory).invariantSeparatorsPath
            val start = path.lastIndexOf(marker).takeIf { it >= 0 } ?: return@mapNotNull null
            path.substring(start + marker.length).takeIf { ResourceKind.of(it) != null }?.let { it to file }
        }
        .toList()
}
