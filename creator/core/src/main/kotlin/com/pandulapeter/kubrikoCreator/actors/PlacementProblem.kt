/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

import com.pandulapeter.kubrikoCreator.foundation.DomainText

/**
 * Why an Event, a Block, or what they name doesn't fit where it was put. Events and Blocks are named by their titles in
 * the editor, with their IDs in brackets for the AI.
 */
public enum class PlacementProblem(override val english: String) : DomainText {
    EVENT_GAME_STARTED_IN_ACTOR_TYPE("Only a Manager type can have a When the game starts (gameStarted) Event, since an Actor type's Actors come into Scenes after the game started. Use When added to the Scene (addedToScene) instead."),
    EVENT_ADDED_TO_SCENE_IN_MANAGER_TYPE("A Manager type is never added to a Scene, so it can't have a When added to the Scene (addedToScene) Event. Use When the game starts (gameStarted) instead."),
    EVENT_EDGE_TOUCHED_IN_MANAGER_TYPE("A Manager type isn't in a Scene, so it can't have a When it reaches an edge of the Scene (edgeTouched) Event."),
    EVENT_LEFT_SCENE_IN_MANAGER_TYPE("A Manager type isn't in a Scene, so it can't have a When it leaves the Scene (leftScene) Event."),
    EVENT_COLLISION_IN_MANAGER_TYPE("A Manager type isn't in a Scene, so it can't have a When it collides with (collision) Event."),
    EVENT_ACTOR_ADDED_IN_ACTOR_TYPE("Only a Manager type can have a When an Actor is added (actorAdded) Event, since Manager types outlive every Scene. Give the Actor type itself a When added to the Scene (addedToScene) Event, or put this one in a Manager type."),
    EVENT_ACTOR_REMOVED_IN_ACTOR_TYPE("Only a Manager type can have a When an Actor is removed (actorRemoved) Event, since Manager types outlive every Scene. Put it in a Manager type."),
    BLOCK_BOUNCE_OFF_EDGE_OUTSIDE_EDGE_TOUCHED("Bounce off the edge (bounceOffEdge) works only in a When it reaches an edge of the Scene (edgeTouched) Event, the only one that knows which edge was touched."),
    BLOCK_BOUNCE_OFF_OTHER_OUTSIDE_COLLISION("Bounce off it (bounceOffOther) works only in a When it collides with (collision) Event, the only one that knows what was hit."),
    BLOCK_REMOVE_OTHER_OUTSIDE_COLLISION("Remove the other Actor (removeOther) works only in a When it collides with (collision) Event, the only one that knows the other Actor."),
    BLOCK_MOVE_WITH_KEYS_OUTSIDE_EVERY_FRAME("Move with keys (moveWithKeys) works only in an Every frame (everyFrame) Event, which checks the keys held on every frame."),
    BLOCK_MOVEMENT_IN_MANAGER_TYPE("A Manager type isn't in a Scene, so it has nothing for the Movement Blocks (setVelocity, addToVelocity, moveInDirection, moveWithKeys, stop, jumpTo, moveTowardsNearest, bounceOffEdge, and bounceOffOther) to move. They belong in an Actor type."),
    BLOCK_REMOVE_SELF_IN_MANAGER_TYPE("A Manager type isn't in a Scene, so it can't have a Remove this Actor (removeSelf) Block."),
    BLOCK_REMOVE_OTHER_IN_MANAGER_TYPE("A Manager type isn't in a Scene, so it can't have a Remove the other Actor (removeOther) Block."),
    BLOCK_ANIMATION_IN_MANAGER_TYPE("A Manager type isn't in a Scene and has no sprite, so it can't have the Set the frame (setFrame) or Set the animation (setAnimation) Blocks. They belong in an Actor type drawn as a sprite cut into frames."),
    BLOCK_APPEARANCE_IN_MANAGER_TYPE("A Manager type isn't in a Scene and isn't drawn, so it can't have the Set the color (setColor), Set the opacity (setOpacity), or Change the opacity (changeOpacity) Blocks. They belong in an Actor type."),
    BLOCK_ANIMATE_IN_MANAGER_TYPE("A Manager type isn't in a Scene and isn't drawn, so it can't have an Animate (animate) Block. It belongs in an Actor type."),
    BLOCK_RUN_PARENT_EVENT_IN_MANAGER_TYPE("Only Actor types have parents, so a Manager type can't have a Run the parent's Event (runParentEvent) Block."),
    CONDITION_NOT_ALLOWED("Only an Actor type has a velocity, so only its If Blocks can check it (the velocity condition)."),
    KEY_NOT_ALLOWED("Only a When a key is pressed (keyPressed) Event has a key."),
    OTHER_ACTOR_TYPE_NOT_ALLOWED("Only a When it collides with (collision), When an Actor is added (actorAdded), or When an Actor is removed (actorRemoved) Event has another Actor type."),
    TIMER_NOT_ALLOWED("Only a When a Timer finishes (timerFinished) Event has a Timer."),
    NUMBER_PROPERTY_MISSING("%2\$s has no number Property called %1\$s."),
    TEXT_PROPERTY_MISSING("%2\$s has no text Property called %1\$s."),
    TOGGLE_PROPERTY_MISSING("%2\$s has no toggle Property called %1\$s."),
    NUMBER_PROPERTY_MISSING_SIMILAR("%2\$s has no number Property called %1\$s. Close matches: %3\$s."),
    TEXT_PROPERTY_MISSING_SIMILAR("%2\$s has no text Property called %1\$s. Close matches: %3\$s."),
    TOGGLE_PROPERTY_MISSING_SIMILAR("%2\$s has no toggle Property called %1\$s. Close matches: %3\$s."),
    NUMBER_PROPERTY_OF_OTHER_KIND("%2\$s has a Property called %1\$s, but it isn't a number Property, so this Block can't use it. Use a Block for its kind, or another Property."),
    TEXT_PROPERTY_OF_OTHER_KIND("%2\$s has a Property called %1\$s, but it isn't a text Property, so this Block can't use it. Use a Block for its kind, or another Property."),
    TOGGLE_PROPERTY_OF_OTHER_KIND("%2\$s has a Property called %1\$s, but it isn't a toggle Property, so this Block can't use it. Use a Block for its kind, or another Property."),
}
