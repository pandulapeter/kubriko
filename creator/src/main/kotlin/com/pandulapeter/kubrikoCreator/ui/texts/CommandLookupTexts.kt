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

import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_command_actor_property_missing_similar
import kubriko.creator.generated.resources.error_command_actor_type_is_manager_type
import kubriko.creator.generated.resources.error_command_actor_type_missing_similar
import kubriko.creator.generated.resources.error_command_block_insertion_index_invalid
import kubriko.creator.generated.resources.error_command_block_missing_none
import kubriko.creator.generated.resources.error_command_blocks_count_too_large
import kubriko.creator.generated.resources.error_command_document_missing_similar
import kubriko.creator.generated.resources.error_command_event_missing_none
import kubriko.creator.generated.resources.error_command_folder_missing_similar
import kubriko.creator.generated.resources.error_command_game_key_unknown_similar
import kubriko.creator.generated.resources.error_command_manager_type_is_actor_type
import kubriko.creator.generated.resources.error_command_manager_type_missing_similar
import kubriko.creator.generated.resources.error_command_music_missing_similar
import kubriko.creator.generated.resources.error_command_page_missing_similar
import kubriko.creator.generated.resources.error_command_page_section_missing_listed
import kubriko.creator.generated.resources.error_command_property_unknown_similar
import kubriko.creator.generated.resources.error_command_relations_property_missing_similar
import kubriko.creator.generated.resources.error_command_scene_actor_missing_similar
import kubriko.creator.generated.resources.error_command_scene_missing_similar
import kubriko.creator.generated.resources.error_command_sound_missing_similar
import kubriko.creator.generated.resources.error_command_sprite_missing_similar
import kubriko.creator.generated.resources.error_command_text_missing_none
import kubriko.creator.generated.resources.error_command_timer_unknown_similar
import kubriko.creator.generated.resources.error_command_type_missing_similar
import kubriko.creator.generated.resources.error_command_unknown_similar
import org.jetbrains.compose.resources.StringResource

/**
 * The string the user is shown for this [CommandProblem] if it names what exists in place of what a command asked for
 * (similar names, the indexes there are), or null.
 */
internal fun CommandProblem.lookupResource(): StringResource? = when (this) {
    CommandProblem.ACTOR_PROPERTY_MISSING_SIMILAR -> Res.string.error_command_actor_property_missing_similar
    CommandProblem.ACTOR_TYPE_MISSING_SIMILAR -> Res.string.error_command_actor_type_missing_similar
    CommandProblem.ACTOR_TYPE_IS_MANAGER_TYPE -> Res.string.error_command_actor_type_is_manager_type
    CommandProblem.BLOCK_MISSING_NONE -> Res.string.error_command_block_missing_none
    CommandProblem.BLOCK_INSERTION_INDEX_INVALID -> Res.string.error_command_block_insertion_index_invalid
    CommandProblem.BLOCKS_COUNT_TOO_LARGE -> Res.string.error_command_blocks_count_too_large
    CommandProblem.DOCUMENT_MISSING_SIMILAR -> Res.string.error_command_document_missing_similar
    CommandProblem.EVENT_MISSING_NONE -> Res.string.error_command_event_missing_none
    CommandProblem.MANAGER_TYPE_MISSING_SIMILAR -> Res.string.error_command_manager_type_missing_similar
    CommandProblem.MANAGER_TYPE_IS_ACTOR_TYPE -> Res.string.error_command_manager_type_is_actor_type
    CommandProblem.MUSIC_MISSING_SIMILAR -> Res.string.error_command_music_missing_similar
    CommandProblem.PAGE_MISSING_SIMILAR -> Res.string.error_command_page_missing_similar
    CommandProblem.PAGE_SECTION_MISSING_LISTED -> Res.string.error_command_page_section_missing_listed
    CommandProblem.PROPERTY_UNKNOWN_SIMILAR -> Res.string.error_command_property_unknown_similar
    CommandProblem.RELATIONS_PROPERTY_MISSING_SIMILAR -> Res.string.error_command_relations_property_missing_similar
    CommandProblem.SCENE_ACTOR_MISSING_SIMILAR -> Res.string.error_command_scene_actor_missing_similar
    CommandProblem.SCENE_MISSING_SIMILAR -> Res.string.error_command_scene_missing_similar
    CommandProblem.SOUND_MISSING_SIMILAR -> Res.string.error_command_sound_missing_similar
    CommandProblem.SPRITE_MISSING_SIMILAR -> Res.string.error_command_sprite_missing_similar
    CommandProblem.TEXT_MISSING_NONE -> Res.string.error_command_text_missing_none
    CommandProblem.TIMER_UNKNOWN_SIMILAR -> Res.string.error_command_timer_unknown_similar
    CommandProblem.TYPE_MISSING_SIMILAR -> Res.string.error_command_type_missing_similar
    CommandProblem.UNKNOWN_SIMILAR -> Res.string.error_command_unknown_similar
    CommandProblem.FOLDER_MISSING_SIMILAR -> Res.string.error_command_folder_missing_similar
    CommandProblem.GAME_KEY_UNKNOWN_SIMILAR -> Res.string.error_command_game_key_unknown_similar
    else -> null
}
