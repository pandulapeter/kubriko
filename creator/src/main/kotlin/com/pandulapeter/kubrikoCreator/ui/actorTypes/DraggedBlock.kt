/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.actorTypes

import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockKind

/**
 * A Block being dragged in the Actor type editor: a new one from the palette, or one of the selected Event's.
 */
internal sealed interface DraggedBlock {

    val kind: BlockKind

    data class New(
        override val kind: BlockKind,
    ) : DraggedBlock

    data class Existing(
        val index: Int,
        val block: BlockDefinition,
    ) : DraggedBlock {
        override val kind get() = BlockKind.of(block)
    }
}

/**
 * A Block being dragged in the Actor type editor, dropped into the selected Event's Blocks.
 */
internal typealias BlockDragState = ListDragState<DraggedBlock>
