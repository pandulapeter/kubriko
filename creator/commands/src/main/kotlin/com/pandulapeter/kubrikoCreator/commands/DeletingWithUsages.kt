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

import com.pandulapeter.kubrikoCreator.context.RelationPlace
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import com.pandulapeter.kubrikoCreator.context.Usage
import com.pandulapeter.kubrikoCreator.context.UsageEffect
import com.pandulapeter.kubrikoCreator.context.UsagePlace
import com.pandulapeter.kubrikoCreator.context.projectReferences
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonArray

/**
 * Deletes [subject] by applying [delete], which deletes only it, after cleaning up what Kubriko Creator owns that refers
 * to it ([com.pandulapeter.kubrikoCreator.context.ProjectReferences]): the Actors of its type leave every Scene, Blocks and
 * Events that only mean something with it are removed, and placeholders of its Properties become text. All of it is
 * one step, undone together, and the AI hears every usage and what happened to it, Kotlin left to change included.
 */
internal suspend fun CommandContext.deleteWithUsages(subject: RelationSubject, delete: Invocation<*>): Applied {
    val references = projectReferences(subject)
    val usages = references.usagesOf(subject)
    val parts = buildList {
        scenesWithout(subject, usages).takeIf { it.isNotEmpty() }?.let { add(Invocation(ReplaceScenes, ReplaceScenes.Input(it))) }
        references.typesAfterDeleting(usages).takeIf { it.isNotEmpty() }?.let { add(Invocation(ReplaceTypes, ReplaceTypes.Input(it))) }
        add(delete)
    }
    val applied = parts.singleOrNull()?.apply(this) ?: CompositeCommand.apply(this, CompositeCommand.Input(parts))
    return if (applied.failure == null && usages.isNotEmpty()) applied.copy(details = usagesDetails(references.usagesAfterDeleting(usages))) else applied
}

/**
 * What deleting [subject] would do to each of its usages, as [deleteWithUsages] would make it: for the confirmation and
 * `check_change`.
 */
internal suspend fun CommandContext.usagesOf(subject: RelationSubject): List<Usage> = projectReferences(subject).usagesOf(subject)

private suspend fun CommandContext.scenesWithout(subject: RelationSubject, usages: List<Usage>): Map<String, String> {
    val typeName = (subject as? RelationSubject.Type)?.name ?: return emptyMap()
    val sceneNames = usages.filter { it.effect == UsageEffect.ACTORS_REMOVED }.mapNotNull { ((it.place as? UsagePlace.InModel)?.place as? RelationPlace.Scene)?.name }
    return withContext(ioDispatcher) {
        sceneNames.associateWith { scene -> SceneDocument.format(checkNotNull(scenes.actors(scene)).filter { it.typeId != typeName }) }
    }
}

private fun usagesDetails(usages: List<Usage>) = buildJsonObject {
    putJsonArray("usages") { usages.forEach { add(it.toString()) } }
}
