/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands

import com.pandulapeter.kubrikoCreator.commands.resources.scenesWithMusicReplaced
import com.pandulapeter.kubrikoCreator.commands.screenplay.KeepPageImages
import com.pandulapeter.kubrikoCreator.commands.screenplay.UnlinkMentions
import com.pandulapeter.kubrikoCreator.context.RelationPlace
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import com.pandulapeter.kubrikoCreator.context.Usage
import com.pandulapeter.kubrikoCreator.context.UsageEffect
import com.pandulapeter.kubrikoCreator.context.UsagePlace
import com.pandulapeter.kubrikoCreator.context.projectReferences
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.putJsonArray
import java.io.File

/**
 * Deletes [subject] by applying [delete], which deletes only it, after cleaning up what Kubriko Creator owns that refers
 * to it ([com.pandulapeter.kubrikoCreator.context.ProjectReferences]): the Actors of its type leave every Scene, Blocks and
 * Events that only mean something with it are removed, placeholders of its Properties become text, and so do the pages'
 * mentions of it; a sprite's Actor types are drawn as their shapes, a music's Scenes play none, and the pages showing
 * a sprite show a copy of it kept next to them. All of it is one step, undone together, and the AI hears every usage
 * and what happened to it, Kotlin left to change included.
 */
internal suspend fun CommandContext.deleteWithUsages(subject: RelationSubject, delete: Invocation<*>): Applied {
    val references = projectReferences(subject)
    val usages = references.usagesOf(subject)
    val mentioning = usages.filter { it.effect == UsageEffect.MENTION_TO_TEXT }.mapNotNull { (it.place as? UsagePlace.Markdown)?.path }.distinct()
    val showing = usages.filter { it.effect == UsageEffect.IMAGE_COPIED }.mapNotNull { (it.place as? UsagePlace.Markdown)?.path }.distinct()
    val shownImage = references.mentionedPath?.takeIf { showing.isNotEmpty() }?.let { path ->
        withContext(ioDispatcher) { File(project.rootDirectory, path).readBytes() }.let { KeepPageImages.Input(path, it, showing) }
    }
    val parts = buildList {
        scenesWithout(subject, usages).takeIf { it.isNotEmpty() }?.let { add(Invocation(ReplaceScenes, ReplaceScenes.Input(it))) }
        references.typesAfterDeleting(usages).takeIf { it.isNotEmpty() }?.let { add(Invocation(ReplaceTypes, ReplaceTypes.Input(it))) }
        add(delete)
        references.mentionedPath?.takeIf { mentioning.isNotEmpty() }?.let { add(Invocation(UnlinkMentions, UnlinkMentions.Input(it, mentioning))) }
        shownImage?.let { add(Invocation(KeepPageImages, it)) }
    }
    val applied = parts.singleOrNull()?.apply(this) ?: CompositeCommand.apply(this, CompositeCommand.Input(parts))
    if (applied.failure != null) return applied
    val details = applied.details.orEmpty()
    // A file that stopped mentioning it since the usages were read was left as it is, so it isn't reported either.
    val unlinked = details[UnlinkMentions.UNLINKED]?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty()
    val reported = references.usagesAfterDeleting(usages).filter { usage ->
        (usage.place as? UsagePlace.Markdown)?.path.let { it == null || usage.effect == UsageEffect.IMAGE_COPIED || it in unlinked }
    }
    val kept = details - UnlinkMentions.UNLINKED
    return applied.copy(details = if (reported.isEmpty()) kept.takeIf { it.isNotEmpty() }?.let(::JsonObject) else JsonObject(kept + usagesDetails(reported)))
}

/**
 * What deleting [subject] would do to each of its usages, as [deleteWithUsages] would make it: for the confirmation and
 * `check_change`.
 */
internal suspend fun CommandContext.usagesOf(subject: RelationSubject): List<Usage> = projectReferences(subject).usagesOf(subject)

private suspend fun CommandContext.scenesWithout(subject: RelationSubject, usages: List<Usage>): Map<String, String> {
    fun scenesWith(effect: UsageEffect) = usages.filter { it.effect == effect }.mapNotNull { ((it.place as? UsagePlace.InModel)?.place as? RelationPlace.Scene)?.name }
    return when (subject) {
        is RelationSubject.Type -> withContext(ioDispatcher) {
            scenesWith(UsageEffect.ACTORS_REMOVED).associateWith { scene -> SceneDocument.format(checkNotNull(scenes.actors(scene)).filter { it.typeId != subject.name }) }
        }
        is RelationSubject.Resource -> withContext(ioDispatcher) { scenesWithMusicReplaced(scenesWith(UsageEffect.MUSIC_CLEARED), subject.path, "") }
        is RelationSubject.Scene, is RelationSubject.Property, is RelationSubject.Page, is RelationSubject.Image -> emptyMap()
    }
}

private fun usagesDetails(usages: List<Usage>) = buildJsonObject {
    putJsonArray("usages") { usages.forEach { add(it.toString()) } }
}
