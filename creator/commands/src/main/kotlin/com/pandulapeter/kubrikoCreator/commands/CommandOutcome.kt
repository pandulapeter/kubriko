/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull

/**
 * What running a command or calling a tool came to.
 */
public sealed interface CommandOutcome {

    /**
     * @param image A PNG that goes with the [result] (a screenshot, for example).
     */
    public class Success(
        public val result: JsonElement = JsonNull,
        public val image: ByteArray? = null,
    ) : CommandOutcome {

        override fun equals(other: Any?): Boolean = other is Success && other.result == result && other.image.contentEquals(image)

        override fun hashCode(): Int = 31 * result.hashCode() + image.contentHashCode()

        override fun toString(): String = "Success(result=$result, image=${image?.size} bytes)"
    }

    /**
     * The command was not run, for [rejection].
     */
    public data class Rejected(
        val rejection: Rejection,
    ) : CommandOutcome
}
