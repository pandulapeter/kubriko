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

import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import java.io.File
import java.net.URI

/**
 * Takes files dropped onto this element from outside Kubriko Creator, handing what [among] makes of them (the ones it
 * takes) to [onDrop]; a drop it takes none of is refused.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun <T> Modifier.fileDropTarget(among: (List<File>) -> List<T>, onDrop: (List<T>) -> Unit): Modifier {
    val currentAmong by rememberUpdatedState(among)
    val currentOnDrop by rememberUpdatedState(onDrop)
    val target = remember {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                val files = (event.dragData() as? DragData.FilesList)?.readFiles().orEmpty().mapNotNull { runCatching { File(URI(it)) }.getOrNull() }
                val taken = currentAmong(files)
                if (taken.isEmpty()) return false
                currentOnDrop(taken)
                return true
            }
        }
    }
    return dragAndDropTarget(shouldStartDragAndDrop = { it.dragData() is DragData.FilesList }, target = target)
}
