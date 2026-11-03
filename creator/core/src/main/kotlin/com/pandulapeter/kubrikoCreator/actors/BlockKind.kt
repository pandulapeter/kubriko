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
 * @param needsActor Whether the Block moves or removes the Actor, which a Manager type doesn't have.
 * @param onlyIn The only Event the Block works in, if it needs what only that Event knows: which edge was touched, or
 *   which Actor was collided with.
 * @param misplacedComment The comment the Block's code becomes in any other Event; set exactly when [onlyIn] is.
 */
public enum class BlockKind(
    public val createDefault: () -> BlockDefinition,
    public val needsActor: Boolean,
    internal val onlyIn: EventType? = null,
    internal val misplacedComment: String? = null,
) {
    SET_VELOCITY({ BlockDefinition.SetVelocity(x = 200f, y = 0f) }, needsActor = true),
    ADD_TO_VELOCITY({ BlockDefinition.AddToVelocity(x = 50f, y = 0f) }, needsActor = true),
    STOP({ BlockDefinition.Stop }, needsActor = true),
    RUN_PARENT_EVENT({ BlockDefinition.RunParentEvent }, needsActor = true),
    JUMP_TO({ BlockDefinition.JumpTo() }, needsActor = true),
    BOUNCE_OFF_EDGE({ BlockDefinition.BounceOffEdge }, needsActor = true, onlyIn = EventType.EDGE_TOUCHED, misplacedComment = "// Bounce off the edge only works in the Edge touched Event."),
    REMOVE_SELF({ BlockDefinition.RemoveSelf }, needsActor = true),
    BOUNCE_OFF_OTHER({ BlockDefinition.BounceOffOther }, needsActor = true, onlyIn = EventType.COLLISION, misplacedComment = "// Bounce off it only works in a collision Event."),
    REMOVE_OTHER({ BlockDefinition.RemoveOther }, needsActor = true, onlyIn = EventType.COLLISION, misplacedComment = "// Remove the other Actor only works in a collision Event."),
    SET_NUMBER({ BlockDefinition.SetNumber() }, needsActor = false),
    CHANGE_NUMBER({ BlockDefinition.ChangeNumber() }, needsActor = false),
    SET_TEXT({ BlockDefinition.SetText() }, needsActor = false),
    SET_TOGGLE({ BlockDefinition.SetToggle() }, needsActor = false),
    ADD_ACTOR({ BlockDefinition.AddActor() }, needsActor = false),
    MOVE_TOWARDS_NEAREST({ BlockDefinition.MoveTowardsNearest() }, needsActor = true),
    START_TIMER({ BlockDefinition.StartTimer() }, needsActor = false),
    STOP_TIMER({ BlockDefinition.StopTimer() }, needsActor = false),
    GO_TO_SCENE({ BlockDefinition.GoToScene() }, needsActor = false),
    RESTART_SCENE({ BlockDefinition.RestartScene }, needsActor = false),
    PAUSE_GAME({ BlockDefinition.PauseGame() }, needsActor = false),
    PLAY_SOUND({ BlockDefinition.PlaySound() }, needsActor = false),
    PLAY_MUSIC({ BlockDefinition.PlayMusic() }, needsActor = false),
    STOP_MUSIC({ BlockDefinition.StopMusic }, needsActor = false),
    IF({ BlockDefinition.If() }, needsActor = false),
    ELSE({ BlockDefinition.Else }, needsActor = false),
    REPEAT({ BlockDefinition.Repeat() }, needsActor = false),
    END({ BlockDefinition.End }, needsActor = false),
    LOG({ BlockDefinition.Log() }, needsActor = false),
    KOTLIN({ BlockDefinition.Kotlin() }, needsActor = false);

    init {
        require((onlyIn == null) == (misplacedComment == null)) { "$name needs both onlyIn and misplacedComment, or neither." }
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
