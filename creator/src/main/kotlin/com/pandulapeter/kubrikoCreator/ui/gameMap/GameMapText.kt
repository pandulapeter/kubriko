/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.gameMap

import com.pandulapeter.kubrikoCreator.context.ProjectDigest
import com.pandulapeter.kubrikoCreator.context.RelationKind
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.game_map_adds
import kubriko.creator.generated.resources.game_map_changes
import kubriko.creator.generated.resources.game_map_collides_with
import kubriko.creator.generated.resources.game_map_goes_to
import kubriko.creator.generated.resources.game_map_inherits_from
import kubriko.creator.generated.resources.game_map_mentions
import kubriko.creator.generated.resources.game_map_moves_towards
import kubriko.creator.generated.resources.game_map_names_in_kotlin
import kubriko.creator.generated.resources.game_map_places
import kubriko.creator.generated.resources.game_map_reads
import kubriko.creator.generated.resources.game_map_starts_with

/**
 * The lines of the digest about each type, by the type.
 */
internal fun referencesByOwner(digest: ProjectDigest): Map<RelationSubject, List<ProjectDigest.RelationLine>> = digest.relations.groupBy { it.owner }

/**
 * How the map names a relation, with what it refers to as its argument.
 */
internal val RelationKind.label
    get() = when (this) {
        RelationKind.INHERITS_FROM -> Res.string.game_map_inherits_from
        RelationKind.COLLIDES_WITH -> Res.string.game_map_collides_with
        RelationKind.ADDS -> Res.string.game_map_adds
        RelationKind.MOVES_TOWARDS -> Res.string.game_map_moves_towards
        RelationKind.GOES_TO -> Res.string.game_map_goes_to
        RelationKind.PLACES -> Res.string.game_map_places
        RelationKind.READS -> Res.string.game_map_reads
        RelationKind.CHANGES -> Res.string.game_map_changes
        RelationKind.NAMES_IN_KOTLIN -> Res.string.game_map_names_in_kotlin
        RelationKind.STARTS_WITH -> Res.string.game_map_starts_with
        RelationKind.MENTIONS -> Res.string.game_map_mentions
    }

/**
 * What a Scene places: `Brick (40), Ball, Paddle`, the most numerous first.
 */
internal fun placedText(actorCounts: Map<String, Int>) = actorCounts.entries
    .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
    .joinToString(", ") { (type, count) -> if (count > 1) "$type ($count)" else type }
