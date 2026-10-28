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

/**
 * The rules for Scene names, which are paths: folder names and the file's name separated by `/` (`levels/1-1`).
 */
internal object SceneNaming {

    private val partPattern = Regex("[A-Za-z0-9][A-Za-z0-9_-]*")

    fun isValid(name: String) = name.split('/').all(partPattern::matches)
}
