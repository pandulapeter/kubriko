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
import com.pandulapeter.kubrikoCreator.actors.withResourceRenamed
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.ReplaceScenes
import com.pandulapeter.kubrikoCreator.commands.ReplaceTypes
import com.pandulapeter.kubrikoCreator.console.ConsoleEntry
import com.pandulapeter.kubrikoCreator.context.RelationKind
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import com.pandulapeter.kubrikoCreator.context.handWrittenKotlin
import com.pandulapeter.kubrikoCreator.context.resourceNaming
import com.pandulapeter.kubrikoCreator.context.resourceUsagesOf
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Makes the types that use each of [renames]' keys (resource paths from `composeResources`) use its value.
 *
 * @return What puts back the types it changed as they were, or null if it changed none.
 */
@EditorMutation
internal fun CommandContext.followInTypes(renames: Map<String, String>): Invocation<*>? {
    val blockTypes = blockTypes ?: return null
    val previous = blockTypes.types.value.mapNotNull { definition ->
        renames.entries.fold(definition) { renamed, (path, newPath) -> renamed.withResourceRenamed(path, newPath) }.takeIf { it != definition }?.let { renamed ->
            blockTypes.put(renamed)
            definition
        }
    }
    return previous.takeIf { it.isNotEmpty() }?.let { Invocation(ReplaceTypes, ReplaceTypes.Input(it)) }
}

/**
 * Makes the Scenes playing the music at one of [renames]' keys play its value, all of them or none, written together,
 * saying in the console which still play the old music when they can't be written.
 *
 * @return What puts back the Scenes it changed byte for byte, or null if it changed none.
 */
@EditorMutation
internal suspend fun CommandContext.followInScenes(renames: Map<String, String>): Invocation<*>? {
    val musicRenames = renames.filterKeys { ResourceKind.of(it) == ResourceKind.MUSIC }
    if (musicRenames.isEmpty()) return null
    val previous = withContext(ioDispatcher) {
        val contents = scenesWithMusicReplaced(scenes.scanNames(), musicRenames)
        val texts = contents.keys.associateWith { checkNotNull(scenes.read(it)) }
        try {
            scenes.writeAllTexts(contents)
            texts
        } catch (exception: IOException) {
            val reason = exception.message ?: exception.toString()
            report(ConsoleEntry.Status(CommandProblem.RESOURCE_MUSIC_NOT_FOLLOWED, isError = true, arguments = listOf(contents.keys.joinToString(), musicRenames.keys.joinToString(), reason)))
            emptyMap()
        }
    }
    return previous.takeIf { it.isNotEmpty() }?.let { Invocation(ReplaceScenes, ReplaceScenes.Input(it)) }
}

/**
 * Says in the console where Kotlin still names [path] once it is [newPath]: the Kotlin Blocks and Kotlin UI of the
 * types, and hand-written files (read once for several such calls inside `withKotlinFilesHeld`), none of which Kubriko
 * Creator rewrites.
 */
internal suspend fun CommandContext.reportNamedInCode(path: String, newPath: String) {
    val subject = RelationSubject.Resource(path)
    val inTypes = resourceUsagesOf(subject, blockTypes?.types?.value.orEmpty(), emptyMap()).filter { it.kind == RelationKind.NAMES_IN_KOTLIN }.map { it.place.toString() }
    val isNaming = resourceNaming(subject)
    val inFiles = withContext(ioDispatcher) { handWrittenKotlin().filterValues(isNaming).keys.sorted() }
    (inTypes + inFiles).forEach { place -> report(ConsoleEntry.Status(CommandProblem.RESOURCE_NAMED_IN_CODE, isError = true, arguments = listOf(place, path, newPath))) }
}
