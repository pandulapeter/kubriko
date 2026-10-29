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

import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Description
import com.pandulapeter.kubrikoCreator.commands.ReadCommand
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.scenes.SceneActorEntry
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_read_scene
import kubriko.creator.generated.resources.error_command_number_invalid
import kubriko.creator.generated.resources.error_command_page_invalid

internal object ReadScene : ReadCommand<ReadScene.Input> {
    override val name = "read_scene"
    override val description = "Reads the Actors of a Scene, in order, a page at a time: the ID of each one (actorId, how Actors are addressed), " +
        "its index in the Scene's list, its type ID (the name of its Actor type), its position when it has one, and its whole saved state. " +
        "total counts the Actors that match, and nextOffset, when there are more, is the offset of the next page. " +
        "Narrow it to an Actor type or to a rectangle, or ask for a summary first in a large Scene: the number of Actors of each type and the bounds of their positions. " +
        "content, the raw Scene file, is there only when the page holds the whole Scene."
    override val inputSerializer = Input.serializer()
    override val title = Res.string.command_read_scene

    override fun target(context: CommandContext, input: Input) = CommandTarget.Scene(input.scene)

    override suspend fun read(context: CommandContext, input: Input): CommandOutcome = withContext(context.ioDispatcher) {
        val rejection = context.validateScene(input.scene) ?: when {
            input.offset < 0 || input.count < 1 -> Rejection(Res.string.error_command_page_invalid)
            listOfNotNull(input.left, input.top, input.right, input.bottom).any { !it.isFinite() } -> Rejection(Res.string.error_command_number_invalid)
            else -> null
        }
        rejection?.let { return@withContext CommandOutcome.Rejected(it) }
        val actors = context.readScene(input.scene)
        val isFiltered = input.actorType != null || input.left != null || input.top != null || input.right != null || input.bottom != null
        val matching = if (isFiltered) actors.withIndex().filter { (_, actor) -> input.matches(actor) } else actors.withIndex().toList()
        CommandOutcome.Success(if (input.summary) summaryOf(matching) else pageOf(actors, matching, input, isFiltered))
    }

    private fun Input.matches(actor: SceneActorEntry): Boolean {
        if (actorType != null && actor.typeId != actorType) return false
        if (left == null && top == null && right == null && bottom == null) return true
        val (x, y) = SceneDocument.positionOf(actor.state) ?: return false
        return (left == null || x >= left) && (top == null || y >= top) && (right == null || x <= right) && (bottom == null || y <= bottom)
    }

    private fun pageOf(actors: List<SceneActorEntry>, matching: List<IndexedValue<SceneActorEntry>>, input: Input, isFiltered: Boolean): JsonObject {
        val end = (input.offset.toLong() + input.count).coerceAtMost(matching.size.toLong()).toInt()
        val page = if (input.offset < end) matching.subList(input.offset, end) else emptyList()
        return buildJsonObject {
            put("total", matching.size)
            putJsonArray("actors") {
                page.forEach { (index, actor) ->
                    addJsonObject {
                        put("actorId", actor.id)
                        put("index", index)
                        put("typeId", actor.typeId)
                        SceneDocument.positionOf(actor.state)?.let { (x, y) ->
                            put("x", x)
                            put("y", y)
                        }
                        put("state", actor.state)
                    }
                }
            }
            if (end < matching.size) put("nextOffset", end)
            if (!isFiltered && page.size == actors.size) put("content", SceneDocument.format(actors))
        }
    }

    private fun summaryOf(matching: List<IndexedValue<SceneActorEntry>>): JsonObject {
        val counts = sortedMapOf<String, Int>()
        var left = Float.POSITIVE_INFINITY
        var top = Float.POSITIVE_INFINITY
        var right = Float.NEGATIVE_INFINITY
        var bottom = Float.NEGATIVE_INFINITY
        matching.forEach { (_, actor) ->
            counts[actor.typeId] = (counts[actor.typeId] ?: 0) + 1
            SceneDocument.positionOf(actor.state)?.let { (x, y) ->
                left = minOf(left, x)
                top = minOf(top, y)
                right = maxOf(right, x)
                bottom = maxOf(bottom, y)
            }
        }
        return buildJsonObject {
            put("total", matching.size)
            putJsonArray("actorTypes") {
                counts.forEach { (typeId, count) ->
                    addJsonObject {
                        put("typeId", typeId)
                        put("count", count)
                    }
                }
            }
            if (left <= right) {
                putJsonObject("bounds") {
                    put("left", left)
                    put("top", top)
                    put("right", right)
                    put("bottom", bottom)
                }
            }
        }
    }

    @Serializable
    data class Input(
        @Description("The name of the Scene: its path in the Scenes folder without .json, like level-2 or levels/1-1.")
        @SerialName("scene") val scene: String,
        @Description("Whether to return only a summary of the matching Actors: how many there are of each type, and the bounds of their positions.")
        @SerialName("summary") val summary: Boolean = false,
        @Description("Only the Actors of this type: the name of their Actor type (its children not included).")
        @SerialName("actorType") val actorType: String? = null,
        @Description("Only the Actors positioned at or right of this, in scene units.")
        @SerialName("left") val left: Float? = null,
        @Description("Only the Actors positioned at or below this, in scene units.")
        @SerialName("top") val top: Float? = null,
        @Description("Only the Actors positioned at or left of this, in scene units.")
        @SerialName("right") val right: Float? = null,
        @Description("Only the Actors positioned at or above this, in scene units.")
        @SerialName("bottom") val bottom: Float? = null,
        @Description("How many matching Actors to skip: the nextOffset of the previous page.")
        @SerialName("offset") val offset: Int = 0,
        @Description("At most how many Actors to return.")
        @SerialName("count") val count: Int = DEFAULT_PAGE_SIZE,
    )

    const val DEFAULT_PAGE_SIZE = 100
}
