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

/**
 * The palette of Blocks, in the order the editor offers them, with every fact that is the same for each Block of a kind.
 *
 * @param managerTypeProblem Why a Manager type can't have the Block, if it moves or removes the Actor (or runs its parent's
 *   Event), which a Manager type doesn't have; set exactly when the Block [needsActor].
 * @param onlyIn The only Event the Block works in, if it needs what only that Event knows: which edge was touched, or
 *   which Actor was collided with.
 * @param misplacedComment The comment the Block's code becomes in any other Event; set exactly when [onlyIn] is.
 * @param otherEventProblem Why the Block can't be in any other Event; set exactly when [onlyIn] is.
 */
public enum class BlockKind(
    public val createDefault: () -> BlockDefinition,
    public val managerTypeProblem: PlacementProblem?,
    public val onlyIn: EventType? = null,
    internal val misplacedComment: String? = null,
    public val otherEventProblem: PlacementProblem? = null,
) {

    SET_VELOCITY({ BlockDefinition.SetVelocity(x = 200f, y = 0f) }, managerTypeProblem = PlacementProblem.BLOCK_MOVEMENT_IN_MANAGER_TYPE),
    ADD_TO_VELOCITY({ BlockDefinition.AddToVelocity(x = 50f, y = 0f) }, managerTypeProblem = PlacementProblem.BLOCK_MOVEMENT_IN_MANAGER_TYPE),
    STOP({ BlockDefinition.Stop }, managerTypeProblem = PlacementProblem.BLOCK_MOVEMENT_IN_MANAGER_TYPE),
    RUN_PARENT_EVENT({ BlockDefinition.RunParentEvent }, managerTypeProblem = PlacementProblem.BLOCK_RUN_PARENT_EVENT_IN_MANAGER_TYPE),
    JUMP_TO({ BlockDefinition.JumpTo() }, managerTypeProblem = PlacementProblem.BLOCK_MOVEMENT_IN_MANAGER_TYPE),
    BOUNCE_OFF_EDGE({ BlockDefinition.BounceOffEdge }, managerTypeProblem = PlacementProblem.BLOCK_MOVEMENT_IN_MANAGER_TYPE, onlyIn = EventType.EDGE_TOUCHED, misplacedComment = "// Bounce off the edge only works in the Edge touched Event.", otherEventProblem = PlacementProblem.BLOCK_BOUNCE_OFF_EDGE_OUTSIDE_EDGE_TOUCHED),
    REMOVE_SELF({ BlockDefinition.RemoveSelf }, managerTypeProblem = PlacementProblem.BLOCK_REMOVE_SELF_IN_MANAGER_TYPE),
    BOUNCE_OFF_OTHER({ BlockDefinition.BounceOffOther }, managerTypeProblem = PlacementProblem.BLOCK_MOVEMENT_IN_MANAGER_TYPE, onlyIn = EventType.COLLISION, misplacedComment = "// Bounce off it only works in a collision Event.", otherEventProblem = PlacementProblem.BLOCK_BOUNCE_OFF_OTHER_OUTSIDE_COLLISION),
    REMOVE_OTHER({ BlockDefinition.RemoveOther }, managerTypeProblem = PlacementProblem.BLOCK_REMOVE_OTHER_IN_MANAGER_TYPE, onlyIn = EventType.COLLISION, misplacedComment = "// Remove the other Actor only works in a collision Event.", otherEventProblem = PlacementProblem.BLOCK_REMOVE_OTHER_OUTSIDE_COLLISION),
    SET_NUMBER({ BlockDefinition.SetNumber() }, managerTypeProblem = null),
    CHANGE_NUMBER({ BlockDefinition.ChangeNumber() }, managerTypeProblem = null),
    SET_TEXT({ BlockDefinition.SetText() }, managerTypeProblem = null),
    SET_TOGGLE({ BlockDefinition.SetToggle() }, managerTypeProblem = null),
    ADD_ACTOR({ BlockDefinition.AddActor() }, managerTypeProblem = null),
    MOVE_TOWARDS_NEAREST({ BlockDefinition.MoveTowardsNearest() }, managerTypeProblem = PlacementProblem.BLOCK_MOVEMENT_IN_MANAGER_TYPE),
    START_TIMER({ BlockDefinition.StartTimer() }, managerTypeProblem = null),
    STOP_TIMER({ BlockDefinition.StopTimer() }, managerTypeProblem = null),
    GO_TO_SCENE({ BlockDefinition.GoToScene() }, managerTypeProblem = null),
    RESTART_SCENE({ BlockDefinition.RestartScene }, managerTypeProblem = null),
    PAUSE_GAME({ BlockDefinition.PauseGame() }, managerTypeProblem = null),
    PLAY_SOUND({ BlockDefinition.PlaySound() }, managerTypeProblem = null),
    PLAY_MUSIC({ BlockDefinition.PlayMusic() }, managerTypeProblem = null),
    STOP_MUSIC({ BlockDefinition.StopMusic }, managerTypeProblem = null),
    IF({ BlockDefinition.If() }, managerTypeProblem = null),
    ELSE({ BlockDefinition.Else }, managerTypeProblem = null),
    REPEAT({ BlockDefinition.Repeat() }, managerTypeProblem = null),
    END({ BlockDefinition.End }, managerTypeProblem = null),
    LOG({ BlockDefinition.Log() }, managerTypeProblem = null),
    KOTLIN({ BlockDefinition.Kotlin() }, managerTypeProblem = null);

    /**
     * Whether the Block needs the Actor, which a Manager type doesn't have.
     */
    public val needsActor: Boolean get() = managerTypeProblem != null

    init {
        require((onlyIn == null) == (misplacedComment == null) && (onlyIn == null) == (otherEventProblem == null)) {
            "$name needs onlyIn, misplacedComment, and otherEventProblem, or none of them."
        }
    }

    /**
     * Whether the palette offers this Block in an Event of [eventType] of a type of [kind]. Some Blocks need what only
     * one Event knows: which edge was touched, or which Actor was collided with. Those that move or remove the Actor
     * need one, so a Manager doesn't have them.
     */
    public fun isAvailableIn(eventType: EventType, kind: BlockTypeKind): Boolean = !(needsActor && kind != BlockTypeKind.ACTOR) && (onlyIn == null || onlyIn == eventType)

    public companion object {

        public fun of(block: BlockDefinition): BlockKind = when (block) {
            is BlockDefinition.SetVelocity -> SET_VELOCITY
            is BlockDefinition.AddToVelocity -> ADD_TO_VELOCITY
            BlockDefinition.Stop -> STOP
            BlockDefinition.RunParentEvent -> RUN_PARENT_EVENT
            is BlockDefinition.JumpTo -> JUMP_TO
            BlockDefinition.BounceOffEdge -> BOUNCE_OFF_EDGE
            BlockDefinition.RemoveSelf -> REMOVE_SELF
            BlockDefinition.BounceOffOther -> BOUNCE_OFF_OTHER
            BlockDefinition.RemoveOther -> REMOVE_OTHER
            is BlockDefinition.SetNumber -> SET_NUMBER
            is BlockDefinition.ChangeNumber -> CHANGE_NUMBER
            is BlockDefinition.SetText -> SET_TEXT
            is BlockDefinition.SetToggle -> SET_TOGGLE
            is BlockDefinition.AddActor -> ADD_ACTOR
            is BlockDefinition.MoveTowardsNearest -> MOVE_TOWARDS_NEAREST
            is BlockDefinition.StartTimer -> START_TIMER
            is BlockDefinition.StopTimer -> STOP_TIMER
            is BlockDefinition.GoToScene -> GO_TO_SCENE
            BlockDefinition.RestartScene -> RESTART_SCENE
            is BlockDefinition.PauseGame -> PAUSE_GAME
            is BlockDefinition.PlaySound -> PLAY_SOUND
            is BlockDefinition.PlayMusic -> PLAY_MUSIC
            BlockDefinition.StopMusic -> STOP_MUSIC
            is BlockDefinition.If -> IF
            BlockDefinition.Else -> ELSE
            is BlockDefinition.Repeat -> REPEAT
            BlockDefinition.End -> END
            is BlockDefinition.Log -> LOG
            is BlockDefinition.Kotlin -> KOTLIN
        }
    }
}
