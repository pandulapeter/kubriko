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

import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * How closely a read tool looks at a type: how much of it its result holds.
 */
@Serializable
@Description("How much of the type to return.")
public enum class ReadDetail {

    @Description("Its Events by title, each with the number of its Blocks: the cheapest way to find the Event to look at.")
    @SerialName("outline")
    OUTLINE,

    @Description("Its Events with their Blocks, one short line each as the editor shows them, indexed and indented by the if and repeat Blocks they are in.")
    @SerialName("blocks")
    BLOCKS,

    @Description("The whole definition as JSON, every value included, in the form the write tools take.")
    @SerialName("json")
    JSON,
}
