/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.scenes

import kotlinx.serialization.json.JsonObject

/**
 * [this] state with the value of the Property called [oldName] stored as [newName], in the same place.
 */
public fun JsonObject.withValueRenamed(oldName: String, newName: String): JsonObject = if (oldName in this) {
    JsonObject(entries.filter { it.key != newName }.associate { (key, value) -> (if (key == oldName) newName else key) to value })
} else {
    this
}
