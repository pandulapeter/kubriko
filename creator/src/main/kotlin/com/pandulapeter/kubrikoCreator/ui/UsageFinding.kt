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

import androidx.compose.runtime.staticCompositionLocalOf
import com.pandulapeter.kubrikoCreator.commands.folders.FolderGroup
import com.pandulapeter.kubrikoCreator.context.RelationSubject

/**
 * Opens the Search panel on what refers to the given subject, for the editors offering Find usages deep inside them
 * (a Property's row), or null where nothing does.
 */
internal val LocalUsageFinder = staticCompositionLocalOf<((RelationSubject) -> Unit)?> { null }

/**
 * What the sidebar's item [name] of [group] is, as find usages names it: a sprite, sound, or music file by its path from
 * `composeResources`, as the sidebar names those.
 */
internal fun usageSubjectOf(group: FolderGroup, name: String): RelationSubject = when (group) {
    FolderGroup.ACTOR_TYPES, FolderGroup.MANAGER_TYPES -> RelationSubject.Type(name)
    FolderGroup.SCENES -> RelationSubject.Scene(name)
    FolderGroup.PAGES -> RelationSubject.Page(name)
    FolderGroup.SPRITES, FolderGroup.SOUNDS, FolderGroup.MUSIC -> RelationSubject.Resource(name)
}
