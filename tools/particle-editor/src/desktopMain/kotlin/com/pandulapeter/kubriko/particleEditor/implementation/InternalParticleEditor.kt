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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.particleEditor.implementation.manager.ParticleEditorManager
import com.pandulapeter.kubriko.particleEditor.implementation.ui.EmitterPropertiesPanel
import com.pandulapeter.kubriko.pointerInput.PointerInputManager
import java.awt.Dimension

@Composable
internal fun InternalParticleEditor(
    title: String,
    onCloseRequest: () -> Unit,
) {
    val particleEditorManager = remember { ParticleEditorManager() }
    val editorKubriko = remember {
        Kubriko.newInstance(
            ViewportManager.newInstance(
                aspectRatioMode = ViewportManager.AspectRatioMode.Dynamic,
            ),
            PointerInputManager.newInstance(),
            particleEditorManager,
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
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier.weight(1f),
            ) {
                KubrikoViewport(
                    kubriko = editorKubriko,
                )
            }
            VerticalDivider()
            EmitterPropertiesPanel(
                modifier = Modifier.width(240.dp).fillMaxHeight(),
                particleEditorManager = particleEditorManager,
            )
        }
    }
}