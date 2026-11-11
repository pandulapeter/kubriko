/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.texts

import com.pandulapeter.kubrikoCreator.commands.LookupProblem
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_command_actor_property_missing
import kubriko.creator.generated.resources.error_command_actor_property_missing_similar
import kubriko.creator.generated.resources.error_command_actor_type_is_manager_type
import kubriko.creator.generated.resources.error_command_actor_type_missing
import kubriko.creator.generated.resources.error_command_actor_type_missing_similar
import kubriko.creator.generated.resources.error_command_block_insertion_index_invalid
import kubriko.creator.generated.resources.error_command_block_missing
import kubriko.creator.generated.resources.error_command_block_missing_none
import kubriko.creator.generated.resources.error_command_blocks_count_too_large
import kubriko.creator.generated.resources.error_command_blocks_none_to_remove
import kubriko.creator.generated.resources.error_command_document_missing
import kubriko.creator.generated.resources.error_command_document_missing_similar
import kubriko.creator.generated.resources.error_command_event_missing
import kubriko.creator.generated.resources.error_command_event_missing_none
import kubriko.creator.generated.resources.error_command_folder_missing_similar
import kubriko.creator.generated.resources.error_command_game_key_unknown_similar
import kubriko.creator.generated.resources.error_command_index_invalid
import kubriko.creator.generated.resources.error_command_manager_type_is_actor_type
import kubriko.creator.generated.resources.error_command_manager_type_missing
import kubriko.creator.generated.resources.error_command_manager_type_missing_similar
import kubriko.creator.generated.resources.error_command_music_missing
import kubriko.creator.generated.resources.error_command_music_missing_similar
import kubriko.creator.generated.resources.error_command_page_missing
import kubriko.creator.generated.resources.error_command_page_missing_similar
import kubriko.creator.generated.resources.error_command_page_section_missing
import kubriko.creator.generated.resources.error_command_page_section_missing_listed
import kubriko.creator.generated.resources.error_command_page_section_missing_similar
import kubriko.creator.generated.resources.error_command_property_unknown
import kubriko.creator.generated.resources.error_command_property_unknown_similar
import kubriko.creator.generated.resources.error_command_relations_property_missing
import kubriko.creator.generated.resources.error_command_relations_property_missing_similar
import kubriko.creator.generated.resources.error_command_scene_actor_missing
import kubriko.creator.generated.resources.error_command_scene_actor_missing_similar
import kubriko.creator.generated.resources.error_command_scene_missing
import kubriko.creator.generated.resources.error_command_scene_missing_similar
import kubriko.creator.generated.resources.error_command_sound_missing
import kubriko.creator.generated.resources.error_command_sound_missing_similar
import kubriko.creator.generated.resources.error_command_sprite_missing
import kubriko.creator.generated.resources.error_command_sprite_missing_similar
import kubriko.creator.generated.resources.error_command_text_missing
import kubriko.creator.generated.resources.error_command_text_missing_none
import kubriko.creator.generated.resources.error_command_timer_unknown
import kubriko.creator.generated.resources.error_command_timer_unknown_similar
import kubriko.creator.generated.resources.error_command_type_missing
import kubriko.creator.generated.resources.error_command_type_missing_similar
import kubriko.creator.generated.resources.error_command_unknown
import kubriko.creator.generated.resources.error_command_unknown_similar
import kubriko.creator.generated.resources.error_folder_missing
import kubriko.creator.generated.resources.error_game_key_unknown
import org.jetbrains.compose.resources.StringResource

/**
 * The string the user is shown for this [LookupProblem].
 */
