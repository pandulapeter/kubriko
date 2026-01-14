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

import java.io.File

internal suspend fun loadFile(path: String): String? = try {
    File(path).readBytes().decodeToString()
} catch (_: Exception) {
    null
}

internal suspend fun saveFile(path: String, content: String) = File(path).let { file ->
    file.parentFile?.mkdirs()
    if (!file.exists()) {
        file.createNewFile()
    }
    file.writeText(content)
}