/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.folders

import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A group of the sidebar that has folders.
 */
@Serializable
internal enum class FolderGroup(
    val kind: BlockTypeKind,
) {
    @SerialName("actorTypes")
    ACTOR_TYPES(BlockTypeKind.ACTOR),

    @SerialName("managerTypes")
    MANAGER_TYPES(BlockTypeKind.MANAGER),
    ;

    companion object {
        fun of(kind: BlockTypeKind) = entries.first { it.kind == kind }
    }
}
