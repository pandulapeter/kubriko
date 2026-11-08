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

/**
 * What a new iOS simulator can be made of on this Mac, as Xcode's `simctl` lists it.
 */
public sealed interface IosSimulatorOptions {

    /**
     * @param deviceTypes The iPhones, then the iPads, each newest first, that at least one of [runtimes] runs.
     * @param runtimes The installed iOS versions simulators run, newest first.
     * @param takenNames The names of the iOS simulators already on this Mac.
     */
    public data class Ready(
        public val deviceTypes: List<IosDeviceType>,
        public val runtimes: List<IosRuntime>,
        public val takenNames: Set<String>,
    ) : IosSimulatorOptions {

        /**
         * What a new simulator starts as: the newest device type the newest iOS version runs, rather than one made for a
         * single device whose iOS version runs nothing else.
         */
        public val suggestedDeviceType: IosDeviceType? get() = runtimes.firstNotNullOfOrNull { runtime -> deviceTypes.firstOrNull(runtime::runs) }

        /**
         * The iOS versions [deviceType] runs, newest first.
         */
        public fun runtimesFor(deviceType: IosDeviceType): List<IosRuntime> = runtimes.filter { it.runs(deviceType) }

        /**
         * Whether an iOS simulator is already called [name], whatever the case of its letters, since Play's menu would
         * then show two alike.
         */
        public fun isNameTaken(name: String): Boolean = name.trim().let { trimmed -> takenNames.any { it.equals(trimmed, ignoreCase = true) } }

        /**
         * [deviceType]'s name, followed by the first number from 2 that makes it a name no simulator has yet, if it is taken.
         */
        public fun suggestedName(deviceType: IosDeviceType): String = (sequenceOf(deviceType.name) + generateSequence(2) { it + 1 }.map { "${deviceType.name} $it" })
            .first { !isNameTaken(it) }
    }

    /**
     * No iOS version that simulators run is installed, which Xcode downloads in its Settings, under Components.
     */
    public data object NoRuntime : IosSimulatorOptions

    /**
     * `simctl` couldn't list them.
     *
     * @param reason What it printed about why, if anything.
     */
    public data class Unreadable(
        public val reason: String?,
    ) : IosSimulatorOptions
}

/**
 * A kind of iPhone or iPad a simulator can be.
 *
 * @param identifier What `simctl create` takes (`com.apple.CoreSimulator.SimDeviceType.iPhone-16-Pro`).
 * @param name As people name it (`iPhone 16 Pro`).
 */
public data class IosDeviceType(
    public val identifier: String,
    public val name: String,
)

/**
 * An installed iOS version simulators run.
 *
 * @param identifier What `simctl create` takes (`com.apple.CoreSimulator.SimRuntime.iOS-18-2`).
 * @param name As people name it (`iOS 18.2`).
 * @param build Apple's build of it, which tells apart two of the same version, such as one made for a single device.
 * @param deviceTypes The identifiers of the device types it runs.
 */
public data class IosRuntime(
    public val identifier: String,
    public val name: String,
    public val build: String,
    public val deviceTypes: Set<String>,
) {
    /**
     * Whether it runs [deviceType].
     */
    public fun runs(deviceType: IosDeviceType): Boolean = deviceType.identifier in deviceTypes

    /**
     * How a list of [runtimes] it is in names it: by its [name], followed by its [build] when another of them has the same.
     */
    public fun labelAmong(runtimes: List<IosRuntime>): String = if (runtimes.any { it != this && it.name == name }) "$name ($build)" else name
}
