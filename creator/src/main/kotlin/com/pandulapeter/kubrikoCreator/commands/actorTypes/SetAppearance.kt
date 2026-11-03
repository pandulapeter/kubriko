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

import com.pandulapeter.kubrikoCreator.actors.ActorShape
import com.pandulapeter.kubrikoCreator.actors.HexColorSerializer
import com.pandulapeter.kubrikoCreator.actors.SpriteFiles
import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Descriptions
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(EditorMutation::class)
internal object SetAppearance : WriteCommand<SetAppearance.Input> {
    override val name = "set_appearance"
    override val description = "Changes how the Actors of a block-built Actor type look. Properties left out stay as they are. " +
        "Choosing a sprite also sets the size to the image's, unless a width or height is given."
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.BlockType(input.actorType)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateActorType(input.actorType) ?: run {
        val definition = context.requireActorType(input.actorType)
        input.sprite?.ifEmpty { null }?.let { context.validateSprite(it) } ?: validateSize(input.width ?: definition.width, input.height ?: definition.height)
    }

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val definition = context.requireActorType(input.actorType)
        val sprite = input.sprite?.ifEmpty { null }
        val spriteSize = if (sprite != null && sprite != definition.sprite && input.width == null && input.height == null) {
            context.spriteFiles()?.files?.get(sprite)?.let { file -> withContext(context.ioDispatcher) { SpriteFiles.sizeOf(file) } }
        } else {
            null
        }
        val width = input.width ?: spriteSize?.first?.toFloat()
        val height = input.height ?: spriteSize?.second?.toFloat()
        context.requireBlockTypes.put(
            definition.copy(
                shape = input.shape ?: definition.shape,
                sprite = if (input.sprite == null) definition.sprite else sprite,
                color = input.color ?: definition.color,
                width = width ?: definition.width,
                height = height ?: definition.height,
            ),
        )
        return Applied(
            inverse = Invocation(
                command = SetAppearance,
                input = Input(
                    actorType = input.actorType,
                    shape = input.shape?.let { definition.shape },
                    sprite = input.sprite?.let { definition.sprite.orEmpty() },
                    color = input.color?.let { definition.color },
                    // Explicit, so undoing a sprite change never sizes the Actor type to the image.
                    width = if (width != null || input.sprite != null) definition.width else null,
                    height = if (height != null || input.sprite != null) definition.height else null,
                ),
            ),
        )
    }

    override fun mergeKey(input: Input) = listOf(input.actorType, input.shape != null, input.sprite != null, input.color != null, input.width != null, input.height != null)

    @Serializable
    data class Input(
        @Description(Descriptions.ACTOR_TYPE_NAME)
        @SerialName("actorType") val actorType: String,
        @Description("The shape its Actors are drawn as, and of their collision mask.")
        @SerialName("shape") val shape: ActorShape? = null,
        @Description("The image its Actors are drawn as instead of the shape, by its path from composeResources (drawable/ball.png), or an empty text to draw the shape again.")
        @SerialName("sprite") val sprite: String? = null,
        @Description("The color its Actors are drawn with, as #RRGGBB, or #AARRGGBB when it isn't opaque.")
        @SerialName("color") @Serializable(with = HexColorSerializer::class) val color: Long? = null,
        @Description("The width of a newly placed Actor, in scene units.")
        @SerialName("width") val width: Float? = null,
        @Description("The height of a newly placed Actor, in scene units.")
        @SerialName("height") val height: Float? = null,
    )
}
