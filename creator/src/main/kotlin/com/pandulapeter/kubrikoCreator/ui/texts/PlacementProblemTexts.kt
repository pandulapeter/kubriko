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

import com.pandulapeter.kubrikoCreator.commands.PlacementProblem
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_placement_block_movement_in_manager_type
import kubriko.creator.generated.resources.error_placement_block_needs_actor_in_manager_type
import kubriko.creator.generated.resources.error_placement_block_only_in_another_event
import kubriko.creator.generated.resources.error_placement_block_only_in_collision
import kubriko.creator.generated.resources.error_placement_block_only_in_edge_touched
import kubriko.creator.generated.resources.error_placement_block_remove_other_in_manager_type
import kubriko.creator.generated.resources.error_placement_block_remove_self_in_manager_type
import kubriko.creator.generated.resources.error_placement_block_run_parent_event_in_manager_type
import kubriko.creator.generated.resources.error_placement_condition_not_allowed
import kubriko.creator.generated.resources.error_placement_event_added_to_scene_in_manager_type
import kubriko.creator.generated.resources.error_placement_event_collision_in_manager_type
import kubriko.creator.generated.resources.error_placement_event_edge_touched_in_manager_type
import kubriko.creator.generated.resources.error_placement_event_game_started_in_actor_type
import kubriko.creator.generated.resources.error_placement_event_only_in_actor_type
import kubriko.creator.generated.resources.error_placement_event_only_in_manager_type
import kubriko.creator.generated.resources.error_placement_key_not_allowed
import kubriko.creator.generated.resources.error_placement_number_property_missing
import kubriko.creator.generated.resources.error_placement_other_actor_type_not_allowed
import kubriko.creator.generated.resources.error_placement_text_property_missing
import kubriko.creator.generated.resources.error_placement_timer_not_allowed
import kubriko.creator.generated.resources.error_placement_toggle_property_missing
import org.jetbrains.compose.resources.StringResource

/**
 * The string the user is shown for this [PlacementProblem].
 */
internal fun PlacementProblem.toResource(): StringResource = when (this) {
    PlacementProblem.EVENT_GAME_STARTED_IN_ACTOR_TYPE -> Res.string.error_placement_event_game_started_in_actor_type
    PlacementProblem.EVENT_ADDED_TO_SCENE_IN_MANAGER_TYPE -> Res.string.error_placement_event_added_to_scene_in_manager_type
    PlacementProblem.EVENT_EDGE_TOUCHED_IN_MANAGER_TYPE -> Res.string.error_placement_event_edge_touched_in_manager_type
    PlacementProblem.EVENT_COLLISION_IN_MANAGER_TYPE -> Res.string.error_placement_event_collision_in_manager_type
    PlacementProblem.EVENT_ONLY_IN_ACTOR_TYPE -> Res.string.error_placement_event_only_in_actor_type
    PlacementProblem.EVENT_ONLY_IN_MANAGER_TYPE -> Res.string.error_placement_event_only_in_manager_type
    PlacementProblem.BLOCK_ONLY_IN_EDGE_TOUCHED -> Res.string.error_placement_block_only_in_edge_touched
    PlacementProblem.BLOCK_ONLY_IN_COLLISION -> Res.string.error_placement_block_only_in_collision
    PlacementProblem.BLOCK_ONLY_IN_ANOTHER_EVENT -> Res.string.error_placement_block_only_in_another_event
    PlacementProblem.BLOCK_MOVEMENT_IN_MANAGER_TYPE -> Res.string.error_placement_block_movement_in_manager_type
    PlacementProblem.BLOCK_REMOVE_SELF_IN_MANAGER_TYPE -> Res.string.error_placement_block_remove_self_in_manager_type
    PlacementProblem.BLOCK_REMOVE_OTHER_IN_MANAGER_TYPE -> Res.string.error_placement_block_remove_other_in_manager_type
    PlacementProblem.BLOCK_RUN_PARENT_EVENT_IN_MANAGER_TYPE -> Res.string.error_placement_block_run_parent_event_in_manager_type
    PlacementProblem.BLOCK_NEEDS_ACTOR_IN_MANAGER_TYPE -> Res.string.error_placement_block_needs_actor_in_manager_type
    PlacementProblem.CONDITION_NOT_ALLOWED -> Res.string.error_placement_condition_not_allowed
    PlacementProblem.KEY_NOT_ALLOWED -> Res.string.error_placement_key_not_allowed
    PlacementProblem.OTHER_ACTOR_TYPE_NOT_ALLOWED -> Res.string.error_placement_other_actor_type_not_allowed
    PlacementProblem.TIMER_NOT_ALLOWED -> Res.string.error_placement_timer_not_allowed
    PlacementProblem.NUMBER_PROPERTY_MISSING -> Res.string.error_placement_number_property_missing
    PlacementProblem.TEXT_PROPERTY_MISSING -> Res.string.error_placement_text_property_missing
    PlacementProblem.TOGGLE_PROPERTY_MISSING -> Res.string.error_placement_toggle_property_missing
}
