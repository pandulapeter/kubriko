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
 */
internal data class ProjectResource(
    val group: ResourceGroup,
    val file: File,
    val relativePath: String,
) {
    val name: String get() = file.name
}
