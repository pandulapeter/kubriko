/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.actorTypes

import com.pandulapeter.kubrikoCreator.actors.BlockKind
import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.PlacementProblem
import com.pandulapeter.kubrikoCreator.actors.PropertyKind

/**
 * Why a type of [kind] can't have an Event of this type, or null if it can: the Event's own reason
 * ([EventType.misplacedProblem]), named as the editor names it.
 */
internal fun EventType.problemIn(kind: BlockTypeKind): PlacementProblem? = if (isAvailableIn(kind)) {
    null
} else {
    checkNotNull(misplacedProblem) { "$this has no misplacedProblem, though a $kind type can't have it." }
}

/**
 * Why a Block of this kind can't be in an Event of [eventType] of a type of [kind], or null if it can: what a Manager
 * type lacks comes first, since no Event of one could take the Block, then the Event the Block needs. Each reason is the
 * Block's own ([BlockKind.managerTypeProblem], [BlockKind.otherEventProblem]).
 */
internal fun BlockKind.problemIn(eventType: EventType, kind: BlockTypeKind): PlacementProblem? = if (isAvailableIn(eventType, kind)) {
    null
} else {
    checkNotNull(managerTypeProblem.takeIf { kind == BlockTypeKind.MANAGER } ?: otherEventProblem) { "$this has no reason for not being in a $eventType Event of a $kind type." }
}

/**
 * Why a Block can't name a Property of this kind that its type (or the Manager type it names) doesn't have.
 */
internal val PropertyKind.missingProblem: PlacementProblem
    get() = when (this) {
        PropertyKind.NUMBER -> PlacementProblem.NUMBER_PROPERTY_MISSING
        PropertyKind.TEXT -> PlacementProblem.TEXT_PROPERTY_MISSING
        PropertyKind.TOGGLE -> PlacementProblem.TOGGLE_PROPERTY_MISSING
    }

/**
 * Why a Block can't name a Property of this kind that its type (or the Manager type it names) doesn't have, when some it
 * has are named similarly.
 */
internal val PropertyKind.missingSimilarProblem: PlacementProblem
    get() = when (this) {
        PropertyKind.NUMBER -> PlacementProblem.NUMBER_PROPERTY_MISSING_SIMILAR
        PropertyKind.TEXT -> PlacementProblem.TEXT_PROPERTY_MISSING_SIMILAR
        PropertyKind.TOGGLE -> PlacementProblem.TOGGLE_PROPERTY_MISSING_SIMILAR
    }

/**
 * Why a Block can't name a Property of this kind that its type (or the Manager type it names) has only of another kind.
 */
internal val PropertyKind.otherKindProblem: PlacementProblem
    get() = when (this) {
        PropertyKind.NUMBER -> PlacementProblem.NUMBER_PROPERTY_OF_OTHER_KIND
        PropertyKind.TEXT -> PlacementProblem.TEXT_PROPERTY_OF_OTHER_KIND
        PropertyKind.TOGGLE -> PlacementProblem.TOGGLE_PROPERTY_OF_OTHER_KIND
    }
