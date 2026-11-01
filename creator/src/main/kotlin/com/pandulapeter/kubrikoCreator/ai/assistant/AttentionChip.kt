/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ai.assistant

import com.agentclientprotocol.model.ContentBlock
import com.pandulapeter.kubrikoCreator.workspace.ConsoleEntry
import java.util.Base64

/**
 * What the user is looking at, offered to the agent with their next message: as links the agent follows with Creator's
 * read tools when it needs them, rather than pasted content, except for a console problem, which is short, and the running
 * game's screen, sent as an image to agents that take images.
 */
internal sealed interface AttentionChip {

    /**
     * Tells chips apart, so that one the user turned off stays off while it is the same.
     */
    val key: String

    fun toContent(): ContentBlock

    data class Scene(
        val name: String,
    ) : AttentionChip {
        override val key get() = "scene:$name"

        override fun toContent() = ContentBlock.ResourceLink(
            name = "Scene $name",
            uri = "$URI_SCHEME://scenes/$name",
            description = "The Scene open in the editor. read_scene reads it.",
        )
    }

    data class BlockType(
        val name: String,
        val isManager: Boolean,
    ) : AttentionChip {
        override val key get() = "type:$name"

        override fun toContent() = ContentBlock.ResourceLink(
            name = (if (isManager) "Manager type " else "Actor type ") + name,
            uri = "$URI_SCHEME://types/$name",
            description = "The type selected in the editor. " + (if (isManager) "read_manager_type" else "read_actor_type") + " reads it.",
        )
    }

    /**
     * What the running game shows, captured when the message is sent ([contentOf]).
     */
    data object GameScreen : AttentionChip {
        override val key get() = "game"

        override fun toContent() = ContentBlock.Text("A screenshot of the running game follows.")
    }

    data class Problem(
        val message: String,
        val location: String?,
    ) : AttentionChip {
        override val key get() = "problem:$location:$message"

        override fun toContent() = ContentBlock.Text("The latest problem in the console: " + listOfNotNull(location, message).joinToString(": "))
    }

    companion object {
        private const val URI_SCHEME = "kubriko-creator"

        /**
         * The chips for the editor's state: the open Scene, otherwise the selected type, and the console's latest problem,
         * unless something went well after it.
         */
        fun of(
            openScene: String?,
            selectedType: String?,
            isSelectedTypeManager: Boolean,
            console: List<ConsoleEntry>,
            canSendGameScreen: Boolean = false,
        ): List<AttentionChip> = listOfNotNull(
            openScene?.let(::Scene),
            selectedType?.takeIf { openScene == null }?.let { BlockType(it, isSelectedTypeManager) },
            latestProblemOf(console),
            GameScreen.takeIf { canSendGameScreen },
        )

        /**
         * What [chips] send: each one's content, and for [GameScreen] the screenshot [captureScreen] takes, left out if it
         * takes none.
         */
        suspend fun contentOf(chips: List<AttentionChip>, captureScreen: suspend () -> ByteArray?): List<ContentBlock> = chips.flatMap { chip ->
            if (chip == GameScreen) {
                captureScreen()?.let { png -> listOf(chip.toContent(), ContentBlock.Image(data = Base64.getEncoder().encodeToString(png), mimeType = "image/png")) }.orEmpty()
            } else {
                listOf(chip.toContent())
            }
        }

        private fun latestProblemOf(console: List<ConsoleEntry>): Problem? {
            for (index in console.indices.reversed()) {
                when (val entry = console[index]) {
                    is ConsoleEntry.Problem -> return Problem(entry.message, entry.location)
                    is ConsoleEntry.SessionFailure -> return Problem(entry.message, null)
                    is ConsoleEntry.Status -> if (!entry.isError) return null
                    is ConsoleEntry.BuildOutput, is ConsoleEntry.Log -> Unit
                }
            }
            return null
        }
    }
}
