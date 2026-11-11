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

import com.pandulapeter.kubrikoCreator.actors.ResourceKind
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.scenes.firstSceneSettings
import com.pandulapeter.kubrikoCreator.scenes.SceneSettingsState
import com.pandulapeter.kubrikoCreator.screenplay.PageNaming
import kotlinx.coroutines.withContext

/**
 * The number of Actors of each type ID that each readable Scene of the project places, by the Scene's name.
 */
internal suspend fun CommandContext.sceneTypeCounts(): Map<String, Map<String, Int>> = withContext(ioDispatcher) {
    scenes.names().mapNotNull { name -> scenes.actors(name)?.let { actors -> name to actors.groupingBy { it.typeId }.eachCount().toSortedMap() } }.toMap()
}

/**
 * The music each Scene of the open project plays, by the Scene's name, leaving out those that play none.
 */
internal fun CommandContext.sceneMusic(): Map<String, String> = scenes.names().mapNotNull { name ->
    scenes.actors(name)?.let { actors ->
        SceneSettingsState.indexIn(actors).takeIf { it >= 0 }?.let { index -> name to SceneSettingsState.read(actors[index].state).music }
    }
}.filter { (_, music) -> music.isNotEmpty() }.toMap()

/**
 * The relations between the parts of the open project.
 */
internal suspend fun CommandContext.projectRelations() = ProjectRelations(types = blockTypes?.types?.value.orEmpty(), scenes = sceneTypeCounts())

/**
 * Everything that refers to the parts of the open project, with the hand-written Kotlin files only when those can name
 * [subject] ([ProjectReferences.readsKotlinFiles]).
 */
internal suspend fun CommandContext.projectReferences(subject: RelationSubject): ProjectReferences = projectReferences(listOf(subject)).single()

/**
 * [projectReferences] of each of [subjects], in their order, reading the project once for all of them: they differ only
 * in the file a page links to to mention each.
 */
internal suspend fun CommandContext.projectReferences(subjects: List<RelationSubject>): List<ProjectReferences> {
    val types = blockTypes?.types?.value.orEmpty()
    // A Scene's Actors refer to nothing but their type, so no other subject's references need them.
    val sceneCounts = if (subjects.any { it is RelationSubject.Type }) sceneTypeCounts() else emptyMap()
    return withContext(ioDispatcher) {
        val mentionedPaths = subjects.map { mentionedPathOf(it) }
        val firstScene = firstSceneSettings(project.rootDirectory)?.firstScene
        val kotlinFiles = if (subjects.any(ProjectReferences::readsKotlinFiles)) handWrittenKotlin() else emptyMap()
        // Documents are the user's, which Creator changes only when asked to, so only pages are cleaned up; and
        // pages are read only for what they can link to.
        val markdown = if (mentionedPaths.all { it == null }) emptyMap() else screenplayTexts().markdown.filterKeys { PageNaming.nameOf(it) != null }
        val music = if (subjects.any { it is RelationSubject.Resource && ResourceKind.of(it.path) == ResourceKind.MUSIC }) sceneMusic() else emptyMap()
        val relations = ProjectRelations(types, sceneCounts)
        mentionedPaths.map { mentionedPath ->
            ProjectReferences(
                types = types,
                relations = relations,
                firstScene = firstScene,
                kotlinFiles = kotlinFiles,
                markdown = if (mentionedPath == null) emptyMap() else markdown,
                mentionedPath = mentionedPath,
                sceneMusic = music,
            )
        }
    }
}
