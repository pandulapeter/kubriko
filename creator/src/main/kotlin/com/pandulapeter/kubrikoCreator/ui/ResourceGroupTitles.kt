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

import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.resource_group_documents
import kubriko.creator.generated.resources.resource_group_kotlin
import kubriko.creator.generated.resources.resource_group_music
import kubriko.creator.generated.resources.resource_group_scenes
import kubriko.creator.generated.resources.resource_group_sounds
import kubriko.creator.generated.resources.resource_group_sprites
import org.jetbrains.compose.resources.StringResource

internal val ResourceGroup.title: StringResource
    get() = when (this) {
        ResourceGroup.SCENES -> Res.string.resource_group_scenes
        ResourceGroup.DOCUMENTS -> Res.string.resource_group_documents
        ResourceGroup.SPRITES -> Res.string.resource_group_sprites
        ResourceGroup.SOUNDS -> Res.string.resource_group_sounds
        ResourceGroup.MUSIC -> Res.string.resource_group_music
        ResourceGroup.KOTLIN -> Res.string.resource_group_kotlin
    }
