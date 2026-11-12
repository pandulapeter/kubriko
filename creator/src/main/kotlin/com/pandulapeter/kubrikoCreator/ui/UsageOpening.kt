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

import com.pandulapeter.kubrikoCreator.context.RelationSubject
import com.pandulapeter.kubrikoCreator.context.Usage
import com.pandulapeter.kubrikoCreator.workspace.SelectionController

/**
 * Shows the Actor type, Manager type, Scene, or page [usage] is in ([placeOwner]), if it is in one.
 */
internal fun SelectionController.open(usage: Usage) {
    when (val owner = usage.placeOwner) {
        is RelationSubject.Type -> selectType(owner.name)
        is RelationSubject.Scene -> openScene(owner.name)
        is RelationSubject.Page -> openPage(owner.name)
        is RelationSubject.Property, is RelationSubject.Image, is RelationSubject.Resource, is RelationSubject.Timer, is RelationSubject.Folder, null -> Unit
    }
}
