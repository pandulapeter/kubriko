/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.project

import java.io.File

/**
 * A Kubriko project opened in Kubriko Creator.
 *
 * @param rootDirectory The directory of the project's root Gradle build.
 * @param name The root project's name.
 */
internal data class CreatorProject(
    val rootDirectory: File,
    val name: String,
)
