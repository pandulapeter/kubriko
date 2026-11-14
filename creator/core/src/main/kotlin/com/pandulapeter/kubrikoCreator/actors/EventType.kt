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

import com.pandulapeter.kubrikoCreator.schema.Description
import com.pandulapeter.kubrikoCreator.schema.SummarizedSchema
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * When an Event's Blocks run.
 *
 * @param misplacedProblem Why a type that can't react to this Event ([isAvailableIn]) can't have it; set exactly for
 *   the Events some kind of type can't have (`EventTypeTest` holds them to it).
 */
@Serializable
@Description("When an Event happens.")
@SummarizedSchema("get_block_catalog describes each, and whether Actor types or Manager types have it.")
public enum class EventType(public val misplacedProblem: PlacementProblem? = null) {

    @Description("Once, when the game starts. Only Manager types have it.")
    @SerialName("gameStarted")
    GAME_STARTED(PlacementProblem.EVENT_GAME_STARTED_IN_ACTOR_TYPE),

    @Description("Once, when the Actor is added to the Scene. Only Actor types have it.")
    @SerialName("addedToScene")
    ADDED_TO_SCENE(PlacementProblem.EVENT_ADDED_TO_SCENE_IN_MANAGER_TYPE),

    @Description("On every frame (in an Actor type, before the Actor moves).")
    @SerialName("everyFrame")
    EVERY_FRAME,

    @Description("When the player presses the Event's key.")
    @SerialName("keyPressed")
    KEY_PRESSED,

    @Description("When the Actor starts reaching past an edge of the Scene (the screen's, while it has no bounds). Touching an edge doesn't stop the Actor: the Actor type's staysInsideScene does. Only Actor types have it.")
    @SerialName("edgeTouched")
    EDGE_TOUCHED(PlacementProblem.EVENT_EDGE_TOUCHED_IN_MANAGER_TYPE),

    @Description("Once, when the Actor, having been at least partly inside the Scene's bounds (the screen's, while it has none), is wholly outside them. An Actor added outside them fires it only after it came in, and one whose type stays inside the Scene never. Without bounds, moving the camera moves the screen's. Only Actor types have it.")
    @SerialName("leftScene")
    LEFT_SCENE(PlacementProblem.EVENT_LEFT_SCENE_IN_MANAGER_TYPE),

    @Description("On every frame the Actor overlaps an Actor of the Event's other Actor type, once per overlapping Actor. Only Actor types have it.")
    @SerialName("collision")
    COLLISION(PlacementProblem.EVENT_COLLISION_IN_MANAGER_TYPE),

    @Description("When the Event's Timer, started by a startTimer Block, finishes (each time, if it repeats).")
    @SerialName("timerFinished")
    TIMER_FINISHED,

    @Description("On the frame after an Actor of the Event's other Actor type (or of a type inheriting from it) joins the Scene, once per Actor: placed in a Scene that starts, or added by a Block. Only Manager types have it.")
    @SerialName("actorAdded")
    ACTOR_ADDED(PlacementProblem.EVENT_ACTOR_ADDED_IN_ACTOR_TYPE),

    @Description("On the frame after an Actor of the Event's other Actor type (or of a type inheriting from it) is removed, once per Actor; never for the Actors a Scene change takes away. Only Manager types have it.")
    @SerialName("actorRemoved")
    ACTOR_REMOVED(PlacementProblem.EVENT_ACTOR_REMOVED_IN_ACTOR_TYPE);

    /**
     * Whether the Event reacts to Actors of another Actor type, which it names in `otherActorType`.
     */
    public val takesActorType: Boolean
        get() = this == COLLISION || this == ACTOR_ADDED || this == ACTOR_REMOVED

    /**
     * Whether a type of [kind] can react to this Event. Managers aren't in the Scene, so they have no position, edges, or
     * collisions, and they start with the game and outlive every Scene, so they are the ones to hear about Actors coming
     * and going.
     */
    public fun isAvailableIn(kind: BlockTypeKind): Boolean = when (this) {
        GAME_STARTED, ACTOR_ADDED, ACTOR_REMOVED -> kind == BlockTypeKind.MANAGER
        ADDED_TO_SCENE, EDGE_TOUCHED, LEFT_SCENE, COLLISION -> kind == BlockTypeKind.ACTOR
        EVERY_FRAME, KEY_PRESSED, TIMER_FINISHED -> true
    }
}
