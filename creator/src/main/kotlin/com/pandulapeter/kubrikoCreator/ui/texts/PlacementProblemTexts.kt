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

import com.pandulapeter.kubrikoCreator.actors.PlacementProblem
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_placement_block_animate_in_manager_type
import kubriko.creator.generated.resources.error_placement_block_animation_in_manager_type
import kubriko.creator.generated.resources.error_placement_block_appearance_in_manager_type
import kubriko.creator.generated.resources.error_placement_block_bounce_off_edge_outside_edge_touched
import kubriko.creator.generated.resources.error_placement_block_bounce_off_other_outside_collision
import kubriko.creator.generated.resources.error_placement_block_movement_in_manager_type
import kubriko.creator.generated.resources.error_placement_block_remove_other_in_manager_type
import kubriko.creator.generated.resources.error_placement_block_remove_other_outside_collision
import kubriko.creator.generated.resources.error_placement_block_remove_self_in_manager_type
import kubriko.creator.generated.resources.error_placement_block_run_parent_event_in_manager_type
import kubriko.creator.generated.resources.error_placement_condition_not_allowed
import kubriko.creator.generated.resources.error_placement_event_added_to_scene_in_manager_type
import kubriko.creator.generated.resources.error_placement_event_collision_in_manager_type
import kubriko.creator.generated.resources.error_placement_event_edge_touched_in_manager_type
import kubriko.creator.generated.resources.error_placement_event_game_started_in_actor_type
import kubriko.creator.generated.resources.error_placement_event_left_scene_in_manager_type
import kubriko.creator.generated.resources.error_placement_key_not_allowed
import kubriko.creator.generated.resources.error_placement_number_property_missing
import kubriko.creator.generated.resources.error_placement_number_property_missing_similar
import kubriko.creator.generated.resources.error_placement_number_property_of_other_kind
import kubriko.creator.generated.resources.error_placement_other_actor_type_not_allowed
import kubriko.creator.generated.resources.error_placement_text_property_missing
import kubriko.creator.generated.resources.error_placement_text_property_missing_similar
import kubriko.creator.generated.resources.error_placement_text_property_of_other_kind
import kubriko.creator.generated.resources.error_placement_timer_not_allowed
import kubriko.creator.generated.resources.error_placement_toggle_property_missing
import kubriko.creator.generated.resources.error_placement_toggle_property_missing_similar
import kubriko.creator.generated.resources.error_placement_toggle_property_of_other_kind
import org.jetbrains.compose.resources.StringResource

/**
 * The string the user is shown for this [PlacementProblem].
 */
internal fun PlacementProblem.toResource(): StringResource = when (this) {
    PlacementProblem.EVENT_GAME_STARTED_IN_ACTOR_TYPE -> Res.string.error_placement_event_game_started_in_actor_type
    PlacementProblem.EVENT_ADDED_TO_SCENE_IN_MANAGER_TYPE -> Res.string.error_placement_event_added_to_scene_in_manager_type
    PlacementProblem.EVENT_EDGE_TOUCHED_IN_MANAGER_TYPE -> Res.string.error_placement_event_edge_touched_in_manager_type
    PlacementProblem.EVENT_LEFT_SCENE_IN_MANAGER_TYPE -> Res.string.error_placement_event_left_scene_in_manager_type
    PlacementProblem.EVENT_COLLISION_IN_MANAGER_TYPE -> Res.string.error_placement_event_collision_in_manager_type
    PlacementProblem.BLOCK_BOUNCE_OFF_EDGE_OUTSIDE_EDGE_TOUCHED -> Res.string.error_placement_block_bounce_off_edge_outside_edge_touched
    PlacementProblem.BLOCK_BOUNCE_OFF_OTHER_OUTSIDE_COLLISION -> Res.string.error_placement_block_bounce_off_other_outside_collision
    PlacementProblem.BLOCK_REMOVE_OTHER_OUTSIDE_COLLISION -> Res.string.error_placement_block_remove_other_outside_collision
    PlacementProblem.BLOCK_MOVEMENT_IN_MANAGER_TYPE -> Res.string.error_placement_block_movement_in_manager_type
    PlacementProblem.BLOCK_REMOVE_SELF_IN_MANAGER_TYPE -> Res.string.error_placement_block_remove_self_in_manager_type
    PlacementProblem.BLOCK_REMOVE_OTHER_IN_MANAGER_TYPE -> Res.string.error_placement_block_remove_other_in_manager_type
    PlacementProblem.BLOCK_ANIMATE_IN_MANAGER_TYPE -> Res.string.error_placement_block_animate_in_manager_type
    PlacementProblem.BLOCK_ANIMATION_IN_MANAGER_TYPE -> Res.string.error_placement_block_animation_in_manager_type
    PlacementProblem.BLOCK_APPEARANCE_IN_MANAGER_TYPE -> Res.string.error_placement_block_appearance_in_manager_type
    PlacementProblem.BLOCK_RUN_PARENT_EVENT_IN_MANAGER_TYPE -> Res.string.error_placement_block_run_parent_event_in_manager_type
    PlacementProblem.CONDITION_NOT_ALLOWED -> Res.string.error_placement_condition_not_allowed
    PlacementProblem.KEY_NOT_ALLOWED -> Res.string.error_placement_key_not_allowed
    PlacementProblem.OTHER_ACTOR_TYPE_NOT_ALLOWED -> Res.string.error_placement_other_actor_type_not_allowed
    PlacementProblem.TIMER_NOT_ALLOWED -> Res.string.error_placement_timer_not_allowed
    PlacementProblem.NUMBER_PROPERTY_MISSING -> Res.string.error_placement_number_property_missing
    PlacementProblem.NUMBER_PROPERTY_MISSING_SIMILAR -> Res.string.error_placement_number_property_missing_similar
    PlacementProblem.TEXT_PROPERTY_MISSING -> Res.string.error_placement_text_property_missing
    PlacementProblem.TEXT_PROPERTY_MISSING_SIMILAR -> Res.string.error_placement_text_property_missing_similar
    PlacementProblem.TOGGLE_PROPERTY_MISSING -> Res.string.error_placement_toggle_property_missing
    PlacementProblem.TOGGLE_PROPERTY_MISSING_SIMILAR -> Res.string.error_placement_toggle_property_missing_similar
    PlacementProblem.NUMBER_PROPERTY_OF_OTHER_KIND -> Res.string.error_placement_number_property_of_other_kind
    PlacementProblem.TEXT_PROPERTY_OF_OTHER_KIND -> Res.string.error_placement_text_property_of_other_kind
    PlacementProblem.TOGGLE_PROPERTY_OF_OTHER_KIND -> Res.string.error_placement_toggle_property_of_other_kind
}
