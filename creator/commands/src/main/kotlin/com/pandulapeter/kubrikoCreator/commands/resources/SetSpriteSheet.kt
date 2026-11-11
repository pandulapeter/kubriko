/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.resources

import com.pandulapeter.kubrikoCreator.actors.ResourceKind
import com.pandulapeter.kubrikoCreator.actors.SpriteFiles
import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.LookupProblem
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.schema.Description
import com.pandulapeter.kubrikoCreator.sprites.SpriteSheet
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.IOException

@OptIn(EditorMutation::class)
public object SetSpriteSheet : WriteCommand<SetSpriteSheet.Input> {
    override val name: String = "set_sprite_sheet"
    override val description: String = "Cuts a sprite into the frames of an animation, which every Actor type drawn as it plays (Set the frame and " +
        "Set the animation Blocks control it), or shows it whole again. Only Kubriko Creator's note about the sprite changes, never the image."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget = CommandTarget.Project

    override fun validate(context: CommandContext, input: Input): Rejection? = when {
        ResourceKind.of(input.sprite) != ResourceKind.SPRITE -> Rejection(LookupProblem.RESOURCE_MISSING, input.sprite)
        else -> context.validateExistingResource(input.sprite) ?: input.sheet?.problem?.let(::Rejection)
    }

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val name = ResourceKind.nameOf(input.sprite)
        val previous = context.spriteFolders.state.value.sheets[name]
        val image = context.spriteFiles()?.files?.get(input.sprite)
        if (input.sheet != null && image != null) {
            val size = withContext(context.ioDispatcher) { SpriteFiles.sizeOf(image) }
            if (size != null && input.sheet.problemWithImage(size.first, size.second) != null) {
                return Applied.failed(Rejection(CommandProblem.SPRITE_SHEET_DOES_NOT_FIT, input.sprite, size.first.toString(), size.second.toString()))
            }
        }
        try {
            withContext(context.ioDispatcher) { context.spriteFolders.setSheet(name, input.sheet) }
        } catch (exception: IOException) {
            return Applied.failed(Rejection(CommandProblem.SPRITE_SHEET_NOT_WRITTEN, input.sprite, exception.message ?: exception.toString()))
        }
        return Applied(inverse = Invocation(SetSpriteSheet, input.copy(sheet = previous)))
    }

    override fun mergeKey(input: Input): Any? = input.sprite

    @Serializable
    public data class Input(
        @Description("The sprite's path from composeResources, like drawable/hero-walk.png.")
        @SerialName("sprite") val sprite: String,
        @Description("How its image is cut into frames, or left out to show it whole.")
        @SerialName("sheet") val sheet: SpriteSheet? = null,
    )
}
