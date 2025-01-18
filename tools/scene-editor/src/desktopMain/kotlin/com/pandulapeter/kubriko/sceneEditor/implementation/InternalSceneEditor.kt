/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.AwtWindow
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.persistence.PersistenceManager
import com.pandulapeter.kubriko.pointerInput.PointerInputManager
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.sceneEditor.EditableMetadata
import com.pandulapeter.kubriko.sceneEditor.SceneEditorMode
import com.pandulapeter.kubriko.sceneEditor.implementation.overlay.OverlayManager
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.EditorUserInterface
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.settings.Settings
import com.pandulapeter.kubriko.serialization.SerializationManager
import java.awt.Dimension
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
internal fun InternalSceneEditor(
    defaultSceneFilename: String?,
    defaultSceneFolderPath: String,
    serializationManager: SerializationManager<EditableMetadata<*>, Editable<*>>,
    sceneEditorMode: SceneEditorMode,
    title: String,
    onCloseRequest: () -> Unit,
) {
    val editorKubriko = remember {
        Kubriko.newInstance(
            ViewportManager.newInstance(
                aspectRatioMode = ViewportManager.AspectRatioMode.Dynamic,
                minimumScaleFactor = MINIMUM_SCALE_FACTOR,
                maximumScaleFactor = MAXIMUM_SCALE_FACTOR,
            ),
            StateManager.newInstance(shouldAutoStart = false),
            KeyboardInputManager.newInstance(),
            PointerInputManager.newInstance(),
            PersistenceManager.newInstance(fileName = "kubrikoSceneEditor"),
            serializationManager,
            instanceNameForLogging = "Editor",
        )
    }
    val isLoadFileChooserOpen = remember { mutableStateOf(false) }
    val isSaveFileChooserOpen = remember { mutableStateOf(false) }
    val isSettingsOpen = remember { mutableStateOf(false) }

    lateinit var overlayKubriko: Kubriko

    fun disposeAndClose() {
        editorKubriko.dispose()
        overlayKubriko.dispose()
        onCloseRequest()
    }

    val editorController = remember {
        EditorController(
            kubriko = editorKubriko,
            sceneEditorMode = sceneEditorMode,
            defaultSceneFilename = defaultSceneFilename,
            defaultSceneFolderPath = defaultSceneFolderPath,
            onCloseRequest = {
                if (isSettingsOpen.value) {
                    isSettingsOpen.value = false
                } else if (!isLoadFileChooserOpen.value && !isSaveFileChooserOpen.value) {
                    disposeAndClose()
                }
            },
        )
    }
    val overlayManager = remember { OverlayManager(editorController) }

    overlayKubriko = remember {
        Kubriko.newInstance(
            ViewportManager.newInstance(
                aspectRatioMode = ViewportManager.AspectRatioMode.Dynamic,
                minimumScaleFactor = MINIMUM_SCALE_FACTOR,
                maximumScaleFactor = MAXIMUM_SCALE_FACTOR,
            ),
            overlayManager,
        )
    }

    Window(
        onCloseRequest = ::disposeAndClose,
        title = title,
    ) {
        window.minimumSize = Dimension(600, 400)
        EditorUserInterface(
            editorController = editorController,
            openFilePickerForLoading = { isLoadFileChooserOpen.value = true },
            openFilePickerForSaving = { isSaveFileChooserOpen.value = true },
            openSettings = { isSettingsOpen.value = !isSettingsOpen.value },
            overlayKubriko = overlayKubriko,
        )
        if (isLoadFileChooserOpen.value) {
            FileDialog(
                parent = window,
                currentFileName = editorController.currentFileName.value,
                currentFolderPath = editorController.currentFolderPath.value,
                isForLoading = true,
                onCloseRequest = { directory, fileName ->
                    isLoadFileChooserOpen.value = false
                    if (fileName != null) {
                        editorController.loadMap("$directory/$fileName")
                    }
                }
            )
        }
        if (isSaveFileChooserOpen.value) {
            FileDialog(
                parent = window,
                currentFileName = editorController.currentFileName.value,
                currentFolderPath = editorController.currentFolderPath.value,
                isForLoading = false,
                onCloseRequest = { directory, fileName ->
                    isSaveFileChooserOpen.value = false
                    if (fileName != null) {
                        editorController.saveScene("$directory/$fileName")
                    }
                }
            )
        }
    }
    if (isSettingsOpen.value) {
        Window(
            onCloseRequest = { isSettingsOpen.value = false },
            title = "Editor Settings",
            state = rememberWindowState(
                size = DpSize(200.dp, 250.dp),
            )
        ) {
            window.minimumSize = Dimension(200, 250)
            Settings(
                colorEditorMode = editorController.colorEditorMode.collectAsState().value,
                onColorEditorModeChanged = editorController::onColorEditorModeChanged,
                angleEditorMode = editorController.angleEditorMode.collectAsState().value,
                onAngleEditorModeChanged = editorController::onAngleEditorModeChanged,
                isDebutMenuEnabled = editorController.isDebugMenuEnabled.collectAsState().value,
                onIsDebutMenuEnabledChanged = editorController::onIsDebugMenuEnabledChanged,
            )
        }
    }
}

@Composable
private fun FileDialog(
    parent: Frame? = null,
    currentFileName: String?,
    currentFolderPath: String,
    isForLoading: Boolean,
    onCloseRequest: (directory: String?, fileName: String?) -> Unit
) = AwtWindow(
    create = {
        object : FileDialog(parent, "Scene file", if (isForLoading) LOAD else SAVE) {
            init {
                val scenesDirectoryFile = File(currentFolderPath)
                scenesDirectoryFile.parentFile?.mkdirs()
                if (!scenesDirectoryFile.exists()) {
                    scenesDirectoryFile.mkdir()
                }
//                filenameFilter = FilenameFilter { _, name ->
//                    name.endsWith(".json")
//                }
                directory = currentFolderPath
                if (!isForLoading) {
                    file = currentFileName
                }
            }

            override fun setVisible(value: Boolean) {
                super.setVisible(value)
                if (value) {
                    onCloseRequest(directory, file)
                }
            }
        }
    },
    dispose = FileDialog::dispose
)

internal const val MINIMUM_SCALE_FACTOR = 0.1f
internal const val MAXIMUM_SCALE_FACTOR = 10f