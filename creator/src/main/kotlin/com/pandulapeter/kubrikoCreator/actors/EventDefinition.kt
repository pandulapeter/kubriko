/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

import com.pandulapeter.kubrikoCreator.commands.Description
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One Event of an Actor type and the Blocks that run when it happens.
 *
 * @param key The key of a [EventType.KEY_PRESSED] Event; null for every other type.
 * @param otherActorType The Actor type a [EventType.COLLISION] Event reacts to; null for every other type, and until
 *   one is chosen.
 * @param timer The name of the Timer a [EventType.TIMER_FINISHED] Event reacts to ([TimerNaming]); null for every other type.
 */
@Serializable
@Description("Something an Actor reacts to, with the Blocks it runs when it happens.")
internal data class EventDefinition(
    @Description("When the Event happens.")
    @SerialName("type") val type: EventType,
    @Description("The key a keyPressed Event reacts to, " + KeyChoice.DESCRIPTION + " Only keyPressed Events have one.")
    @SerialName("key") val key: KeyChoice? = null,
    @Description("The Actor type a collision Event reacts to overlapping. Only collision Events have one.")
    @SerialName("otherActorType") val otherActorType: String? = null,
    @Description("The name of the Timer a timerFinished Event reacts to, like spawnEnemy: a letter, then letters and digits, starting lowercase. Only timerFinished Events have one.")
    @Serializable(with = TimerNameSerializer::class)
    @SerialName("timer") val timer: String? = null,
    @Description("The Blocks the Event runs, in order.")
    @SerialName("blocks") val blocks: List<BlockDefinition> = emptyList(),
)
