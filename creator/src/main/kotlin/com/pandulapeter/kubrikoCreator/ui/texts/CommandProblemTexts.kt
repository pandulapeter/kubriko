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
import kubriko.creator.generated.resources.error_audio_folder_reserved
import kubriko.creator.generated.resources.error_blocks_nothing_to_replace
import kubriko.creator.generated.resources.error_command_actor_id_empty
import kubriko.creator.generated.resources.error_command_actor_id_invalid
import kubriko.creator.generated.resources.error_command_actor_id_letter_beyond_a_to_z
import kubriko.creator.generated.resources.error_command_actor_id_repeated
import kubriko.creator.generated.resources.error_command_actor_id_taken
import kubriko.creator.generated.resources.error_command_actor_type_has_children
import kubriko.creator.generated.resources.error_command_actors_empty
import kubriko.creator.generated.resources.error_command_already_under_version_control
import kubriko.creator.generated.resources.error_command_change_missing
import kubriko.creator.generated.resources.error_command_changed_outside
import kubriko.creator.generated.resources.error_command_changes_not_written
import kubriko.creator.generated.resources.error_command_commit_conflicts
import kubriko.creator.generated.resources.error_command_commit_message_empty
import kubriko.creator.generated.resources.error_command_description_invalid
import kubriko.creator.generated.resources.error_command_document_not_moved
import kubriko.creator.generated.resources.error_command_document_taken
import kubriko.creator.generated.resources.error_command_document_too_long
import kubriko.creator.generated.resources.error_command_document_unreadable
import kubriko.creator.generated.resources.error_command_failed
import kubriko.creator.generated.resources.error_command_first_scene_not_followed
import kubriko.creator.generated.resources.error_command_game_notes_too_long
import kubriko.creator.generated.resources.error_command_game_settings_unavailable
import kubriko.creator.generated.resources.error_command_generated_code_not_written
import kubriko.creator.generated.resources.error_command_input_invalid
import kubriko.creator.generated.resources.error_command_markdown_missing
import kubriko.creator.generated.resources.error_command_mentions_not_followed
import kubriko.creator.generated.resources.error_command_no_actor_types
import kubriko.creator.generated.resources.error_command_no_repository
import kubriko.creator.generated.resources.error_command_not_a_change
import kubriko.creator.generated.resources.error_command_not_changed_by_hand
import kubriko.creator.generated.resources.error_command_nothing_to_commit
import kubriko.creator.generated.resources.error_command_number_invalid
import kubriko.creator.generated.resources.error_command_page_image_not_viewable
import kubriko.creator.generated.resources.error_command_page_image_source
import kubriko.creator.generated.resources.error_command_page_image_unreadable
import kubriko.creator.generated.resources.error_command_page_invalid
import kubriko.creator.generated.resources.error_command_page_not_moved
import kubriko.creator.generated.resources.error_command_page_taken
import kubriko.creator.generated.resources.error_command_page_too_long
import kubriko.creator.generated.resources.error_command_parent_cycle
import kubriko.creator.generated.resources.error_command_parent_property_kind
import kubriko.creator.generated.resources.error_command_plan_invalid
import kubriko.creator.generated.resources.error_command_plan_outside_panel
import kubriko.creator.generated.resources.error_command_play_duration_invalid
import kubriko.creator.generated.resources.error_command_play_target_missing
import kubriko.creator.generated.resources.error_command_project_settings_empty
import kubriko.creator.generated.resources.error_command_project_settings_unavailable
import kubriko.creator.generated.resources.error_command_property_inherited
import kubriko.creator.generated.resources.error_command_property_kind_changed
import kubriko.creator.generated.resources.error_command_property_kind_in_descendant
import kubriko.creator.generated.resources.error_command_property_value_invalid
import kubriko.creator.generated.resources.error_command_question_invalid
import kubriko.creator.generated.resources.error_command_relations_subject
import kubriko.creator.generated.resources.error_command_resource_content_mismatch
import kubriko.creator.generated.resources.error_command_resource_folder_not_moved
import kubriko.creator.generated.resources.error_command_resource_kind_changed
import kubriko.creator.generated.resources.error_command_resource_not_deleted
import kubriko.creator.generated.resources.error_command_resource_not_moved
import kubriko.creator.generated.resources.error_command_resource_not_moved_back
import kubriko.creator.generated.resources.error_command_resource_path_invalid
import kubriko.creator.generated.resources.error_command_resource_source_extension
import kubriko.creator.generated.resources.error_command_resource_source_unreadable
import kubriko.creator.generated.resources.error_command_resource_taken
import kubriko.creator.generated.resources.error_command_resource_unchanged
import kubriko.creator.generated.resources.error_command_scene_actor_not_block_built
import kubriko.creator.generated.resources.error_command_scene_actor_without_position
import kubriko.creator.generated.resources.error_command_scene_content_invalid
import kubriko.creator.generated.resources.error_command_scene_invalid
import kubriko.creator.generated.resources.error_command_scene_is_first
import kubriko.creator.generated.resources.error_command_screenplay_settings_unreadable
import kubriko.creator.generated.resources.error_command_screenplay_style
import kubriko.creator.generated.resources.error_command_size_invalid
import kubriko.creator.generated.resources.error_command_summary_invalid
import kubriko.creator.generated.resources.error_command_text_size_invalid
import kubriko.creator.generated.resources.error_command_timer_taken
import kubriko.creator.generated.resources.error_command_upgrade_by_hand
import kubriko.creator.generated.resources.error_command_upgrade_not_needed
import kubriko.creator.generated.resources.error_folder_already_there
import kubriko.creator.generated.resources.error_folder_exists
import kubriko.creator.generated.resources.error_folder_into_itself
import kubriko.creator.generated.resources.error_folder_not_empty
import kubriko.creator.generated.resources.error_folder_top
import kubriko.creator.generated.resources.error_game_command_failed
import kubriko.creator.generated.resources.error_game_duration_invalid
import kubriko.creator.generated.resources.error_game_keys_count
import kubriko.creator.generated.resources.error_game_keys_or_steps
import kubriko.creator.generated.resources.error_game_no_answer
import kubriko.creator.generated.resources.error_game_not_on_desktop
import kubriko.creator.generated.resources.error_game_not_running
import kubriko.creator.generated.resources.error_game_steps_count
import kubriko.creator.generated.resources.error_game_steps_duration
import kubriko.creator.generated.resources.error_game_unsupported
import kubriko.creator.generated.resources.error_mcp_internal
import kubriko.creator.generated.resources.error_not_a_resource_group
import kubriko.creator.generated.resources.error_pixels_letter_unknown
import kubriko.creator.generated.resources.error_pixels_none
import kubriko.creator.generated.resources.error_pixels_outside_frame
import kubriko.creator.generated.resources.error_pixels_palette_key
import kubriko.creator.generated.resources.error_scene_folder_reserved
import kubriko.creator.generated.resources.error_scene_name_taken
import kubriko.creator.generated.resources.error_scene_not_moved
import kubriko.creator.generated.resources.error_sprite_frame_missing
import kubriko.creator.generated.resources.error_sprite_frame_size_alone
import kubriko.creator.generated.resources.error_sprite_not_moved
import kubriko.creator.generated.resources.error_sprite_not_written
import kubriko.creator.generated.resources.error_sprite_path_invalid
import kubriko.creator.generated.resources.error_sprite_scale_invalid
import kubriko.creator.generated.resources.error_sprite_sheet_does_not_fit
import kubriko.creator.generated.resources.error_sprite_sheet_not_written
import kubriko.creator.generated.resources.error_sprite_size_invalid
import kubriko.creator.generated.resources.error_sprite_too_large_for_rows
import kubriko.creator.generated.resources.error_sprite_unreadable
import kubriko.creator.generated.resources.warning_resource_music_not_followed
import kubriko.creator.generated.resources.warning_resource_named_in_code
import kubriko.creator.generated.resources.warning_scene_named_in_code
import org.jetbrains.compose.resources.StringResource

