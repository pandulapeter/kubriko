/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.search

import com.pandulapeter.kubrikoCreator.actors.ResourceKind
import com.pandulapeter.kubrikoCreator.context.RelationPlace
import com.pandulapeter.kubrikoCreator.context.UsagePlace
import com.pandulapeter.kubrikoCreator.ui.components.Accent
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.ic_block
import kubriko.creator.generated.resources.ic_code
import kubriko.creator.generated.resources.ic_music
import kubriko.creator.generated.resources.ic_page
import kubriko.creator.generated.resources.ic_scene
import kubriko.creator.generated.resources.ic_settings
import kubriko.creator.generated.resources.ic_sound
import kubriko.creator.generated.resources.ic_sprite
import org.jetbrains.compose.resources.DrawableResource

/**
 * The icon and accent of the kind of place [this] is.
 */
internal val UsagePlace.look: Pair<DrawableResource, Accent?>
    get() = when (this) {
        is UsagePlace.InModel -> when (place) {
            is RelationPlace.Scene -> Res.drawable.ic_scene to Accent.SCENE
            is RelationPlace.Type, is RelationPlace.Text, is RelationPlace.KotlinUi -> Res.drawable.ic_block to Accent.EVENT
        }
        is UsagePlace.KotlinFile -> Res.drawable.ic_code to Accent.CODE
        is UsagePlace.Markdown -> Res.drawable.ic_page to Accent.SCREENPLAY
        is UsagePlace.ResourceFile -> when (ResourceKind.of(path)) {
            ResourceKind.SOUND -> Res.drawable.ic_sound to Accent.AUDIO
            ResourceKind.MUSIC -> Res.drawable.ic_music to Accent.AUDIO
            ResourceKind.SPRITE, null -> Res.drawable.ic_sprite to Accent.CODE
        }
        UsagePlace.GameSettings -> Res.drawable.ic_settings to null
    }
