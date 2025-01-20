/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.particleEditor.implementation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.pointerInput.PointerInputManager
import java.awt.Dimension

@Composable
internal fun InternalParticleEditor(
    title: String,
    onCloseRequest: () -> Unit,
) {
    val editorKubriko = remember {
        Kubriko.newInstance(
            ViewportManager.newInstance(
                aspectRatioMode = ViewportManager.AspectRatioMode.Dynamic,
            ),
            PointerInputManager.newInstance(),
            instanceNameForLogging = "ParticleEditor",
        )
    }

    fun disposeAndClose() {
        editorKubriko.dispose()
        onCloseRequest()
    }

    Window(
        onCloseRequest = ::disposeAndClose,
        title = title,
    ) {
        window.minimumSize = Dimension(600, 400)
    }
}