/**
 * The string the user is shown for this [CommandProblem].
 */
internal fun CommandProblem.toResource(): StringResource = when (this) {
    CommandProblem.TURN_CANCELLED -> Res.string.assistant_turn_cancelled
    CommandProblem.ACTOR_ID_EMPTY -> Res.string.error_command_actor_id_empty
    CommandProblem.ACTOR_ID_LETTER_BEYOND_A_TO_Z -> Res.string.error_command_actor_id_letter_beyond_a_to_z
    CommandProblem.ACTOR_ID_INVALID -> Res.string.error_command_actor_id_invalid
    CommandProblem.ACTOR_ID_REPEATED -> Res.string.error_command_actor_id_repeated
    CommandProblem.ACTOR_ID_TAKEN -> Res.string.error_command_actor_id_taken
    CommandProblem.ACTOR_TYPE_HAS_CHILDREN -> Res.string.error_command_actor_type_has_children
    CommandProblem.ACTORS_EMPTY -> Res.string.error_command_actors_empty
    CommandProblem.ALREADY_UNDER_VERSION_CONTROL -> Res.string.error_command_already_under_version_control
    CommandProblem.AUDIO_FOLDER_RESERVED -> Res.string.error_audio_folder_reserved
    CommandProblem.BLOCKS_NOTHING_TO_REPLACE -> Res.string.error_blocks_nothing_to_replace
    CommandProblem.CHANGE_MISSING -> Res.string.error_command_change_missing
    CommandProblem.CHANGED_OUTSIDE -> Res.string.error_command_changed_outside
    CommandProblem.CHANGES_NOT_WRITTEN -> Res.string.error_command_changes_not_written
    CommandProblem.COMMIT_CONFLICTS -> Res.string.error_command_commit_conflicts
    CommandProblem.COMMIT_MESSAGE_EMPTY -> Res.string.error_command_commit_message_empty
    CommandProblem.DESCRIPTION_INVALID -> Res.string.error_command_description_invalid
    CommandProblem.FAILED -> Res.string.error_command_failed
    CommandProblem.GAME_NOTES_TOO_LONG -> Res.string.error_command_game_notes_too_long
    CommandProblem.GENERATED_CODE_NOT_WRITTEN -> Res.string.error_command_generated_code_not_written
    CommandProblem.INPUT_INVALID -> Res.string.error_command_input_invalid
    CommandProblem.NO_ACTOR_TYPES -> Res.string.error_command_no_actor_types
    CommandProblem.NO_REPOSITORY -> Res.string.error_command_no_repository
    CommandProblem.NOT_A_CHANGE -> Res.string.error_command_not_a_change
    CommandProblem.NOT_CHANGED_BY_HAND -> Res.string.error_command_not_changed_by_hand
    CommandProblem.NOTHING_TO_COMMIT -> Res.string.error_command_nothing_to_commit
    CommandProblem.NUMBER_INVALID -> Res.string.error_command_number_invalid
    CommandProblem.DOCUMENT_NOT_MOVED -> Res.string.error_command_document_not_moved
    CommandProblem.DOCUMENT_TAKEN -> Res.string.error_command_document_taken
    CommandProblem.DOCUMENT_TOO_LONG -> Res.string.error_command_document_too_long
    CommandProblem.DOCUMENT_UNREADABLE -> Res.string.error_command_document_unreadable
    CommandProblem.PAGE_NOT_MOVED -> Res.string.error_command_page_not_moved
    CommandProblem.MENTIONS_NOT_FOLLOWED -> Res.string.error_command_mentions_not_followed
    CommandProblem.MARKDOWN_MISSING -> Res.string.error_command_markdown_missing
    CommandProblem.PAGE_IMAGE_SOURCE -> Res.string.error_command_page_image_source
    CommandProblem.PAGE_IMAGE_UNREADABLE -> Res.string.error_command_page_image_unreadable
    CommandProblem.PAGE_IMAGE_NOT_VIEWABLE -> Res.string.error_command_page_image_not_viewable
    CommandProblem.SCREENPLAY_STYLE -> Res.string.error_command_screenplay_style
    CommandProblem.SCREENPLAY_SETTINGS_UNREADABLE -> Res.string.error_command_screenplay_settings_unreadable
    CommandProblem.QUESTION_INVALID -> Res.string.error_command_question_invalid
    CommandProblem.PLAN_INVALID -> Res.string.error_command_plan_invalid
    CommandProblem.PLAN_OUTSIDE_PANEL -> Res.string.error_command_plan_outside_panel
    CommandProblem.SUMMARY_INVALID -> Res.string.error_command_summary_invalid
    CommandProblem.PAGE_TAKEN -> Res.string.error_command_page_taken
    CommandProblem.PAGE_TOO_LONG -> Res.string.error_command_page_too_long
    CommandProblem.PAGE_INVALID -> Res.string.error_command_page_invalid
    CommandProblem.PARENT_CYCLE -> Res.string.error_command_parent_cycle
    CommandProblem.PARENT_PROPERTY_KIND -> Res.string.error_command_parent_property_kind
    CommandProblem.PLAY_DURATION_INVALID -> Res.string.error_command_play_duration_invalid
    CommandProblem.PLAY_TARGET_MISSING -> Res.string.error_command_play_target_missing
    CommandProblem.FIRST_SCENE_NOT_FOLLOWED -> Res.string.error_command_first_scene_not_followed
    CommandProblem.GAME_SETTINGS_UNAVAILABLE -> Res.string.error_command_game_settings_unavailable
    CommandProblem.PROJECT_SETTINGS_EMPTY -> Res.string.error_command_project_settings_empty
    CommandProblem.PROJECT_SETTINGS_UNAVAILABLE -> Res.string.error_command_project_settings_unavailable
    CommandProblem.PROPERTY_INHERITED -> Res.string.error_command_property_inherited
    CommandProblem.PROPERTY_KIND_CHANGED -> Res.string.error_command_property_kind_changed
    CommandProblem.PROPERTY_KIND_IN_DESCENDANT -> Res.string.error_command_property_kind_in_descendant
    CommandProblem.PROPERTY_VALUE_INVALID -> Res.string.error_command_property_value_invalid
    CommandProblem.RELATIONS_SUBJECT -> Res.string.error_command_relations_subject
    CommandProblem.RESOURCE_CONTENT_MISMATCH -> Res.string.error_command_resource_content_mismatch
    CommandProblem.RESOURCE_KIND_CHANGED -> Res.string.error_command_resource_kind_changed
    CommandProblem.RESOURCE_MUSIC_NOT_FOLLOWED -> Res.string.warning_resource_music_not_followed
    CommandProblem.RESOURCE_NAMED_IN_CODE -> Res.string.warning_resource_named_in_code
    CommandProblem.RESOURCE_FOLDER_NOT_MOVED -> Res.string.error_command_resource_folder_not_moved
    CommandProblem.RESOURCE_NOT_MOVED_BACK -> Res.string.error_command_resource_not_moved_back
    CommandProblem.RESOURCE_NOT_DELETED -> Res.string.error_command_resource_not_deleted
    CommandProblem.RESOURCE_NOT_MOVED -> Res.string.error_command_resource_not_moved
    CommandProblem.RESOURCE_PATH_INVALID -> Res.string.error_command_resource_path_invalid
    CommandProblem.RESOURCE_SOURCE_EXTENSION -> Res.string.error_command_resource_source_extension
    CommandProblem.RESOURCE_SOURCE_UNREADABLE -> Res.string.error_command_resource_source_unreadable
    CommandProblem.RESOURCE_TAKEN -> Res.string.error_command_resource_taken
    CommandProblem.RESOURCE_UNCHANGED -> Res.string.error_command_resource_unchanged
    CommandProblem.SCENE_ACTOR_NOT_BLOCK_BUILT -> Res.string.error_command_scene_actor_not_block_built
    CommandProblem.SCENE_ACTOR_WITHOUT_POSITION -> Res.string.error_command_scene_actor_without_position
    CommandProblem.SCENE_CONTENT_INVALID -> Res.string.error_command_scene_content_invalid
    CommandProblem.SCENE_INVALID -> Res.string.error_command_scene_invalid
    CommandProblem.SCENE_IS_FIRST -> Res.string.error_command_scene_is_first
    CommandProblem.SIZE_INVALID -> Res.string.error_command_size_invalid
    CommandProblem.TEXT_SIZE_INVALID -> Res.string.error_command_text_size_invalid
    CommandProblem.TIMER_TAKEN -> Res.string.error_command_timer_taken
    CommandProblem.UPGRADE_BY_HAND -> Res.string.error_command_upgrade_by_hand
    CommandProblem.UPGRADE_NOT_NEEDED -> Res.string.error_command_upgrade_not_needed
    CommandProblem.FOLDER_EXISTS -> Res.string.error_folder_exists
    CommandProblem.FOLDER_TOP -> Res.string.error_folder_top
    CommandProblem.FOLDER_INTO_ITSELF -> Res.string.error_folder_into_itself
    CommandProblem.FOLDER_ALREADY_THERE -> Res.string.error_folder_already_there
    CommandProblem.FOLDER_NOT_EMPTY -> Res.string.error_folder_not_empty
    CommandProblem.GAME_COMMAND_FAILED -> Res.string.error_game_command_failed
    CommandProblem.GAME_DURATION_INVALID -> Res.string.error_game_duration_invalid
    CommandProblem.GAME_KEYS_COUNT -> Res.string.error_game_keys_count
    CommandProblem.GAME_KEYS_OR_STEPS -> Res.string.error_game_keys_or_steps
    CommandProblem.GAME_STEPS_COUNT -> Res.string.error_game_steps_count
    CommandProblem.GAME_STEPS_DURATION -> Res.string.error_game_steps_duration
    CommandProblem.GAME_NO_ANSWER -> Res.string.error_game_no_answer
    CommandProblem.GAME_NOT_ON_DESKTOP -> Res.string.error_game_not_on_desktop
    CommandProblem.GAME_NOT_RUNNING -> Res.string.error_game_not_running
    CommandProblem.GAME_UNSUPPORTED -> Res.string.error_game_unsupported
    CommandProblem.INTERNAL -> Res.string.error_mcp_internal
    CommandProblem.NOT_A_RESOURCE_GROUP -> Res.string.error_not_a_resource_group
    CommandProblem.SPRITE_NOT_MOVED -> Res.string.error_sprite_not_moved
    CommandProblem.PIXELS_NONE -> Res.string.error_pixels_none
    CommandProblem.PIXELS_PALETTE_KEY -> Res.string.error_pixels_palette_key
    CommandProblem.PIXELS_LETTER_UNKNOWN -> Res.string.error_pixels_letter_unknown
    CommandProblem.PIXELS_OUTSIDE_FRAME -> Res.string.error_pixels_outside_frame
    CommandProblem.SPRITE_SIZE_INVALID -> Res.string.error_sprite_size_invalid
    CommandProblem.SPRITE_SCALE_INVALID -> Res.string.error_sprite_scale_invalid
    CommandProblem.SPRITE_FRAME_SIZE_ALONE -> Res.string.error_sprite_frame_size_alone
    CommandProblem.SPRITE_PATH_INVALID -> Res.string.error_sprite_path_invalid
    CommandProblem.SPRITE_TOO_LARGE_FOR_ROWS -> Res.string.error_sprite_too_large_for_rows
    CommandProblem.SPRITE_UNREADABLE -> Res.string.error_sprite_unreadable
    CommandProblem.SPRITE_NOT_WRITTEN -> Res.string.error_sprite_not_written
    CommandProblem.SPRITE_FRAME_MISSING -> Res.string.error_sprite_frame_missing
    CommandProblem.SPRITE_SHEET_NOT_WRITTEN -> Res.string.error_sprite_sheet_not_written
    CommandProblem.SPRITE_SHEET_DOES_NOT_FIT -> Res.string.error_sprite_sheet_does_not_fit
    CommandProblem.SCENE_FOLDER_RESERVED -> Res.string.error_scene_folder_reserved
    CommandProblem.SCENE_NAME_TAKEN -> Res.string.error_scene_name_taken
    CommandProblem.SCENE_NOT_MOVED -> Res.string.error_scene_not_moved
    CommandProblem.SCENE_NAMED_IN_CODE -> Res.string.warning_scene_named_in_code
}
