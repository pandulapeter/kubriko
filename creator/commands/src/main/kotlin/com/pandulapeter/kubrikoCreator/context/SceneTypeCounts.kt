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

import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyKind
import com.pandulapeter.kubrikoCreator.actors.ResourceKind
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.actorTypes.actorTypeReferenceProperties
import com.pandulapeter.kubrikoCreator.commands.actorTypes.audioReferenceProperties
import com.pandulapeter.kubrikoCreator.commands.actorTypes.referencePropertiesByKind
import com.pandulapeter.kubrikoCreator.commands.scenes.firstSceneSettings
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import com.pandulapeter.kubrikoCreator.scenes.SceneSettingsState
import com.pandulapeter.kubrikoCreator.screenplay.PageNaming
import kotlinx.coroutines.withContext

/**
 * The number of Actors of each type ID that each readable Scene of the project places, by the Scene's name.
 */
internal suspend fun CommandContext.sceneTypeCounts(): Map<String, Map<String, Int>> = sceneCounts().types

/**
 * What the readable Scenes of the project place, read in one pass over them.
 *
 * @param types The type IDs of the Actors each Scene places, each with how many, by the Scene's name.
 * @param savedReferences How many of the values each Scene saves for reference Properties name each thing, by the
 *   Scene's name and then the thing's: an Actor type's name ([actorTypeReferenceProperties]), or a sound's or music's
 *   path ([audioReferenceProperties]), which never look alike. A value an Actor keeps about its own type isn't counted,
 *   since deleting that type takes the Actor away with it.
 * @param savedAudio The sounds and music each Scene saves for sound and music Properties, each of its kind, by the
 *   Scene's name, leaving out those that save none.
 * @param music The music each Scene plays, by the Scene's name, leaving out those that play none.
 */
internal class SceneCounts(
    val types: Map<String, Map<String, Int>>,
    val savedReferences: Map<String, Map<String, Int>>,
    val savedAudio: Map<String, SavedAudio>,
    val music: Map<String, String>,
) {
    companion object {
        val NONE = SceneCounts(emptyMap(), emptyMap(), emptyMap(), emptyMap())
    }
}

/**
 * The paths a Scene saves for sound Properties ([sounds]) and for music Properties ([music]).
 */
internal data class SavedAudio(
    val sounds: Set<String> = emptySet(),
    val music: Set<String> = emptySet(),
)

/**
 * The [SceneCounts] of the open project, with the saved reference values only when the project's [types] are given.
 */
internal suspend fun CommandContext.sceneCounts(types: List<BlockTypeDefinition>? = null): SceneCounts = withContext(ioDispatcher) {
    val references = types?.let(::referencePropertiesByKind).orEmpty()
    val typeCounts = LinkedHashMap<String, Map<String, Int>>()
    val referenceCounts = LinkedHashMap<String, Map<String, Int>>()
    val savedAudio = LinkedHashMap<String, SavedAudio>()
    val music = LinkedHashMap<String, String>()
    scenes.names().forEach { name ->
        val actors = scenes.actors(name) ?: return@forEach
        typeCounts[name] = actors.groupingBy { it.typeId }.eachCount().toSortedMap()
        SceneSettingsState.indexIn(actors).takeIf { it >= 0 }?.let { SceneSettingsState.read(actors[it].state).music }?.ifEmpty { null }?.let { music[name] = it }
        if (references.isEmpty()) return@forEach
        val named = ArrayList<String>()
        val sounds = HashSet<String>()
        val savedMusic = HashSet<String>()
        actors.forEach { actor ->
            val kinds = references[actor.typeId] ?: return@forEach
            val state = SceneDocument.stateObject(actor.state) ?: return@forEach
            kinds.forEach { (kind, names) ->
                val values = SceneDocument.referencedTypes(state, names)
                when (kind) {
                    PropertyKind.SOUND -> sounds += values
                    PropertyKind.MUSIC -> savedMusic += values
                    else -> Unit
                }
                values.filterTo(named) { it != actor.typeId }
            }
        }
        if (named.isNotEmpty()) referenceCounts[name] = named.groupingBy { it }.eachCount()
        if (sounds.isNotEmpty() || savedMusic.isNotEmpty()) savedAudio[name] = SavedAudio(sounds, savedMusic)
    }
    SceneCounts(typeCounts, referenceCounts, savedAudio, music)
}

/**
 * The music each Scene of the open project plays, by the Scene's name, leaving out those that play none.
 */
internal suspend fun CommandContext.sceneMusic(): Map<String, String> = sceneCounts().music

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
    // A Scene's Actors refer to nothing but types and, through what they save and the Scene's settings, audio files.
    val sceneCounts = if (subjects.any { it is RelationSubject.Type || it.isAudio }) sceneCounts(types) else SceneCounts.NONE
    return withContext(ioDispatcher) {
        val mentionedPaths = subjects.map { mentionedPathOf(it) }
        val firstScene = firstSceneSettings(project.rootDirectory)?.firstScene
        val kotlinFiles = if (subjects.any(ProjectReferences::readsKotlinFiles)) handWrittenKotlin() else emptyMap()
        // Documents are the user's, which Creator changes only when asked to, so only pages are cleaned up; and
        // pages are read only for what they can link to.
        val markdown = if (mentionedPaths.all { it == null }) emptyMap() else screenplayTexts().markdown.filterKeys { PageNaming.nameOf(it) != null }
        val relations = ProjectRelations(types, sceneCounts.types)
        mentionedPaths.map { mentionedPath ->
            ProjectReferences(
                types = types,
                relations = relations,
                firstScene = firstScene,
                kotlinFiles = kotlinFiles,
                markdown = if (mentionedPath == null) emptyMap() else markdown,
                mentionedPath = mentionedPath,
                sceneMusic = sceneCounts.music,
                sceneSavedReferences = sceneCounts.savedReferences,
            )
        }
    }
}

private val RelationSubject.isAudio get() = this is RelationSubject.Resource && ResourceKind.of(path).let { it == ResourceKind.SOUND || it == ResourceKind.MUSIC }
