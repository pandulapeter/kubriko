/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.settings

import com.pandulapeter.kubrikoCreator.actors.codegen.PluginReason
import com.pandulapeter.kubrikoCreator.actors.codegen.PluginUse
import com.pandulapeter.kubrikoCreator.project.AspectRatio
import com.pandulapeter.kubrikoCreator.project.GameSettings
import com.pandulapeter.kubrikoCreator.project.KubrikoArtifact
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.library_audio_playback
import kubriko.creator.generated.resources.library_collision
import kubriko.creator.generated.resources.library_creator_api
import kubriko.creator.generated.resources.library_engine
import kubriko.creator.generated.resources.library_gamepad_input
import kubriko.creator.generated.resources.library_keyboard_input
import kubriko.creator.generated.resources.library_particles
import kubriko.creator.generated.resources.library_persistence
import kubriko.creator.generated.resources.library_physics
import kubriko.creator.generated.resources.library_pointer_input
import kubriko.creator.generated.resources.library_scene_editor_api
import kubriko.creator.generated.resources.library_serialization
import kubriko.creator.generated.resources.library_shaders
import kubriko.creator.generated.resources.library_sprites
import kubriko.creator.generated.resources.project_settings_aspect_ratio_dynamic
import kubriko.creator.generated.resources.project_settings_aspect_ratio_dynamic_description
import kubriko.creator.generated.resources.project_settings_aspect_ratio_fit_horizontal
import kubriko.creator.generated.resources.project_settings_aspect_ratio_fit_horizontal_description
import kubriko.creator.generated.resources.project_settings_aspect_ratio_fit_vertical
import kubriko.creator.generated.resources.project_settings_aspect_ratio_fit_vertical_description
import kubriko.creator.generated.resources.project_settings_aspect_ratio_fixed
import kubriko.creator.generated.resources.project_settings_aspect_ratio_fixed_description
import kubriko.creator.generated.resources.project_settings_aspect_ratio_stretched
import kubriko.creator.generated.resources.project_settings_aspect_ratio_stretched_description
import kubriko.creator.generated.resources.project_settings_use_collision
import kubriko.creator.generated.resources.project_settings_use_key_held
import kubriko.creator.generated.resources.project_settings_use_key_pressed
import kubriko.creator.generated.resources.project_settings_use_plays_music
import kubriko.creator.generated.resources.project_settings_use_plays_sound
import kubriko.creator.generated.resources.project_settings_use_scene_music
import kubriko.creator.generated.resources.project_settings_use_sprite
import kubriko.creator.generated.resources.project_settings_use_stops_music
import org.jetbrains.compose.resources.StringResource

/**
 * The name of this library, as Project settings lists it.
 */
internal val KubrikoArtifact.title: StringResource
    get() = when (this) {
        KubrikoArtifact.ENGINE -> Res.string.library_engine
        KubrikoArtifact.AUDIO_PLAYBACK -> Res.string.library_audio_playback
        KubrikoArtifact.COLLISION -> Res.string.library_collision
        KubrikoArtifact.GAMEPAD_INPUT -> Res.string.library_gamepad_input
        KubrikoArtifact.KEYBOARD_INPUT -> Res.string.library_keyboard_input
        KubrikoArtifact.PARTICLES -> Res.string.library_particles
        KubrikoArtifact.PERSISTENCE -> Res.string.library_persistence
        KubrikoArtifact.PHYSICS -> Res.string.library_physics
        KubrikoArtifact.POINTER_INPUT -> Res.string.library_pointer_input
        KubrikoArtifact.SERIALIZATION -> Res.string.library_serialization
        KubrikoArtifact.SHADERS -> Res.string.library_shaders
        KubrikoArtifact.SPRITES -> Res.string.library_sprites
        KubrikoArtifact.CREATOR_API -> Res.string.library_creator_api
        KubrikoArtifact.SCENE_EDITOR_API -> Res.string.library_scene_editor_api
    }

internal val AspectRatio.label: StringResource
    get() = when (this) {
        AspectRatio.DYNAMIC -> Res.string.project_settings_aspect_ratio_dynamic
        AspectRatio.FIT_HORIZONTAL -> Res.string.project_settings_aspect_ratio_fit_horizontal
        AspectRatio.FIT_VERTICAL -> Res.string.project_settings_aspect_ratio_fit_vertical
        AspectRatio.FIXED -> Res.string.project_settings_aspect_ratio_fixed
        AspectRatio.STRETCHED -> Res.string.project_settings_aspect_ratio_stretched
    }

internal val AspectRatio.description: StringResource
    get() = when (this) {
        AspectRatio.DYNAMIC -> Res.string.project_settings_aspect_ratio_dynamic_description
        AspectRatio.FIT_HORIZONTAL -> Res.string.project_settings_aspect_ratio_fit_horizontal_description
        AspectRatio.FIT_VERTICAL -> Res.string.project_settings_aspect_ratio_fit_vertical_description
        AspectRatio.FIXED -> Res.string.project_settings_aspect_ratio_fixed_description
        AspectRatio.STRETCHED -> Res.string.project_settings_aspect_ratio_stretched_description
    }

/**
 * What this use says, with its owner and subject as the arguments.
 */
internal val PluginUse.text: StringResource
    get() = when (reason) {
        PluginReason.COLLISION_EVENT -> Res.string.project_settings_use_collision
        PluginReason.KEY_PRESSED_EVENT -> Res.string.project_settings_use_key_pressed
        PluginReason.KEY_HELD_CONDITION -> Res.string.project_settings_use_key_held
        PluginReason.PLAYS_SOUND -> Res.string.project_settings_use_plays_sound
        PluginReason.PLAYS_MUSIC -> Res.string.project_settings_use_plays_music
        PluginReason.STOPS_MUSIC -> Res.string.project_settings_use_stops_music
        PluginReason.SCENE_MUSIC -> Res.string.project_settings_use_scene_music
        PluginReason.SPRITE -> Res.string.project_settings_use_sprite
    }

internal val PluginUse.arguments: List<String> get() = listOfNotNull(owner, subject)

/**
 * The frame rates the frame rate menu offers: the common ones, every frame the display shows, and [current] if it is
 * none of those.
 */
internal fun frameRateOptions(current: Int): List<Int> = (COMMON_FRAME_RATES + current).distinct().sortedBy { if (it == GameSettings.EVERY_DISPLAY_FRAME) Int.MAX_VALUE else it }

private val COMMON_FRAME_RATES = listOf(30, 60, 120, GameSettings.EVERY_DISPLAY_FRAME)
