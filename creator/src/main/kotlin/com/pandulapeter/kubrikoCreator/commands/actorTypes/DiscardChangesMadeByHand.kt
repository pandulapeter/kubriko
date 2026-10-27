/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.actorTypes

import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.Description
import com.pandulapeter.kubrikoCreator.commands.EditorMutation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_discard_changes_made_by_hand
import kubriko.creator.generated.resources.error_command_not_changed_by_hand

@OptIn(EditorMutation::class)
internal object DiscardChangesMadeByHand : WriteCommand<DiscardChangesMadeByHand.Input> {
    override val name = "discard_changes_made_by_hand"
    override val description = "Throws away the changes made by hand (outside Kubriko Creator) to the generated Kotlin class of a block-built Actor type, " +
        "so that Kubriko Creator updates it again. Kubriko Creator stops updating a generated class that was changed by hand. This can't be undone."
    override val title = Res.string.command_discard_changes_made_by_hand
    override val inputSerializer = Input.serializer()
    override val isDestructive = true

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateActorType(input.actorType)
        ?: if (input.actorType !in context.requireActorTypes.changedByHand.value) Rejection(Res.string.error_command_not_changed_by_hand, input.actorType) else null

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        context.requireActorTypes.discardChangesMadeByHand(input.actorType)
        return Applied(inverse = null)
    }

    @Serializable
    data class Input(
        @Description("The name of the Actor type.")
        @SerialName("actorType") val actorType: String,
    )
}
