/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.scenes

import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.NotATool
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.scenes.ActorStateEdits
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonElement

/**
 * Gives one value of an Actor's saved state, besides its body, a new value: how the Scene editor changes what an Actor of a
 * type written by hand keeps, merging what is typed into one field into one undo step, which `set_actor_state` can't, as
 * it takes the whole state. Kubriko Creator's own building block; not a tool, since `set_actor_state` gives the AI the same
 * reach.
 */
internal object SetActorStateValue : WriteCommand<SetActorStateValue.Input> {
    override val name = "set_actor_state_value"
    override val description = ""
    override val inputSerializer: KSerializer<Input> = NotATool("SetActorStateValue")

    override fun target(context: CommandContext, input: Input): CommandTarget? = CommandTarget.Scene(input.scene, listOf(input.actorId))

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateSceneActors(input.scene, listOf(input.actorId))
        ?: if (input.key == BODY || SceneDocument.stateObject(context.readScene(input.scene).requireActor(input.actorId).state) == null) Rejection(CommandProblem.SCENE_CONTENT_INVALID) else null

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val state = context.readScene(input.scene).requireActor(input.actorId).state
        val previous = context.replaceStates(input.scene, listOf(SetActorsState.ActorState(input.actorId, checkNotNull(ActorStateEdits.withValue(state, input.key, input.value))))).single()
        return Applied(inverse = Invocation(SetActorState, SetActorState.Input(input.scene, input.actorId, previous.state)))
    }

    override fun mergeKey(input: Input): Any = listOf(input.scene, input.actorId, input.key)

    data class Input(
        val scene: String,
        val actorId: String,
        val key: String,
        val value: JsonElement,
    )

    private const val BODY = "body"
}

/**
 * What gives the value [key] of the Actor [actorId] of the Scene [scene] the value [value], as the Scene editor's fields
 * for an Actor type written by hand do.
 */
public fun setActorStateValue(scene: String, actorId: String, key: String, value: JsonElement): Invocation<*> =
    Invocation(SetActorStateValue, SetActorStateValue.Input(scene, actorId, key, value))
