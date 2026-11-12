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
import com.pandulapeter.kubrikoCreator.context.RelationPlace
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import com.pandulapeter.kubrikoCreator.context.Usage
import com.pandulapeter.kubrikoCreator.context.UsageEffect
import com.pandulapeter.kubrikoCreator.context.UsagePlace
import com.pandulapeter.kubrikoCreator.screenplay.PageNaming
import com.pandulapeter.kubrikoCreator.ui.gameMap.label
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.delete_confirmation_title_folder
import kubriko.creator.generated.resources.delete_confirmation_title_property
import kubriko.creator.generated.resources.delete_confirmation_title_resource
import kubriko.creator.generated.resources.delete_confirmation_title_scene
import kubriko.creator.generated.resources.delete_confirmation_title_timer
import kubriko.creator.generated.resources.delete_confirmation_title_type
import kubriko.creator.generated.resources.document_delete_title
import kubriko.creator.generated.resources.page_delete_title
import kubriko.creator.generated.resources.usage_effect_actors_removed
import kubriko.creator.generated.resources.usage_effect_block_removed
import kubriko.creator.generated.resources.usage_effect_deleted_with_folder
import kubriko.creator.generated.resources.usage_effect_event_removed
import kubriko.creator.generated.resources.usage_effect_image_copied
import kubriko.creator.generated.resources.usage_effect_kotlin_to_change
import kubriko.creator.generated.resources.usage_effect_mention_to_text
import kubriko.creator.generated.resources.usage_effect_music_cleared
import kubriko.creator.generated.resources.usage_effect_placeholder_to_text
import kubriko.creator.generated.resources.usage_effect_prevents_deleting
import kubriko.creator.generated.resources.usage_effect_section_removed
import kubriko.creator.generated.resources.usage_effect_sprite_cleared
import kubriko.creator.generated.resources.usage_place_block
import kubriko.creator.generated.resources.usage_place_event
import kubriko.creator.generated.resources.usage_place_game_settings
import kubriko.creator.generated.resources.usage_place_kotlin_ui
import kubriko.creator.generated.resources.usage_place_page
import kubriko.creator.generated.resources.usage_place_scene
import kubriko.creator.generated.resources.usage_place_text
import org.jetbrains.compose.resources.stringResource

/**
 * The title of the confirmation asking to delete [this].
 */
@Composable
internal fun RelationSubject.deleteConfirmationTitle() = when (this) {
    is RelationSubject.Type -> stringResource(Res.string.delete_confirmation_title_type, name)
    is RelationSubject.Scene -> stringResource(Res.string.delete_confirmation_title_scene, name)
    is RelationSubject.Property -> stringResource(Res.string.delete_confirmation_title_property, name)
    is RelationSubject.Page -> stringResource(Res.string.page_delete_title, name)
    is RelationSubject.Image -> stringResource(Res.string.document_delete_title, path)
    is RelationSubject.Resource -> stringResource(Res.string.delete_confirmation_title_resource, path)
    is RelationSubject.Timer -> stringResource(Res.string.delete_confirmation_title_timer, name)
    is RelationSubject.Folder -> stringResource(Res.string.delete_confirmation_title_folder, path)
}

/**
 * Where [this] usage is, for people: Events and Blocks counted from 1, as the editor lists them.
 */
@Composable
internal fun Usage.placeText() = when (val place = place) {
    is UsagePlace.InModel -> when (val inModel = place.place) {
        is RelationPlace.Type -> {
            val eventIndex = inModel.eventIndex
            val blockIndex = inModel.blockIndex
            when {
                eventIndex == null -> inModel.name
                blockIndex == null -> stringResource(Res.string.usage_place_event, inModel.name, eventIndex + 1)
                else -> stringResource(Res.string.usage_place_block, inModel.name, eventIndex + 1, blockIndex + 1)
            }
        }
        is RelationPlace.Text -> stringResource(Res.string.usage_place_text, inModel.manager, inModel.textIndex + 1)
        is RelationPlace.KotlinUi -> stringResource(Res.string.usage_place_kotlin_ui, inModel.manager)
        is RelationPlace.Scene -> stringResource(Res.string.usage_place_scene, inModel.name)
    }
    is UsagePlace.KotlinFile -> place.path
    is UsagePlace.ResourceFile -> place.path
    is UsagePlace.Markdown -> PageNaming.nameOf(place.path)?.let { stringResource(Res.string.usage_place_page, it) } ?: place.path
    UsagePlace.GameSettings -> stringResource(Res.string.usage_place_game_settings)
}

/**
 * How [this] usage refers to what is deleted: `Adds Brick`, `Places Brick (40)`.
 */
@Composable
internal fun Usage.relationText() = stringResource(kind.label, subject.toString() + if (count > 1) " ($count)" else "")

@Composable
internal fun UsageEffect.text() = stringResource(
    when (this) {
        UsageEffect.BLOCK_REMOVED -> Res.string.usage_effect_block_removed
        UsageEffect.SECTION_REMOVED -> Res.string.usage_effect_section_removed
        UsageEffect.EVENT_REMOVED -> Res.string.usage_effect_event_removed
        UsageEffect.ACTORS_REMOVED -> Res.string.usage_effect_actors_removed
        UsageEffect.PLACEHOLDER_TO_TEXT -> Res.string.usage_effect_placeholder_to_text
        UsageEffect.KOTLIN_TO_CHANGE -> Res.string.usage_effect_kotlin_to_change
        UsageEffect.MENTION_TO_TEXT -> Res.string.usage_effect_mention_to_text
        UsageEffect.PREVENTS_DELETING -> Res.string.usage_effect_prevents_deleting
        UsageEffect.SPRITE_CLEARED -> Res.string.usage_effect_sprite_cleared
        UsageEffect.MUSIC_CLEARED -> Res.string.usage_effect_music_cleared
        UsageEffect.IMAGE_COPIED -> Res.string.usage_effect_image_copied
        UsageEffect.DELETED_WITH_FOLDER -> Res.string.usage_effect_deleted_with_folder
    },
)

/**
 * Whether [this] usage is something the user still has to deal with: Kotlin to change, or what refuses the delete.
 */
internal val Usage.needsAttention get() = effect == UsageEffect.KOTLIN_TO_CHANGE || effect == UsageEffect.PREVENTS_DELETING
