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
import kubriko.creator.generated.resources.assistant_turn_cancelled
import kubriko.creator.generated.resources.error_command_actor_id_invalid
import kubriko.creator.generated.resources.error_command_actor_id_repeated
import kubriko.creator.generated.resources.error_command_actor_id_taken
import kubriko.creator.generated.resources.error_command_actor_property_missing
import kubriko.creator.generated.resources.error_command_actor_type_has_children
import kubriko.creator.generated.resources.error_command_actor_type_missing
import kubriko.creator.generated.resources.error_command_actors_empty
import kubriko.creator.generated.resources.error_command_already_under_version_control
import kubriko.creator.generated.resources.error_command_block_missing
import kubriko.creator.generated.resources.error_command_block_not_allowed
import kubriko.creator.generated.resources.error_command_change_missing
import kubriko.creator.generated.resources.error_command_changed_outside
import kubriko.creator.generated.resources.error_command_changes_not_written
import kubriko.creator.generated.resources.error_command_commit_conflicts
import kubriko.creator.generated.resources.error_command_commit_message_empty
import kubriko.creator.generated.resources.error_command_condition_not_allowed
import kubriko.creator.generated.resources.error_command_description_invalid
import kubriko.creator.generated.resources.error_command_event_missing
import kubriko.creator.generated.resources.error_command_event_not_allowed
import kubriko.creator.generated.resources.error_command_failed
import kubriko.creator.generated.resources.error_command_first_scene_not_followed
import kubriko.creator.generated.resources.error_command_game_notes_too_long
import kubriko.creator.generated.resources.error_command_game_settings_unavailable
import kubriko.creator.generated.resources.error_command_generated_code_not_written
import kubriko.creator.generated.resources.error_command_index_invalid
import kubriko.creator.generated.resources.error_command_input_invalid
import kubriko.creator.generated.resources.error_command_key_not_allowed
import kubriko.creator.generated.resources.error_command_manager_type_missing
import kubriko.creator.generated.resources.error_command_music_missing
import kubriko.creator.generated.resources.error_command_no_actor_types
import kubriko.creator.generated.resources.error_command_no_repository
import kubriko.creator.generated.resources.error_command_not_a_change
import kubriko.creator.generated.resources.error_command_not_changed_by_hand
import kubriko.creator.generated.resources.error_command_nothing_to_commit
import kubriko.creator.generated.resources.error_command_number_invalid
import kubriko.creator.generated.resources.error_command_other_actor_type_not_allowed
import kubriko.creator.generated.resources.error_command_page_invalid
import kubriko.creator.generated.resources.error_command_parent_cycle
import kubriko.creator.generated.resources.error_command_parent_property_kind
import kubriko.creator.generated.resources.error_command_play_duration_invalid
import kubriko.creator.generated.resources.error_command_project_settings_empty
import kubriko.creator.generated.resources.error_command_project_settings_unavailable
import kubriko.creator.generated.resources.error_command_property_inherited
import kubriko.creator.generated.resources.error_command_property_kind_changed
import kubriko.creator.generated.resources.error_command_property_kind_in_descendant
import kubriko.creator.generated.resources.error_command_property_missing
import kubriko.creator.generated.resources.error_command_property_unknown
import kubriko.creator.generated.resources.error_command_property_value_invalid
import kubriko.creator.generated.resources.error_command_relations_property_missing
import kubriko.creator.generated.resources.error_command_relations_subject
import kubriko.creator.generated.resources.error_command_scene_actor_missing
import kubriko.creator.generated.resources.error_command_scene_actor_not_block_built
import kubriko.creator.generated.resources.error_command_scene_actor_without_position
import kubriko.creator.generated.resources.error_command_scene_content_invalid
import kubriko.creator.generated.resources.error_command_scene_invalid
import kubriko.creator.generated.resources.error_command_scene_is_first
import kubriko.creator.generated.resources.error_command_scene_missing
import kubriko.creator.generated.resources.error_command_size_invalid
import kubriko.creator.generated.resources.error_command_sound_missing
import kubriko.creator.generated.resources.error_command_sprite_missing
import kubriko.creator.generated.resources.error_command_text_missing
import kubriko.creator.generated.resources.error_command_text_size_invalid
import kubriko.creator.generated.resources.error_command_timer_invalid
import kubriko.creator.generated.resources.error_command_timer_not_allowed
import kubriko.creator.generated.resources.error_command_timer_taken
import kubriko.creator.generated.resources.error_command_timer_unknown
import kubriko.creator.generated.resources.error_command_type_missing
import kubriko.creator.generated.resources.error_command_unknown
import kubriko.creator.generated.resources.error_command_upgrade_by_hand
import kubriko.creator.generated.resources.error_command_upgrade_not_needed
import kubriko.creator.generated.resources.error_folder_exists
import kubriko.creator.generated.resources.error_folder_missing
import kubriko.creator.generated.resources.error_folder_not_empty
import kubriko.creator.generated.resources.error_game_command_failed
import kubriko.creator.generated.resources.error_game_duration_invalid
import kubriko.creator.generated.resources.error_game_key_unknown
import kubriko.creator.generated.resources.error_game_keys_count
import kubriko.creator.generated.resources.error_game_no_answer
import kubriko.creator.generated.resources.error_game_not_running
import kubriko.creator.generated.resources.error_game_unsupported
import kubriko.creator.generated.resources.error_mcp_internal
import kubriko.creator.generated.resources.error_scene_folder_reserved
import kubriko.creator.generated.resources.error_scene_name_invalid
import kubriko.creator.generated.resources.error_scene_name_taken
import kubriko.creator.generated.resources.error_scene_not_moved
import kubriko.creator.generated.resources.warning_scene_named_in_code
import org.jetbrains.compose.resources.StringResource

