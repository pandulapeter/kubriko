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
import com.pandulapeter.kubrikoCreator.template.GameTarget
import com.pandulapeter.kubrikoCreator.template.GameTargets

/**
 * Plays the game anywhere but the desktop, and finds where: the browser once the web target is on, Android's devices
 * and emulators once its target is on and its SDK is found, and this Mac's iOS simulators once that target is on and Xcode
 * is there.
 *
 * @param targets The game's targets, or null if they aren't the template's.
 * @param androidDevices The devices of the Android SDK the project builds with, or null if there is none. Blocks.
 * @param iosSimulators This Mac's simulators, or null if it can't build for iOS. Blocks.
 */
internal class TargetPlayers(
    private val targets: () -> GameTargets?,
    private val androidDevices: () -> AndroidDevices?,
    private val web: WebPlayer,
    private val android: AndroidPlayer,
    private val iosSimulators: () -> IosSimulators?,
    private val ios: IosSimulatorPlayer,
) {
    /**
     * Each finds the places of one kind the game can be played on now, as [PlayTargetChoice] takes them. They block.
     */
    val sources: List<suspend () -> FoundPlaces> = listOf(
        { if (isOn(GameTarget.WEB)) FoundPlaces(PlayTarget.Kind.WEB, listOf(PlayTarget.WEB)) else FoundPlaces.NONE },
        { androidDevices().takeIf { isOn(GameTarget.ANDROID) }?.let { FoundPlaces(PlayTarget.Kind.ANDROID, it.targets()) } ?: FoundPlaces.NONE },
        { iosSimulators().takeIf { isOn(GameTarget.IOS) }?.let { FoundPlaces(PlayTarget.Kind.IOS, it.targets()) } ?: FoundPlaces.NONE },
    )

    /**
     * Plays the game on [target], as [SessionController.playElsewhere] runs it.
     */
    suspend fun play(target: PlayTarget, firstScene: String?, onUp: () -> Unit): Boolean = when (target.kind) {
        PlayTarget.Kind.DESKTOP -> error("The desktop is played through a session.")
        PlayTarget.Kind.WEB -> web.play(firstScene, onUp)
        PlayTarget.Kind.ANDROID -> android.play(target, firstScene, onUp)
        PlayTarget.Kind.IOS -> ios.play(target, firstScene, onUp)
    }

    /**
     * Builds the game in the browser again, if it plays there.
     */
    fun onCodeChanged() = web.onCodeChanged()

    private fun isOn(target: GameTarget) = targets()?.read()?.any { it.target == target && it.isOn } == true
}
