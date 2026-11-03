/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.workspace

import com.pandulapeter.kubrikoCreator.commands.PlayTarget
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/**
 * The iOS simulators of this Mac, found through Xcode's `simctl`. Each call runs it, so it is made off the main thread.
 */
internal class IosSimulators(
    private val tools: ToolProcesses,
) {
    /**
     * A simulator `simctl` lists.
     *
     * @param runtime The iOS version it runs, as people name it (`iOS 18.2`).
     */
    data class Simulator(
        val udid: String,
        val name: String,
        val runtime: String,
        val isBooted: Boolean,
    ) {
        /**
         * Its runtime's version, for putting the newest first.
         */
        val version: List<Int> get() = runtime.removePrefix("iOS ").split('.').mapNotNull(String::toIntOrNull)
    }

    /**
     * Every simulator the game can be played on: those that are booted first, then the newest iOS versions first.
     */
    suspend fun targets(): List<PlayTarget> = available().orEmpty()
        .sortedWith(compareByDescending<Simulator> { it.isBooted }.thenComparator { first, second -> compareVersions(second.version, first.version) }.thenBy { it.name })
        .map { PlayTarget(id = PREFIX + it.udid, kind = PlayTarget.Kind.IOS, name = "${it.name} (${it.runtime})", isVirtual = true) }

    /**
     * The simulators `simctl` lists now, or null if it can't list them.
     */
    suspend fun available(): List<Simulator>? {
        val result = tools.run(listOf("xcrun", "simctl", "list", "devices", "available", "--json"), TIMEOUT_IN_MILLISECONDS)
        return if (result.isSuccessful) parse(result.output) else null
    }

    companion object {
        const val PREFIX = "ios-simulator:"
        private const val TIMEOUT_IN_MILLISECONDS = 30_000L
        private val runtimePattern = Regex("""SimRuntime\.iOS-(\d+)-(\d+)""")

        /**
         * The iOS simulators of `simctl list devices available --json`, leaving out those of the watch, the TV, and the
         * headset, and any output it can't read.
         */
        fun parse(json: String): List<Simulator> {
            val devices = runCatching { (Json.parseToJsonElement(json) as? JsonObject)?.get("devices") as? JsonObject }.getOrNull() ?: return emptyList()
            return devices.flatMap { (runtimeKey, list) ->
                val runtime = runtimePattern.find(runtimeKey)?.let { "iOS ${it.groupValues[1]}.${it.groupValues[2]}" } ?: return@flatMap emptyList()
                (list as? JsonArray).orEmpty().mapNotNull { element ->
                    val device = element as? JsonObject ?: return@mapNotNull null
                    fun text(key: String) = (device[key] as? JsonPrimitive)?.content
                    if ((device["isAvailable"] as? JsonPrimitive)?.booleanOrNull == false) return@mapNotNull null
                    Simulator(
                        udid = text("udid") ?: return@mapNotNull null,
                        name = text("name") ?: return@mapNotNull null,
                        runtime = runtime,
                        isBooted = text("state") == "Booted",
                    )
                }
            }
        }

        private fun compareVersions(first: List<Int>, second: List<Int>): Int {
            first.zip(second).forEach { (a, b) -> if (a != b) return a.compareTo(b) }
            return first.size.compareTo(second.size)
        }
    }
}
