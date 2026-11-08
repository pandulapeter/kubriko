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

import com.pandulapeter.kubrikoCreator.actors.DefinitionPaths
import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.CompositeCommand
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.console.ConsoleEntry
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.screenplay.PageLinks
import com.pandulapeter.kubrikoCreator.screenplay.PageNaming
import com.pandulapeter.kubrikoCreator.screenplay.PathMove
import com.pandulapeter.kubrikoCreator.screenplay.applyTo
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.File

/**
 * [applied], the result of a change that made [moves], once every page's mentions and images of what moved point at where
 * it is now, as do the links of every page and document that moved itself. Undoing it moves the things back and then
 * writes those files back as they were ([RestoreMarkdown]), so the undo is exact; the AI hears which ones changed.
 *
 * A page that can't be written leaves every link as it was, and the console says so: the move itself stays made.
 */
@EditorMutation
internal suspend fun CommandContext.withMentionsFollowed(moves: List<PathMove>, applied: Applied): Applied {
    if (applied.failure != null) return applied
    val made = moves.filter { it.from != it.to }
    if (made.isEmpty()) return applied
    val previous = withContext(ioDispatcher) { followMentions(made) } ?: return applied
    if (previous.isEmpty()) return applied
    val restoring = Invocation(RestoreMarkdown, RestoreMarkdown.Input(previous))
    val followedIn = previous.keys.map { (made.applyTo(it) ?: it) }
    return applied.copy(
        inverse = applied.inverse?.let { Invocation(CompositeCommand, CompositeCommand.Input(listOf(it, restoring))) },
        details = JsonObject(applied.details.orEmpty() + (MENTIONS_FOLLOWED_IN to JsonArray(followedIn.map(::JsonPrimitive)))),
    )
}

/**
 * The text each changed file had, by where it was before [moves]; or null if one couldn't be written, after every file
 * written so far is back as it was.
 */
@EditorMutation
private fun CommandContext.followMentions(moves: List<PathMove>): Map<String, String>? {
    val reversed = moves.map(PathMove::reversed)
    val movedDocuments = moves.mapNotNull { move ->
        move.to.takeIf { it.endsWith(".${PageNaming.EXTENSION}") && pageOf(it) == null && documents.fileOf(it).isFile }
    }
    val paths = (screenplay.scanNames().map(PageNaming::pathOf) + movedDocuments).distinct()
    // A page that holds no move's name can't mention what moved, so it isn't parsed. Letters and digits survive however
    // a destination is escaped or encoded, so the longest run of them is looked for; a name without one is never skipped.
    val needles = moves.map { move -> asciiRun.findAll(move.from.substringAfterLast('/').substringBeforeLast('.')).maxByOrNull { it.value.length }?.value }
        .takeIf { null !in it }?.filterNotNull()
    val previous = LinkedHashMap<String, String>()
    val written = LinkedHashMap<String, String>()
    paths.forEach { path ->
        try {
            val text = readMarkdown(path) ?: return@forEach
            val oldPath = reversed.applyTo(path) ?: path
            if (oldPath == path && needles != null && needles.none { it in text }) return@forEach
            val followed = PageLinks.withMovesFollowed(text, oldPath, moves)
            if (followed != text) {
                writeMarkdown(path, followed)
                previous[oldPath] = text
                written[path] = text
            }
        } catch (exception: Exception) {
            written.forEach { (writtenPath, text) -> runCatching { writeMarkdown(writtenPath, text) } }
            report(ConsoleEntry.Status(CommandProblem.MENTIONS_NOT_FOLLOWED, isError = true, arguments = listOf(path, exception.message ?: exception.toString())))
            return null
        }
    }
    return previous
}

/**
 * How renaming the type [type] to [newName], or moving it into [newFolder], moves its definition file; none for a type
 * there is no definition of.
 */
internal fun CommandContext.typeMoves(type: String, newName: String = type, newFolder: String? = null): List<PathMove> {
    val definition = blockTypes?.get(type) ?: return emptyList()
    return listOf(PathMove(DefinitionPaths.definitionPath(definition), DefinitionPaths.definitionPath(definition.kind, newFolder ?: definition.folder, newName)))
}

/**
 * The path of [file] from the project's root, as mentions name it.
 */
internal fun CommandContext.projectPathOf(file: File): String = file.relativeTo(project.rootDirectory).invariantSeparatorsPath

private val asciiRun = Regex("[A-Za-z0-9]+")

/**
 * The key of a change's result that names the pages whose links followed it.
 */
internal const val MENTIONS_FOLLOWED_IN = "mentionsFollowedIn"
