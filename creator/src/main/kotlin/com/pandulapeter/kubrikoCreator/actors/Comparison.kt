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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * How a condition compares a number with a value, with the Kotlin operator it becomes.
 */
@Serializable
internal enum class Comparison(
    val operator: String,
) {
    @SerialName("less")
    LESS("<"),

    @SerialName("lessOrEqual")
    LESS_OR_EQUAL("<="),

    @SerialName("equal")
    EQUAL("=="),

    @SerialName("notEqual")
    NOT_EQUAL("!="),

    @SerialName("greaterOrEqual")
    GREATER_OR_EQUAL(">="),

    @SerialName("greater")
    GREATER(">"),
}
