/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.folders

import com.pandulapeter.kubrikoCreator.actors.ActorTypeFamily
import com.pandulapeter.kubrikoCreator.actors.FolderPaths
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.actorTypes.DeleteActorType
import com.pandulapeter.kubrikoCreator.commands.managerTypes.DeleteManagerType
import com.pandulapeter.kubrikoCreator.commands.scenes.DeleteScene
import com.pandulapeter.kubrikoCreator.commands.screenplay.DeletePage
import com.pandulapeter.kubrikoCreator.commands.screenplay.markdownTexts
import com.pandulapeter.kubrikoCreator.context.RelationKind
import com.pandulapeter.kubrikoCreator.context.RelationPlace
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import com.pandulapeter.kubrikoCreator.context.Usage
import com.pandulapeter.kubrikoCreator.context.UsageEffect
import com.pandulapeter.kubrikoCreator.context.UsagePlace
import com.pandulapeter.kubrikoCreator.context.projectReferences
import com.pandulapeter.kubrikoCreator.screenplay.PageImages
import com.pandulapeter.kubrikoCreator.screenplay.PageNaming
import kotlinx.coroutines.withContext

/**
 * The deletes of everything in [folder] and the folders in it, each the command deleting it on its own, so each
 * cleans up what refers to it as it would alone: an Actor type's descendants before it, since a parent with children
 * can't be deleted, and a page's own images with it, since its folder goes too. Scenes and pages are the ones their
 * stores know, which is what each delete and the folder's own last check go by.
 */
internal fun CommandContext.contentDeletesOf(folder: RelationSubject.Folder): List<Invocation<*>> {
    fun isIn(itemFolder: String) = FolderPaths.isWithin(itemFolder, folder.path)
    return when (folder.group) {
        FolderGroup.SCENES -> scenes.names()
            .filter { isIn(FolderPaths.parentOf(it)) }
            .map { Invocation(DeleteScene, DeleteScene.Input(it)) }
        FolderGroup.PAGES -> screenplay.names()
            .filter { isIn(PageNaming.folderOf(it)) }
            .map { Invocation(DeletePage, DeletePage.Input(it, deleteImages = true)) }
        FolderGroup.ACTOR_TYPES -> {
            val types = blockTypes?.actorTypes?.value.orEmpty()
            val family = ActorTypeFamily(types)
            types.filter { isIn(it.folder) }
                .sortedByDescending { family.ancestorsOf(it.name).size }
                .map { Invocation(DeleteActorType, DeleteActorType.Input(it.name)) }
        }
        FolderGroup.MANAGER_TYPES -> blockTypes?.managerTypes?.value.orEmpty()
            .filter { isIn(it.folder) }
            .map { Invocation(DeleteManagerType, DeleteManagerType.Input(it.name)) }
    }
}

/**
 * What deleting [folder] deletes and changes: a row for each thing in it (a page's images too), then every usage of those things outside
 * the folder, as [deletes] ([contentDeletesOf]) would clean them up. A usage inside the folder goes with it, so it
 * isn't listed.
 *
 * @param isAfterDeleting Whether the Kotlin left to change is placed where it is once the Blocks and Events before it
 *   are removed, as the AI is told after the delete, rather than where it is now.
 */
internal suspend fun CommandContext.folderUsagesOf(
    folder: RelationSubject.Folder,
    deletes: List<Invocation<*>>,
    isAfterDeleting: Boolean = false,
): List<Usage> {
    val subjects = deletes.mapNotNull { it.deletedSubject }
    if (subjects.isEmpty()) return emptyList()
    val deleted = subjects.toSet()
    val references = projectReferences(subjects)
    val outside = subjects.indices
        .flatMap { index -> references[index].usagesOf(subjects[index]) }
        .filterNot { usage -> usage.ownerOrNull() in deleted }
        .distinct()
    val contents = (subjects.map(::placeOf) + pageImagesDeletedWith(subjects).map(UsagePlace::Markdown))
        .map { place -> Usage(place, RelationKind.IS_IN, folder, UsageEffect.DELETED_WITH_FOLDER) }
    return contents + if (isAfterDeleting) references.first().usagesAfterDeleting(outside) else outside
}

/**
 * The images the pages among [subjects] take with them (each page's own, which no page or document outside them
 * shows), by their paths from the project's root.
 */
private suspend fun CommandContext.pageImagesDeletedWith(subjects: List<RelationSubject>): List<String> {
    val pages = subjects.filterIsInstance<RelationSubject.Page>().map { it.name }
    if (pages.isEmpty()) return emptyList()
    return withContext(ioDispatcher) {
        val deletedPaths = pages.mapTo(HashSet(), PageNaming::pathOf)
        val otherFiles = markdownTexts().filterKeys { it !in deletedPaths }
        pages.flatMap { PageImages.unusedImagesOf(project.rootDirectory, it, otherFiles) }.distinct()
    }
}

/**
 * [usages] as the AI hears them once the folder's deletes are made, given what the deletes themselves reported
 * ([reportedByParts], as text): a page's mention only if the delete of what it mentions turned it into text, since one
 * changed after the usages were read is left as it is.
 */
internal fun usagesReported(usages: List<Usage>, reportedByParts: Set<String>) =
    usages.filter { it.effect != UsageEffect.MENTION_TO_TEXT || it.toString() in reportedByParts }

private fun Usage.ownerOrNull(): RelationSubject? = when (val place = place) {
    is UsagePlace.InModel -> place.place.owner
    is UsagePlace.Markdown -> PageNaming.nameOf(place.path)?.let(RelationSubject::Page)
    is UsagePlace.KotlinFile, UsagePlace.GameSettings -> null
}

private fun placeOf(subject: RelationSubject): UsagePlace = when (subject) {
    is RelationSubject.Type -> UsagePlace.InModel(RelationPlace.Type(subject.name))
    is RelationSubject.Scene -> UsagePlace.InModel(RelationPlace.Scene(subject.name))
    is RelationSubject.Page -> UsagePlace.Markdown(PageNaming.pathOf(subject.name))
    is RelationSubject.Property, is RelationSubject.Timer, is RelationSubject.Resource, is RelationSubject.Image, is RelationSubject.Folder ->
        error("A folder holds no ${subject::class.simpleName}.")
}
