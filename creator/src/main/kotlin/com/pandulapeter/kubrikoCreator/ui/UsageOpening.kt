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
import com.pandulapeter.kubrikoCreator.context.Usage
import com.pandulapeter.kubrikoCreator.context.UsagePlace
import com.pandulapeter.kubrikoCreator.screenplay.PageNaming
import com.pandulapeter.kubrikoCreator.workspace.RevealedPlace
import com.pandulapeter.kubrikoCreator.workspace.SelectionController

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