internal fun LookupProblem.toResource(): StringResource = when (this) {
    LookupProblem.ACTOR_PROPERTY_MISSING -> Res.string.error_command_actor_property_missing
    LookupProblem.ACTOR_PROPERTY_MISSING_SIMILAR -> Res.string.error_command_actor_property_missing_similar
    LookupProblem.ACTOR_TYPE_IS_MANAGER_TYPE -> Res.string.error_command_actor_type_is_manager_type
    LookupProblem.ACTOR_TYPE_MISSING -> Res.string.error_command_actor_type_missing
    LookupProblem.ACTOR_TYPE_MISSING_SIMILAR -> Res.string.error_command_actor_type_missing_similar
    LookupProblem.BLOCKS_COUNT_TOO_LARGE -> Res.string.error_command_blocks_count_too_large
    LookupProblem.BLOCKS_NONE_TO_REMOVE -> Res.string.error_command_blocks_none_to_remove
    LookupProblem.BLOCK_INSERTION_INDEX_INVALID -> Res.string.error_command_block_insertion_index_invalid
    LookupProblem.BLOCK_MISSING -> Res.string.error_command_block_missing
    LookupProblem.BLOCK_MISSING_NONE -> Res.string.error_command_block_missing_none
    LookupProblem.DOCUMENT_MISSING -> Res.string.error_command_document_missing
    LookupProblem.DOCUMENT_MISSING_SIMILAR -> Res.string.error_command_document_missing_similar
    LookupProblem.EVENT_MISSING -> Res.string.error_command_event_missing
    LookupProblem.EVENT_MISSING_NONE -> Res.string.error_command_event_missing_none
    LookupProblem.FOLDER_MISSING -> Res.string.error_folder_missing
    LookupProblem.FOLDER_MISSING_SIMILAR -> Res.string.error_command_folder_missing_similar
    LookupProblem.GAME_KEY_UNKNOWN -> Res.string.error_game_key_unknown
    LookupProblem.GAME_KEY_UNKNOWN_SIMILAR -> Res.string.error_command_game_key_unknown_similar
    LookupProblem.INDEX_INVALID -> Res.string.error_command_index_invalid
    LookupProblem.MANAGER_TYPE_IS_ACTOR_TYPE -> Res.string.error_command_manager_type_is_actor_type
    LookupProblem.MANAGER_TYPE_MISSING -> Res.string.error_command_manager_type_missing
    LookupProblem.MANAGER_TYPE_MISSING_SIMILAR -> Res.string.error_command_manager_type_missing_similar
    LookupProblem.MUSIC_MISSING -> Res.string.error_command_music_missing
    LookupProblem.MUSIC_MISSING_SIMILAR -> Res.string.error_command_music_missing_similar
    LookupProblem.PAGE_MISSING -> Res.string.error_command_page_missing
    LookupProblem.PAGE_MISSING_SIMILAR -> Res.string.error_command_page_missing_similar
    LookupProblem.PAGE_SECTION_MISSING -> Res.string.error_command_page_section_missing
    LookupProblem.PAGE_SECTION_MISSING_LISTED -> Res.string.error_command_page_section_missing_listed
    LookupProblem.PAGE_SECTION_MISSING_SIMILAR -> Res.string.error_command_page_section_missing_similar
    LookupProblem.PROPERTY_UNKNOWN -> Res.string.error_command_property_unknown
    LookupProblem.PROPERTY_UNKNOWN_SIMILAR -> Res.string.error_command_property_unknown_similar
    LookupProblem.RELATIONS_PROPERTY_MISSING -> Res.string.error_command_relations_property_missing
    LookupProblem.RELATIONS_PROPERTY_MISSING_SIMILAR -> Res.string.error_command_relations_property_missing_similar
    LookupProblem.SCENE_ACTOR_MISSING -> Res.string.error_command_scene_actor_missing
    LookupProblem.SCENE_ACTOR_MISSING_SIMILAR -> Res.string.error_command_scene_actor_missing_similar
    LookupProblem.SCENE_MISSING -> Res.string.error_command_scene_missing
    LookupProblem.SCENE_MISSING_SIMILAR -> Res.string.error_command_scene_missing_similar
    LookupProblem.SOUND_MISSING -> Res.string.error_command_sound_missing
    LookupProblem.SOUND_MISSING_SIMILAR -> Res.string.error_command_sound_missing_similar
    LookupProblem.SPRITE_MISSING -> Res.string.error_command_sprite_missing
    LookupProblem.SPRITE_MISSING_SIMILAR -> Res.string.error_command_sprite_missing_similar
    LookupProblem.TEXT_MISSING -> Res.string.error_command_text_missing
    LookupProblem.TEXT_MISSING_NONE -> Res.string.error_command_text_missing_none
    LookupProblem.TIMER_UNKNOWN -> Res.string.error_command_timer_unknown
    LookupProblem.TIMER_UNKNOWN_SIMILAR -> Res.string.error_command_timer_unknown_similar
    LookupProblem.TYPE_MISSING -> Res.string.error_command_type_missing
    LookupProblem.TYPE_MISSING_SIMILAR -> Res.string.error_command_type_missing_similar
    LookupProblem.UNKNOWN -> Res.string.error_command_unknown
    LookupProblem.UNKNOWN_SIMILAR -> Res.string.error_command_unknown_similar
}
