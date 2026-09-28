/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation.helpers

import kotlin.coroutines.cancellation.CancellationException

/**
 * Deserializes a scene, returning null when [json] is not a readable scene: when [deserialize] throws, or when it
 * returns nothing for content that is not an empty array (the deserializer reports malformed input as an empty list).
 */
internal fun <T> deserializeSceneOrNull(
    json: String,
    deserialize: (String) -> List<T>,
): List<T>? {
    val actors = try {
        deserialize(json)
    } catch (exception: CancellationException) {
        throw exception
    } catch (_: Exception) {
        return null
    }
    return if (actors.isEmpty() && json.filterNot(Char::isWhitespace) != "[]") null else actors
}
