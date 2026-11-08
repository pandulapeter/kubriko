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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.longOrNull

/**
 * Makes new iOS simulators through Xcode's `simctl`, so a beginner never leaves Kubriko Creator for one.
 *
 * @param onCreated Makes a new simulator the place Play uses, without waiting for Play's places to be looked for again.
 */
public class IosSimulatorSetup internal constructor(
    private val tools: ToolProcesses,
    private val ioDispatcher: CoroutineDispatcher,
    private val onCreated: (PlayTarget) -> Unit,
) {
    /**
     * What a new simulator can be made of now, listed off the main thread.
     */
    public suspend fun options(): IosSimulatorOptions = withContext(ioDispatcher) {
        val (deviceTypes, runtimes, simulators) = listOf(LIST_DEVICE_TYPES, LIST_RUNTIMES, IosSimulators.LIST_AVAILABLE).map { command ->
            val result = tools.run(command, LIST_TIMEOUT_IN_MILLISECONDS)
            if (!result.isSuccessful) return@withContext IosSimulatorOptions.Unreadable(failureReasonOf(result.output, result.exitCode))
            result.output
        }
        optionsOf(deviceTypes, runtimes, IosSimulators.parse(simulators).mapTo(mutableSetOf()) { it.name })
    }

    /**
     * Makes a simulator called [name] (trimmed), a [deviceType] running [runtime], and has Play use it once it is made.
     */
    public suspend fun create(name: String, deviceType: IosDeviceType, runtime: IosRuntime): IosSimulatorCreation {
        val trimmed = name.trim()
        val result = tools.run(listOf("xcrun", "simctl", "create", trimmed, deviceType.identifier, runtime.identifier), CREATE_TIMEOUT_IN_MILLISECONDS)
        if (result.hasTimedOut) return IosSimulatorCreation.TimedOut
        // What it prints is its errors too, so a warning may come after the UDID.
        val udid = result.output.lines().map(String::trim).lastOrNull(udidPattern::matches)?.takeIf { result.isSuccessful }
            ?: return IosSimulatorCreation.Refused(failureReasonOf(result.output, result.exitCode))
        val target = PlayTarget(id = IosSimulators.PREFIX + udid, kind = PlayTarget.Kind.IOS, name = "$trimmed (${runtime.name})", isVirtual = true)
        onCreated(target)
        return IosSimulatorCreation.Created(target)
    }

    internal companion object {
        val LIST_DEVICE_TYPES = listOf("xcrun", "simctl", "list", "devicetypes", "--json")
        val LIST_RUNTIMES = listOf("xcrun", "simctl", "list", "runtimes", "--json")
        private const val LIST_TIMEOUT_IN_MILLISECONDS = 30_000L
        private const val CREATE_TIMEOUT_IN_MILLISECONDS = 60_000L
        private val FAMILIES = listOf("iPhone", "iPad")
        private val udidPattern = Regex("[0-9A-Fa-f]{8}(-[0-9A-Fa-f]{4}){3}-[0-9A-Fa-f]{12}")

        /**
         * The options of `simctl list devicetypes --json` and `simctl list runtimes --json`: the available iOS runtimes,
         * newest first (one made for a single device after the one for all of that version), and the iPhones, then the
         * iPads, that one of them runs, each newest first by the oldest iOS version it runs, in `simctl`'s order between
         * those of the same.
         */
        fun optionsOf(deviceTypesJson: String, runtimesJson: String, takenNames: Set<String>): IosSimulatorOptions {
            val deviceTypes = jsonArrayOf(deviceTypesJson, "devicetypes") ?: return IosSimulatorOptions.Unreadable(reason = null)
            val runtimes = jsonArrayOf(runtimesJson, "runtimes")?.mapNotNull(::iosRuntimeOf)
                ?.sortedWith { first, second -> IosSimulators.compareVersions(second.second, first.second).takeIf { it != 0 } ?: second.first.deviceTypes.size.compareTo(first.first.deviceTypes.size) }
                ?.map { it.first }
                ?: return IosSimulatorOptions.Unreadable(reason = null)
            val ofFamilies = FAMILIES.flatMap { family ->
                deviceTypes.mapNotNull { deviceTypeOf(it, family) }.filter { (type, _) -> runtimes.any { it.runs(type) } }.sortedByDescending { it.second }.map { it.first }
            }
            return if (ofFamilies.isEmpty()) IosSimulatorOptions.NoRuntime else IosSimulatorOptions.Ready(ofFamilies, runtimes.filter { runtime -> ofFamilies.any(runtime::runs) }, takenNames)
        }

        private fun jsonArrayOf(json: String, key: String) = runCatching { (Json.parseToJsonElement(json) as? JsonObject)?.get(key) as? JsonArray }.getOrNull()

        private fun JsonElement.text(key: String) = ((this as? JsonObject)?.get(key) as? JsonPrimitive)?.takeIf { it.isString }?.content

        /**
         * A device type of [family] with the oldest iOS version it runs, as `simctl` counts versions; the iPod touch calls
         * itself an iPhone, so it is told by its identifier.
         */
        private fun deviceTypeOf(element: JsonElement, family: String): Pair<IosDeviceType, Long>? {
            val identifier = element.text("identifier")?.takeUnless { "iPod" in it } ?: return null
            if (element.text("productFamily") != family) return null
            val minimumRuntime = (((element as JsonObject)["minRuntimeVersion"]) as? JsonPrimitive)?.longOrNull ?: 0L
            return IosDeviceType(identifier, element.text("name") ?: return null) to minimumRuntime
        }

        private fun iosRuntimeOf(element: JsonElement): Pair<IosRuntime, List<Int>>? {
            val identifier = element.text("identifier") ?: return null
            val isIos = element.text("platform")?.let { it == "iOS" } ?: (".SimRuntime.iOS-" in identifier)
            val isAvailable = (((element as JsonObject)["isAvailable"]) as? JsonPrimitive)?.booleanOrNull == true
            if (!isIos || !isAvailable) return null
            val deviceTypes = (element["supportedDeviceTypes"] as? JsonArray).orEmpty().mapNotNullTo(mutableSetOf()) { it.text("identifier") }
            val version = element.text("version").orEmpty()
            val runtime = IosRuntime(identifier, element.text("name") ?: "iOS $version", element.text("buildversion").orEmpty(), deviceTypes)
            return runtime to version.split('.').mapNotNull(String::toIntOrNull)
        }
    }
}

/**
 * How making an iOS simulator ended.
 */
public sealed interface IosSimulatorCreation {

    /**
     * It was made, and is now the place Play uses.
     */
    public data class Created(
        public val target: PlayTarget,
    ) : IosSimulatorCreation

    /**
     * `simctl` refused, for [reason], in its own words.
     */
    public data class Refused(
        public val reason: String,
    ) : IosSimulatorCreation

    /**
     * `simctl` didn't finish in time, so it was stopped.
     */
    public data object TimedOut : IosSimulatorCreation
}
