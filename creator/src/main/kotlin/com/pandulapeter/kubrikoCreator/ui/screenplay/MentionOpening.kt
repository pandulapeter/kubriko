/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.screenplay

import com.pandulapeter.kubrikoCreator.screenplay.Mention
import com.pandulapeter.kubrikoCreator.workspace.SelectionController
import com.pandulapeter.kubrikoCreator.workspace.WorkspaceSelection

/**
 * Shows what [mention] names in the workspace's center: its type, Scene, page, or document. Other kinds have nothing
 * to show it in, so nothing happens for them.
 */
internal fun SelectionController.open(mention: Mention) {
    when (mention) {
        is Mention.ActorType -> selectType(mention.name)
        is Mention.ManagerType -> selectType(mention.name)
        is Mention.Scene -> openScene(mention.name)
        is Mention.Page -> openPage(mention.name)
        is Mention.Document -> openDocument(mention.path)
        is Mention.Sprite, is Mention.Audio, is Mention.Kotlin, is Mention.OtherFile -> Unit
    }
}

/**
 * Shows the page or document [selection] names, which is all the page editor follows.
 */
internal fun SelectionController.open(selection: WorkspaceSelection) {
    when (selection) {
        is WorkspaceSelection.Page -> openPage(selection.name)
        is WorkspaceSelection.Document -> openDocument(selection.path)
        else -> Unit
    }
}
