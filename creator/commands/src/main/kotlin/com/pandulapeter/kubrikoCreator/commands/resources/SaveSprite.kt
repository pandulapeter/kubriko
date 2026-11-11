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

import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.NotATool
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.commands.actorTypes.validateSprite
import com.pandulapeter.kubrikoCreator.sprites.pixels.SpriteFrames
import kotlinx.serialization.KSerializer

/**
 * Saves what the sprite editor drew on a sprite since it was last saved, as one step: its frames, and its sheet when
 * their size changed. Not a tool, since the AI draws through [DrawPixels] and [EditSprite], which save as they go; the
 * sprite editor keeps its changes until the user saves them, as an image editor does.
 */
public object SaveSprite : WriteCommand<SaveSprite.Input> {
    override val name: String = "save_sprite"
    override val description: String = ""
    override val inputSerializer: KSerializer<Input> = NotATool("SaveSprite")

    override fun target(context: CommandContext, input: Input): CommandTarget = CommandTarget.Project

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateSprite(input.sprite)

    override suspend fun apply(context: CommandContext, input: Input): Applied = context.editSpriteImage(input.sprite) { SpriteEdit.Edited(input.frames) }

    /**
     * @param sprite The sprite's path from `composeResources`.
     * @param frames Its frames as the editor leaves them, with their sheet.
     */
    public class Input(
        public val sprite: String,
        public val frames: SpriteFrames,
    )
}
