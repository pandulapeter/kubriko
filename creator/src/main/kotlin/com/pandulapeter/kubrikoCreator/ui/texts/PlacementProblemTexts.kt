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
import kubriko.creator.generated.resources.error_command_block_bounce_off_edge_outside_edge_touched
import kubriko.creator.generated.resources.error_command_block_bounce_off_other_outside_collision
import kubriko.creator.generated.resources.error_command_block_movement_in_manager_type
import kubriko.creator.generated.resources.error_command_block_remove_other_outside_collision
import kubriko.creator.generated.resources.error_command_block_remove_self_in_manager_type
import kubriko.creator.generated.resources.error_command_block_run_parent_event_in_manager_type
import kubriko.creator.generated.resources.error_command_event_added_to_scene_in_manager_type
import kubriko.creator.generated.resources.error_command_event_collision_in_manager_type
import kubriko.creator.generated.resources.error_command_event_edge_touched_in_manager_type
import kubriko.creator.generated.resources.error_command_event_game_started_in_actor_type
import kubriko.creator.generated.resources.error_command_number_property_missing
import kubriko.creator.generated.resources.error_command_text_property_missing
import kubriko.creator.generated.resources.error_command_toggle_property_missing
import org.jetbrains.compose.resources.StringResource

/**
 * The string the user is shown for a [CommandProblem] about what a type, an Event, or a Block can hold, which names Events
 * and Blocks as the editor does.
 */
internal fun CommandProblem.placementResource(): StringResource = when (this) {
    CommandProblem.EVENT_GAME_STARTED_IN_ACTOR_TYPE -> Res.string.error_command_event_game_started_in_actor_type
    CommandProblem.EVENT_ADDED_TO_SCENE_IN_MANAGER_TYPE -> Res.string.error_command_event_added_to_scene_in_manager_type
    CommandProblem.EVENT_EDGE_TOUCHED_IN_MANAGER_TYPE -> Res.string.error_command_event_edge_touched_in_manager_type
    CommandProblem.EVENT_COLLISION_IN_MANAGER_TYPE -> Res.string.error_command_event_collision_in_manager_type
    CommandProblem.BLOCK_BOUNCE_OFF_EDGE_OUTSIDE_EDGE_TOUCHED -> Res.string.error_command_block_bounce_off_edge_outside_edge_touched
    CommandProblem.BLOCK_BOUNCE_OFF_OTHER_OUTSIDE_COLLISION -> Res.string.error_command_block_bounce_off_other_outside_collision
    CommandProblem.BLOCK_REMOVE_OTHER_OUTSIDE_COLLISION -> Res.string.error_command_block_remove_other_outside_collision
    CommandProblem.BLOCK_MOVEMENT_IN_MANAGER_TYPE -> Res.string.error_command_block_movement_in_manager_type
    CommandProblem.BLOCK_REMOVE_SELF_IN_MANAGER_TYPE -> Res.string.error_command_block_remove_self_in_manager_type
    CommandProblem.BLOCK_RUN_PARENT_EVENT_IN_MANAGER_TYPE -> Res.string.error_command_block_run_parent_event_in_manager_type
    CommandProblem.NUMBER_PROPERTY_MISSING -> Res.string.error_command_number_property_missing
    CommandProblem.TEXT_PROPERTY_MISSING -> Res.string.error_command_text_property_missing
    CommandProblem.TOGGLE_PROPERTY_MISSING -> Res.string.error_command_toggle_property_missing
    else -> error("$this isn't about what a type, an Event, or a Block can hold.")
}
