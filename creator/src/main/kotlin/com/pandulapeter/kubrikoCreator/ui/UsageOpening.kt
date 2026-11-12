/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import com.pandulapeter.kubrikoCreator.actors.ResourceKind
import com.pandulapeter.kubrikoCreator.context.RelationPlace
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import com.pandulapeter.kubrikoCreator.context.Usage
import com.pandulapeter.kubrikoCreator.context.UsagePlace
import com.pandulapeter.kubrikoCreator.scenes.SceneStore
import com.pandulapeter.kubrikoCreator.screenplay.PageNaming
import com.pandulapeter.kubrikoCreator.ui.components.openInSystemEditor
import com.pandulapeter.kubrikoCreator.workspace.RevealedPlace
import com.pandulapeter.kubrikoCreator.workspace.SelectionController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Shows where [usage] is, lit up: the Event or Block of a type, a Manager type for its texts and Kotlin UI, a Scene with
 * its Actors of [actorIds] selected, a page or document, or a sprite.
 *
 * @return Whether it showed it: false for a type or Scene that is gone since the usage was read, and for a place the
 *   center has no view of (hand-written Kotlin, sounds and music, the game's settings), which the caller opens its own
 *   way, if at all.
 */
internal fun SelectionController.open(usage: Usage, actorIds: List<String> = emptyList()): Boolean = when (val place = usage.place) {
    is UsagePlace.InModel -> when (val inModel = place.place) {
        is RelationPlace.Type -> selectType(inModel.name, RevealedPlace.InType(inModel.name, inModel.eventIndex, inModel.blockIndex))
        is RelationPlace.Text -> selectType(inModel.manager, RevealedPlace.InType(inModel.manager))
        is RelationPlace.KotlinUi -> selectType(inModel.manager, RevealedPlace.InType(inModel.manager))
        is RelationPlace.Scene -> openScene(inModel.name, actorIds)
    }
    is UsagePlace.Markdown -> true.also { PageNaming.nameOf(place.path)?.let(::openPage) ?: openDocument(place.path) }
    is UsagePlace.ResourceFile -> (ResourceKind.of(place.path) == ResourceKind.SPRITE).also { isSprite -> if (isSprite) openSprite(place.path) }
    is UsagePlace.KotlinFile, UsagePlace.GameSettings -> false
}

/**
 * Goes to where [usage] (of [subject]) is, wherever it can be shown ([canBeShown]): in the center ([open]), with the
 * type's Actors selected in a Scene placing them, a hand-written Kotlin file in the system's app for it, and the game's
 * settings through [onOpenGameSettings]. Reads the Scene off the main thread.
 *
 * @param rootDirectory The project's folder, which a Kotlin file's path starts from.
 */
internal suspend fun SelectionController.goTo(usage: Usage, subject: RelationSubject?, scenes: SceneStore, rootDirectory: File, onOpenGameSettings: () -> Unit) {
    val scene = ((usage.place as? UsagePlace.InModel)?.place as? RelationPlace.Scene)?.name
    val actorIds = if (scene != null && subject is RelationSubject.Type) {
        withContext(Dispatchers.IO) { scenes.actors(scene)?.filter { it.typeId == subject.name }?.map { it.id }?.filter { it.isNotEmpty() } }.orEmpty()
    } else {
        emptyList()
    }
    if (open(usage, actorIds)) return
    when (val place = usage.place) {
        is UsagePlace.KotlinFile -> withContext(Dispatchers.IO) { runCatching { openInSystemEditor(File(rootDirectory, place.path)) } }
        UsagePlace.GameSettings -> onOpenGameSettings()
        is UsagePlace.InModel, is UsagePlace.Markdown, is UsagePlace.ResourceFile -> Unit
    }
}

/**
 * Whether [goTo] has somewhere to go for this usage: everywhere but a sound or music file, which Kubriko Creator has no
 * view of and names only by its path from `composeResources`.
 */
internal val Usage.canBeShown: Boolean get() = (place as? UsagePlace.ResourceFile)?.let { ResourceKind.of(it.path) == ResourceKind.SPRITE } ?: true
