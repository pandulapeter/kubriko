/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.creatorSession.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A command Kubriko Creator sends to a session process in [SessionMode.PLAY], one JSON object per line (see
 * [SessionProtocol]), so that the game can be played from Kubriko Creator, by its AI tools for example. The session
 * answers each one with a [SessionMessage.CommandDone], a [SessionMessage.Screenshot], or a [SessionMessage.CommandFailed]
 * carrying the same [id].
 *
 * This is the wire format between the two processes, not an API for game code. Added in protocol version 2.
 */
@Serializable
sealed interface SessionCommand {

    /**
     * Identifies the command in its answer.
     */
    val id: Int

    /**
     * Brings the game's window to the front and focuses it. Kubriko pauses games whose window isn't focused.
     */
    @Serializable
    @SerialName("bringToFront")
    data class BringToFront(
        override val id: Int,
    ) : SessionCommand

    /**
     * Holds [keys] down together for [durationInMilliseconds], then releases them.
     *
     * @param keys The names of the keys as in `java.awt.event.KeyEvent`, without the `VK_` prefix (`LEFT`, `SPACE`, `A`).
     */
    @Serializable
    @SerialName("pressKeys")
    data class PressKeys(
        override val id: Int,
        val keys: List<String>,
        val durationInMilliseconds: Long,
    ) : SessionCommand

    /**
     * Captures what the game's window shows, scaled down to at most [maximumWidth] pixels wide.
     */
    @Serializable
    @SerialName("captureScreen")
    data class CaptureScreen(
        override val id: Int,
        val maximumWidth: Int,
    ) : SessionCommand

    /**
     * Waits until one of the Kubriko instances the game's `CreatorEntryPoint` reports has Actors within its viewport, and
     * they have been drawn, for at most [timeoutInMilliseconds]. Fails right away for a game whose entry point doesn't report
     * its instances. Added in protocol version 3.
     */
    @Serializable
    @SerialName("awaitActorsDrawn")
    data class AwaitActorsDrawn(
        override val id: Int,
        val timeoutInMilliseconds: Long,
    ) : SessionCommand

    /**
     * Holds the keys of each of [steps] down together for its duration, one step after the other with no pause between
     * them, then answers with a [SessionMessage.Screenshot] at most [screenshotWidth] pixels wide, or with a
     * [SessionMessage.CommandDone] when it is null. Added in protocol version 4.
     */
    @Serializable
    @SerialName("pressKeySteps")
    data class PressKeySteps(
        override val id: Int,
        val steps: List<KeyHold>,
        val screenshotWidth: Int? = null,
    ) : SessionCommand

    /**
     * Captures what the game's window shows, like [CaptureScreen], and finds in the same moment where its Actors whose
     * classes have one of the simple names [actorTypes] are, answered with a [SessionMessage.ActorsCaptured]. Fails for a
     * game whose entry point doesn't report its instances. Added in protocol version 4.
     */
    @Serializable
    @SerialName("captureActors")
    data class CaptureActors(
        override val id: Int,
        val maximumWidth: Int,
        val actorTypes: List<String>,
    ) : SessionCommand
}
