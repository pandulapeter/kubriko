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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.commands.folders.FolderGroup
import com.pandulapeter.kubrikoCreator.context.HealthFinding
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import kotlinx.coroutines.flow.StateFlow

/**
 * The project's findings ([com.pandulapeter.kubrikoCreator.workspace.ProjectHealth]) by where they show: the screenplay's
 * by the sidebar row they are about (a type, a Scene, or a page), as a quiet hint beside its name, and all of them by
 * where they are fixed ([HealthFindingPlace]).
 */
internal class HealthHints(
    private val rows: Map<Pair<FolderGroup, String>, List<HealthFinding>> = emptyMap(),
    private val places: Map<HealthFindingPlace, List<HealthFinding>> = emptyMap(),
) {
    fun ofRow(group: FolderGroup, name: String): List<HealthFinding> = rows[group to name].orEmpty()

    /**
     * The findings [place] shows.
     */
    fun ofPlace(place: HealthFindingPlace): List<HealthFinding> = places[place].orEmpty()
}

/**
 * The workspace's [HealthHints], none outside one.
 */
internal val LocalHealthHints = compositionLocalOf { HealthHints() }

/**
 * The [HealthHints] of [findings], following them as they change; [types] tell Actor types from Manager types.
 */
@Composable
internal fun rememberHealthHints(findings: StateFlow<List<HealthFinding>?>, types: StateFlow<List<BlockTypeDefinition>>?): HealthHints {
    val current = findings.collectAsState().value.orEmpty()
    val definitions = types?.collectAsState()?.value.orEmpty()
    return remember(current, definitions) { healthHintsOf(current, definitions) }
}

/**
 * The hints [findings] give: the sidebar's rows only the screenplay's, since the rest belong to the Game map and the
 * editors; and the places all but the Scenes nothing goes to while the first Scene isn't known, since one of those is
 * the first, which would always show one.
 */
internal fun healthHintsOf(findings: List<HealthFinding>, types: List<BlockTypeDefinition>): HealthHints {
    val managerTypes = types.filterIsInstance<ManagerTypeDefinition>().mapTo(HashSet()) { it.name }
    return HealthHints(
        rows = findings.filter { it.kind in screenplayKinds }.groupBy { finding ->
            when (val subject = finding.subject) {
                is RelationSubject.Type -> (if (subject.name in managerTypes) FolderGroup.MANAGER_TYPES else FolderGroup.ACTOR_TYPES) to subject.name
                is RelationSubject.Scene -> FolderGroup.SCENES to subject.name
                is RelationSubject.Page -> FolderGroup.PAGES to subject.name
                is RelationSubject.Property, is RelationSubject.Image, is RelationSubject.Resource -> null
            }
        }.filterKeys { it != null }.mapKeys { checkNotNull(it.key) },
        places = findings
            .filterNot { it.kind == HealthFinding.Kind.UNREACHED_SCENE && it.detail == null }
            .groupBy { it.place }
            .filterKeys { it != null }
            .mapKeys { checkNotNull(it.key) },
    )
}

private val screenplayKinds = setOf(
    HealthFinding.Kind.UNMENTIONED,
    HealthFinding.Kind.MENTION_MISSING,
    HealthFinding.Kind.PAGE_WITHOUT_TITLE,
    HealthFinding.Kind.PAGE_WITHOUT_SUMMARY,
    HealthFinding.Kind.PLANNED_MAY_BE_BUILT,
    HealthFinding.Kind.PAGE_IMAGE_MISSING,
)
