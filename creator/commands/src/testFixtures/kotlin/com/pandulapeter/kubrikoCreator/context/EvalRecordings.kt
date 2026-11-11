/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.context

import com.pandulapeter.kubrikoCreator.foundation.writeTextAtomically
import com.pandulapeter.kubrikoCreator.testing.creatorDirectory
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

/**
 * A tool call as a real session made it: the tool's name and its whole input.
 */
public data class RecordedCall(
    val tool: String,
    val arguments: JsonObject,
)

/**
 * The calls a real agent made for each [EvalSet.Request], one JSON file per request, written by the live run and kept
 * in this module's test fixtures' resources, one call per line, so a new recording reads as a diff.
 */
public object EvalRecordings {
    private const val FOLDER = "eval-set"

    /**
     * Tools whose calls aren't kept: they only mean something inside the assistant's own conversation, which a replay
     * has none of.
     */
    private val conversationTools = setOf("set_plan", "ask_user")

    /**
     * The calls recorded for [request], or none if it was never recorded.
     */
    public fun of(request: EvalSet.Request): List<RecordedCall> = EvalRecordings::class.java.getResource("/$FOLDER/${request.id}.json")
        ?.readText()
        ?.let(::decode)
        .orEmpty()

    /**
     * Writes [calls] as [request]'s recording into the test fixtures' sources, keeping only those a replay can make.
     */
    public fun write(request: EvalSet.Request, calls: List<RecordedCall>) {
        File(creatorDirectory(), "commands/src/testFixtures/resources/$FOLDER/${request.id}.json").apply { parentFile.mkdirs() }.writeTextAtomically(encode(calls.filter { it.tool !in conversationTools }))
    }

    public fun encode(calls: List<RecordedCall>): String = calls.joinToString(",\n", prefix = "[\n", postfix = "\n]\n") { call ->
        "  " + buildJsonObject {
            put("tool", JsonPrimitive(call.tool))
            put("arguments", call.arguments)
        }.toString()
    }

    public fun decode(text: String): List<RecordedCall> = Json.parseToJsonElement(text).jsonArray.map { element ->
        val call = element.jsonObject
        RecordedCall(call.getValue("tool").jsonPrimitive.content, call.getValue("arguments").jsonObject)
    }
}
