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
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.console.ConsoleEntry
import java.util.Base64

/**
 * What the user is looking at, offered to the agent with their next message: as links the agent follows with Creator's
 * read tools when it needs them, rather than pasted content, except for a console problem, which is short, and pictures
 * ([Screenshot]), sent as images to agents that take images.
 */
public sealed interface AttentionChip {

    /**
     * Tells chips apart, so that one the user turned on or off stays so while it is the same.
     */
    public val key: String

    /**
     * Whether the chip is sent unless the user leaves it out; otherwise it is sent only once the user turns it on.
     */
    public val isOnByDefault: Boolean get() = true

    /**
     * Whether the chip is sent, given the [toggled] keys: those the user turned from how they are by default.
     */
    public fun isSentWith(toggled: Set<String>): Boolean = isOnByDefault != (key in toggled)

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
     * The Event shown in the type editor, by its index among the type's Events, as commands address Events.
     */
    public data class Event(
        val typeName: String,
        val isManager: Boolean,
        val index: Int,
        val type: EventType,
    ) : AttentionChip {
        override val key: String get() = "event:$typeName:$index:$type"

        override fun toContent(): ContentBlock = ContentBlock.ResourceLink(
            name = "Event $index (${EventType.serializer().descriptor.getElementName(type.ordinal)}) of " + (if (isManager) "Manager type " else "Actor type ") + typeName,
            uri = "$URI_SCHEME://types/$typeName/events/$index",
            description = "The Event selected in the editor, by its index among the type's Events. " +
                (if (isManager) "read_manager_type" else "read_actor_type") + " reads it.",
        )
    }

    /**
     * A picture, captured when the message is sent ([contentOf]).
     */
    public sealed interface Screenshot : AttentionChip

    /**
     * What the running game shows.
     */
    public data object GameScreen : Screenshot {
        override val key: String get() = "game"

        override fun toContent(): ContentBlock = ContentBlock.Text("A screenshot of the running game follows.")
    }

    /**
     * The open Scene as the Scene editor draws it, sent only once the user turns it on, since a picture with every message
     * costs far more than the Scene's link.
     */
    public data object SceneScreen : Screenshot {
        override val key: String get() = "scene-screen"

        override val isOnByDefault: Boolean get() = false

        override fun toContent(): ContentBlock = ContentBlock.Text("A picture of the open Scene as the Scene editor shows it follows.")
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
         * The chips for the editor's state: the open Scene (and its picture), otherwise the selected type and the Event shown
         * of it ([shownEvent], left out when it is another type's), and the console's latest problem, unless something went
         * well after it ([latestProblem], as `ConsoleLines.latestProblem` keeps it).
         */
        public fun of(
            openScene: String?,
            selectedType: String?,
            isSelectedTypeManager: Boolean,
            latestProblem: ConsoleEntry.Failing?,
            canSendGameScreen: Boolean = false,
            shownEvent: Event? = null,
            canSendSceneScreen: Boolean = false,
        ): List<AttentionChip> = listOfNotNull(
            openScene?.let(::Scene),
            SceneScreen.takeIf { openScene != null && canSendSceneScreen },
            selectedType?.takeIf { openScene == null }?.let { BlockType(it, isSelectedTypeManager) },
            shownEvent?.takeIf { openScene == null && it.typeName == selectedType }?.copy(isManager = isSelectedTypeManager),
            latestProblem?.let { Problem(it.message, it.location) },
            GameScreen.takeIf { canSendGameScreen },
        )

        /**
         * What [chips] send: each one's content, and for a [Screenshot] the PNG [capture] takes, left out if it takes none.
         */
        public suspend fun contentOf(chips: List<AttentionChip>, capture: suspend (Screenshot) -> ByteArray?): List<ContentBlock> = chips.flatMap { chip ->
            if (chip is Screenshot) {
                capture(chip)?.let { png -> listOf(chip.toContent(), ContentBlock.Image(data = Base64.getEncoder().encodeToString(png), mimeType = "image/png")) }.orEmpty()
            } else {
                listOf(chip.toContent())
            }
        }
    }
}
