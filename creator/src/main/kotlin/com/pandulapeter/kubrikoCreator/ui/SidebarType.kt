/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind

/**
 * What the sidebar shows of a block-built type.
 */
internal data class SidebarType(
    val kind: BlockTypeKind,
    val name: String,
    val folder: String,
) {
    companion object {
        fun of(definition: BlockTypeDefinition) = SidebarType(definition.kind, definition.name, definition.folder)
    }
}
