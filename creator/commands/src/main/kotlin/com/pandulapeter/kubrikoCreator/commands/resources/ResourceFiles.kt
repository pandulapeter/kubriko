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
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.LookupProblem
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.SimilarNames
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import java.io.File

/**
 * The files of the game's own resources the last resource scan found, by their paths from `composeResources`, or null
 * if they aren't known: those of the module new resources go to ([isGameResource]), sprites, sounds, and music, and
 * every other image of `drawable`, which the game would mix up with a sprite of the same name.
 */
internal fun CommandContext.knownResourcePaths(): List<String>? {
    val resources = scannedResources() ?: return fallbackResourcePaths()
    return listOf(ResourceGroup.SPRITES, ResourceGroup.SOUNDS, ResourceGroup.MUSIC)
        .flatMap { resources[it].orEmpty() }
        .filter { isGameResource(it.file) }
        .mapNotNull(::pathFromComposeResources)
        .distinct()
}

/**
 * What the scan knows when only its sprites and audio are given (as in tests that need no more).
 */
private fun CommandContext.fallbackResourcePaths(): List<String>? {
    val sprites = spriteFiles()?.paths ?: return null
    val audio = audioFiles() ?: return null
    return sprites + audio.sounds + audio.music
}

/**
 * Why there's no sprite, sound, or music file at [path] to change, or null if there is one, or the files aren't known.
 */
internal fun CommandContext.validateExistingResource(path: String): Rejection? {
    val known = knownResourcePaths()?.filter { ResourceKind.of(it) != null } ?: return null
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
 * Mac or Windows takes for the same file, or two images of `drawable` (sprites, vector drawables, or any other) with
 * the same resource accessor, which Compose's resources refuse to build.
 */
internal fun areMixedUp(first: String, second: String): Boolean {
    if (first.equals(second, ignoreCase = true)) return true
    val firstAccessor = drawableAccessorOf(first) ?: return false
    return drawableAccessorOf(second)?.equals(firstAccessor, ignoreCase = true) == true
}

private fun drawableAccessorOf(path: String): String? {
    val segments = path.split('/')
    if (segments.size != 2 || segments[0] != DRAWABLE) return null
    return segments[1].substringBeforeLast('.').replace('-', '_')
}

/**
 * The game's own resource files on disk ([isGameResource]) with their paths from `composeResources`: sprites, sounds,
 * music, and the other images of `drawable`. Found on disk, since what the last scan knows may be a second old. Blocks.
 */
internal fun CommandContext.resourceFilesOnDisk(): List<ResourceFile> = File(resourceModuleDirectory(), SOURCES).walkTopDown()
        .filter { it.isFile && isGameResource(it) }
        .mapNotNull { file -> pathFromComposeResources(file.invariantSeparatorsPath)?.let { ResourceFile(it, file) } }
        .toList()

/**
 * A file of the game's own resources, with its [path] from its `composeResources` folder.
 */
internal data class ResourceFile(
    val path: String,
    val file: File,
)

/**
 * The files at [path] among [files] (one per source set that has one).
 */
internal fun List<ResourceFile>.at(path: String): List<File> = filter { it.path == path && ResourceKind.of(it.path) != null }.map { it.file }

/**
 * Whether a file the game would mix up with [path] is among [files], apart from those at [except].
 */
internal fun List<ResourceFile>.isTaken(path: String, except: String? = null): Boolean = any { it.path != except && areMixedUp(it.path, path) }

/**
 * Every file at [path] (one per source set that has one), found on disk. Blocks.
 */
internal fun CommandContext.resourceFilesAt(path: String): List<File> = resourceFilesOnDisk().at(path)

/**
 * The file [file] (at [path] from its `composeResources` folder) becomes at [newPath].
 */
internal fun movedResourceFile(file: File, path: String, newPath: String): File {
    val composeResources = generateSequence(file) { it.parentFile }.drop(path.count { it == '/' } + 1).first()
    return File(composeResources, newPath)
}

/**
 * Whether [file] is one of the game's own resources: in the `composeResources` folder of a source set of the module new
 * resources go to (`src/<source set>/composeResources`), so that a module nested in it, or beside it, whose `Res` is
 * another, is never changed by a command meant for the game's.
 */
internal fun CommandContext.isGameResource(file: File): Boolean {
    val segments = file.absoluteFile.relativeToOrNull(resourceModuleDirectory())?.invariantSeparatorsPath?.split('/') ?: return false
    return segments.size > 3 && segments[0] == SOURCES && segments[2] == ResourceKind.COMPOSE_RESOURCES
}

/**
 * The module the game's own resources are in: the one holding the `composeResources` folder new ones go to.
 */
private fun CommandContext.resourceModuleDirectory(): File = scenes.resourcesDirectory.absoluteFile.parentFile.parentFile.parentFile

private fun pathFromComposeResources(resource: ProjectResource) = pathFromComposeResources(resource.relativePath)

private fun pathFromComposeResources(path: String): String? {
    val marker = "/${ResourceKind.COMPOSE_RESOURCES}/"
    val start = "/$path".lastIndexOf(marker).takeIf { it >= 0 } ?: return null
    return "/$path".substring(start + marker.length).takeIf { ResourceKind.of(it) != null || drawableAccessorOf(it) != null }
}

private const val DRAWABLE = "drawable"
private const val SOURCES = "src"