/**
 * The string the user is shown for this [CommandProblem].
 */
internal fun CommandProblem.toResource(): StringResource = when (this) {
    CommandProblem.TURN_CANCELLED -> Res.string.assistant_turn_cancelled
    CommandProblem.ACTOR_ID_INVALID -> Res.string.error_command_actor_id_invalid
    CommandProblem.ACTOR_ID_REPEATED -> Res.string.error_command_actor_id_repeated
    CommandProblem.ACTOR_ID_TAKEN -> Res.string.error_command_actor_id_taken
    CommandProblem.ACTOR_PROPERTY_MISSING -> Res.string.error_command_actor_property_missing
    CommandProblem.ACTOR_TYPE_HAS_CHILDREN -> Res.string.error_command_actor_type_has_children
    CommandProblem.ACTOR_TYPE_MISSING -> Res.string.error_command_actor_type_missing
    CommandProblem.ACTORS_EMPTY -> Res.string.error_command_actors_empty
    CommandProblem.ALREADY_UNDER_VERSION_CONTROL -> Res.string.error_command_already_under_version_control
    CommandProblem.BLOCK_MISSING -> Res.string.error_command_block_missing
    CommandProblem.BLOCK_NOT_ALLOWED -> Res.string.error_command_block_not_allowed
    CommandProblem.CHANGE_MISSING -> Res.string.error_command_change_missing
    CommandProblem.CHANGED_OUTSIDE -> Res.string.error_command_changed_outside
    CommandProblem.CHANGES_NOT_WRITTEN -> Res.string.error_command_changes_not_written
    CommandProblem.COMMIT_CONFLICTS -> Res.string.error_command_commit_conflicts
    CommandProblem.COMMIT_MESSAGE_EMPTY -> Res.string.error_command_commit_message_empty
    CommandProblem.CONDITION_NOT_ALLOWED -> Res.string.error_command_condition_not_allowed
    CommandProblem.DESCRIPTION_INVALID -> Res.string.error_command_description_invalid
    CommandProblem.EVENT_MISSING -> Res.string.error_command_event_missing
    CommandProblem.EVENT_NOT_ALLOWED -> Res.string.error_command_event_not_allowed
    CommandProblem.FAILED -> Res.string.error_command_failed
    CommandProblem.GAME_NOTES_TOO_LONG -> Res.string.error_command_game_notes_too_long
    CommandProblem.GENERATED_CODE_NOT_WRITTEN -> Res.string.error_command_generated_code_not_written
    CommandProblem.INDEX_INVALID -> Res.string.error_command_index_invalid
    CommandProblem.INPUT_INVALID -> Res.string.error_command_input_invalid
    CommandProblem.KEY_NOT_ALLOWED -> Res.string.error_command_key_not_allowed
    CommandProblem.MANAGER_TYPE_MISSING -> Res.string.error_command_manager_type_missing
    CommandProblem.MUSIC_MISSING -> Res.string.error_command_music_missing
    CommandProblem.NO_ACTOR_TYPES -> Res.string.error_command_no_actor_types
    CommandProblem.NO_REPOSITORY -> Res.string.error_command_no_repository
    CommandProblem.NOT_A_CHANGE -> Res.string.error_command_not_a_change
    CommandProblem.NOT_CHANGED_BY_HAND -> Res.string.error_command_not_changed_by_hand
    CommandProblem.NOTHING_TO_COMMIT -> Res.string.error_command_nothing_to_commit
    CommandProblem.NUMBER_INVALID -> Res.string.error_command_number_invalid
    CommandProblem.OTHER_ACTOR_TYPE_NOT_ALLOWED -> Res.string.error_command_other_actor_type_not_allowed
    CommandProblem.PAGE_INVALID -> Res.string.error_command_page_invalid
    CommandProblem.PARENT_CYCLE -> Res.string.error_command_parent_cycle
    CommandProblem.PARENT_PROPERTY_KIND -> Res.string.error_command_parent_property_kind
    CommandProblem.PLAY_DURATION_INVALID -> Res.string.error_command_play_duration_invalid
    CommandProblem.FIRST_SCENE_NOT_FOLLOWED -> Res.string.error_command_first_scene_not_followed
    CommandProblem.GAME_SETTINGS_UNAVAILABLE -> Res.string.error_command_game_settings_unavailable
    CommandProblem.PROJECT_SETTINGS_EMPTY -> Res.string.error_command_project_settings_empty
    CommandProblem.PROJECT_SETTINGS_UNAVAILABLE -> Res.string.error_command_project_settings_unavailable
    CommandProblem.PROPERTY_INHERITED -> Res.string.error_command_property_inherited
    CommandProblem.PROPERTY_KIND_CHANGED -> Res.string.error_command_property_kind_changed
    CommandProblem.PROPERTY_KIND_IN_DESCENDANT -> Res.string.error_command_property_kind_in_descendant
    CommandProblem.PROPERTY_MISSING -> Res.string.error_command_property_missing
    CommandProblem.PROPERTY_UNKNOWN -> Res.string.error_command_property_unknown
    CommandProblem.PROPERTY_VALUE_INVALID -> Res.string.error_command_property_value_invalid
    CommandProblem.RELATIONS_PROPERTY_MISSING -> Res.string.error_command_relations_property_missing
    CommandProblem.RELATIONS_SUBJECT -> Res.string.error_command_relations_subject
    CommandProblem.SCENE_ACTOR_MISSING -> Res.string.error_command_scene_actor_missing
    CommandProblem.SCENE_ACTOR_NOT_BLOCK_BUILT -> Res.string.error_command_scene_actor_not_block_built
    CommandProblem.SCENE_ACTOR_WITHOUT_POSITION -> Res.string.error_command_scene_actor_without_position
    CommandProblem.SCENE_CONTENT_INVALID -> Res.string.error_command_scene_content_invalid
    CommandProblem.SCENE_INVALID -> Res.string.error_command_scene_invalid
    CommandProblem.SCENE_IS_FIRST -> Res.string.error_command_scene_is_first
    CommandProblem.SCENE_MISSING -> Res.string.error_command_scene_missing
    CommandProblem.SIZE_INVALID -> Res.string.error_command_size_invalid
    CommandProblem.SOUND_MISSING -> Res.string.error_command_sound_missing
    CommandProblem.SPRITE_MISSING -> Res.string.error_command_sprite_missing
    CommandProblem.TEXT_MISSING -> Res.string.error_command_text_missing
    CommandProblem.TEXT_SIZE_INVALID -> Res.string.error_command_text_size_invalid
    CommandProblem.TIMER_INVALID -> Res.string.error_command_timer_invalid
    CommandProblem.TIMER_NOT_ALLOWED -> Res.string.error_command_timer_not_allowed
    CommandProblem.TIMER_TAKEN -> Res.string.error_command_timer_taken
    CommandProblem.TIMER_UNKNOWN -> Res.string.error_command_timer_unknown
    CommandProblem.TYPE_MISSING -> Res.string.error_command_type_missing
    CommandProblem.UNKNOWN -> Res.string.error_command_unknown
    CommandProblem.UPGRADE_BY_HAND -> Res.string.error_command_upgrade_by_hand
    CommandProblem.UPGRADE_NOT_NEEDED -> Res.string.error_command_upgrade_not_needed
    CommandProblem.FOLDER_EXISTS -> Res.string.error_folder_exists
    CommandProblem.FOLDER_MISSING -> Res.string.error_folder_missing
    CommandProblem.FOLDER_NOT_EMPTY -> Res.string.error_folder_not_empty
    CommandProblem.GAME_COMMAND_FAILED -> Res.string.error_game_command_failed
    CommandProblem.GAME_DURATION_INVALID -> Res.string.error_game_duration_invalid
    CommandProblem.GAME_KEY_UNKNOWN -> Res.string.error_game_key_unknown
    CommandProblem.GAME_KEYS_COUNT -> Res.string.error_game_keys_count
    CommandProblem.GAME_NO_ANSWER -> Res.string.error_game_no_answer
    CommandProblem.GAME_NOT_RUNNING -> Res.string.error_game_not_running
    CommandProblem.GAME_UNSUPPORTED -> Res.string.error_game_unsupported
    CommandProblem.INTERNAL -> Res.string.error_mcp_internal
    CommandProblem.SCENE_FOLDER_RESERVED -> Res.string.error_scene_folder_reserved
    CommandProblem.SCENE_NAME_INVALID -> Res.string.error_scene_name_invalid
    CommandProblem.SCENE_NAME_TAKEN -> Res.string.error_scene_name_taken
    CommandProblem.SCENE_NOT_MOVED -> Res.string.error_scene_not_moved
    CommandProblem.SCENE_NAMED_IN_CODE -> Res.string.warning_scene_named_in_code
}
