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
 * A file shown in the resource tree.
 *
 * @param file The file itself.
 * @param relativePath The file's path relative to the project root, with forward slashes.
 * @param kotlinPackage The package a Kotlin file declares (empty for the default package), or null for other files.
 * @param module The folder of the Gradle module the file belongs to, relative to the project root, with forward slashes
 *   (empty for the root project; [ResourceScanner.moduleOf] says which one that is).
 */
public data class ProjectResource(
    val group: ResourceGroup,
    val file: File,
    val relativePath: String,
    val kotlinPackage: String? = null,
    val module: String = "",
) {
    val name: String get() = file.name
}
