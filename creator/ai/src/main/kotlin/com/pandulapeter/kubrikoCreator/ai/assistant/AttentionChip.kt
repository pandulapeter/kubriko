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
import com.pandulapeter.kubrikoCreator.console.ConsoleEntry
import java.util.Base64

/**
 * What the user is looking at, offered to the agent with their next message: as links the agent follows with Creator's
 * read tools when it needs them, rather than pasted content, except for a console problem, which is short, and the running
 * game's screen, sent as an image to agents that take images.
 */
public sealed interface AttentionChip {

    /**
     * Tells chips apart, so that one the user turned off stays off while it is the same.
     */
    public val key: String

    /**
     * What the chip adds to the prompt it is sent with.
     */
    public fun toContent(): ContentBlock

    /**
     * The Scene open in the editor.
     */
    public data class Scene(
        val name: String,
    ) : AttentionChip {
        override val key: String get() = "scene:$name"

        override fun toContent(): ContentBlock = ContentBlock.ResourceLink(
            name = "Scene $name",
            uri = "$URI_SCHEME://scenes/$name",
            description = "The Scene open in the editor. read_scene reads it.",
        )
    }

    /**
     * The Actor type or Manager type selected.
     */
    public data class BlockType(
        val name: String,
        val isManager: Boolean,
    ) : AttentionChip {
        override val key: String get() = "type:$name"

        override fun toContent(): ContentBlock = ContentBlock.ResourceLink(
            name = (if (isManager) "Manager type " else "Actor type ") + name,
            uri = "$URI_SCHEME://types/$name",
            description = "The type selected in the editor. " + (if (isManager) "read_manager_type" else "read_actor_type") + " reads it.",
        )
    }

    /**
     * What the running game shows, captured when the message is sent ([contentOf]).
     */
    public data object GameScreen : AttentionChip {
        override val key: String get() = "game"

        override fun toContent(): ContentBlock = ContentBlock.Text("A screenshot of the running game follows.")
    }

    /**
     * The latest problem in the console.
     */
    public data class Problem(
        val message: String,
        val location: String?,
    ) : AttentionChip {
        override val key: String get() = "problem:$location:$message"

        override fun toContent(): ContentBlock = ContentBlock.Text("The latest problem in the console: " + listOfNotNull(location, message).joinToString(": "))
    }

    public companion object {
        private const val URI_SCHEME = "kubriko-creator"

        /**
         * The chips for the editor's state: the open Scene, otherwise the selected type, and the console's latest problem,
         * unless something went well after it ([latestProblem], as `ConsoleLines.latestProblem` keeps it).
         */
        public fun of(
            openScene: String?,
            selectedType: String?,
            isSelectedTypeManager: Boolean,
            latestProblem: ConsoleEntry?,
            canSendGameScreen: Boolean = false,
        ): List<AttentionChip> = listOfNotNull(
            openScene?.let(::Scene),
            selectedType?.takeIf { openScene == null }?.let { BlockType(it, isSelectedTypeManager) },
            when (latestProblem) {
                is ConsoleEntry.Problem -> Problem(latestProblem.message, latestProblem.location)
                is ConsoleEntry.SessionFailure -> Problem(latestProblem.message, null)
                else -> null
            },
            GameScreen.takeIf { canSendGameScreen },
        )

        /**
         * What [chips] send: each one's content, and for [GameScreen] the screenshot [captureScreen] takes, left out if it
         * takes none.
         */
        public suspend fun contentOf(chips: List<AttentionChip>, captureScreen: suspend () -> ByteArray?): List<ContentBlock> = chips.flatMap { chip ->
            if (chip == GameScreen) {
                captureScreen()?.let { png -> listOf(chip.toContent(), ContentBlock.Image(data = Base64.getEncoder().encodeToString(png), mimeType = "image/png")) }.orEmpty()
            } else {
                listOf(chip.toContent())
            }
        }
    }
}
