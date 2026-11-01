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

/**
 * What [BlockTypeRepository.load] found: the types it could read, sorted by name, and the files it couldn't.
 */
internal data class LoadedBlockTypes(
    val types: List<BlockTypeDefinition>,
    val unreadableFiles: List<UnreadableBlockTypeFile> = emptyList(),
)

/**
 * A type definition that couldn't be read (a merge conflict, a crash mid-write, or a typo in a hand edit).
 *
 * @param path The file's path, relative to the project root.
 * @param reason Why it couldn't be read, on one line.
 */
internal data class UnreadableBlockTypeFile(
    val path: String,
    val reason: String,
)
