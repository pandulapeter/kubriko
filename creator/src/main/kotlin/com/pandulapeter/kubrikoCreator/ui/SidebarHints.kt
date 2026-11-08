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
 * The screenplay's findings ([com.pandulapeter.kubrikoCreator.workspace.ScreenplayHealth]) by the sidebar row they are
 * about: a type, a Scene, or a page, which shows them as a quiet hint beside its name.
 */
internal class SidebarHints(
    private val findings: Map<Pair<FolderGroup, String>, List<HealthFinding>> = emptyMap(),
) {
    fun of(group: FolderGroup, name: String): List<HealthFinding> = findings[group to name].orEmpty()
}

/**
 * The workspace's [SidebarHints], none outside one.
 */
internal val LocalSidebarHints = compositionLocalOf { SidebarHints() }

/**
 * The [SidebarHints] of [findings], following them as they change; [types] tell Actor types from Manager types.
 */
@Composable
internal fun rememberSidebarHints(findings: StateFlow<List<HealthFinding>?>, types: StateFlow<List<BlockTypeDefinition>>?): SidebarHints {
    val current = findings.collectAsState().value.orEmpty()
    val definitions = types?.collectAsState()?.value.orEmpty()
    return remember(current, definitions) { sidebarHintsOf(current, definitions) }
}

/**
 * The hints [findings] give the sidebar's rows: only the screenplay's, since the rest belong to the Game map.
 */
internal fun sidebarHintsOf(findings: List<HealthFinding>, types: List<BlockTypeDefinition>): SidebarHints {
    val managerTypes = types.filterIsInstance<ManagerTypeDefinition>().mapTo(HashSet()) { it.name }
    return SidebarHints(
        findings.filter { it.kind in screenplayKinds }.groupBy { finding ->
            when (val subject = finding.subject) {
                is RelationSubject.Type -> (if (subject.name in managerTypes) FolderGroup.MANAGER_TYPES else FolderGroup.ACTOR_TYPES) to subject.name
                is RelationSubject.Scene -> FolderGroup.SCENES to subject.name
                is RelationSubject.Page -> FolderGroup.PAGES to subject.name
                is RelationSubject.Property, is RelationSubject.Image -> null
            }
        }.filterKeys { it != null }.mapKeys { checkNotNull(it.key) },
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
