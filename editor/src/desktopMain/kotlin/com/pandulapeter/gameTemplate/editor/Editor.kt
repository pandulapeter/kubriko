package com.pandulapeter.gameTemplate.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.window.AwtWindow
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.pandulapeter.gameTemplate.editor.implementation.EditorApp
import com.pandulapeter.gameTemplate.editor.implementation.EditorController
import com.pandulapeter.gameTemplate.engine.Engine
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import java.awt.Dimension
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FilenameFilter

fun openEditor(
    defaultMapFilename: String? = null,
    vararg supportedGameObjectSerializers: Pair<String, (String) -> GameObject.Serializer<*>>
) = application {
    Engine.get().gameObjectManager.register(entries = supportedGameObjectSerializers)
    defaultMapFilename?.let {
        EditorController.loadMap(
            path = "${EditorController.MAPS_DIRECTORY}/$it.json"
        )
    }
    Window(
        onCloseRequest = ::exitApplication,
        title = "Editor",
    ) {
        val isLoadFileChooserOpen = remember { mutableStateOf(false) }
        val isSaveFileChooserOpen = remember { mutableStateOf(false) }
        window.minimumSize = Dimension(400, 400)
        EditorApp(
            openFilePickerForLoading = { isLoadFileChooserOpen.value = true },
            openFilePickerForSaving = { isSaveFileChooserOpen.value = true },
        )
        if (isLoadFileChooserOpen.value) {
            FileDialog(
                parent = window,
                isForLoading = true,
                onCloseRequest = { directory, fileName ->
                    isLoadFileChooserOpen.value = false
                    if (fileName != null) {
                        EditorController.loadMap("$directory/$fileName")
                    }
                }
            )
        }
        if (isSaveFileChooserOpen.value) {
            FileDialog(
                parent = window,
                isForLoading = false,
                onCloseRequest = { directory, fileName ->
                    isSaveFileChooserOpen.value = false
                    if (fileName != null) {
                        EditorController.saveMap("$directory/$fileName")
                    }
                }
            )
        }
    }
}

@Composable
private fun FileDialog(
    parent: Frame? = null,
    isForLoading: Boolean,
    onCloseRequest: (directory: String?, fileName: String?) -> Unit
) = AwtWindow(
    create = {
        object : FileDialog(parent, "Choose a file", if (isForLoading) LOAD else SAVE) {
            init {
                val mapsDirectoryFile = File(EditorController.MAPS_DIRECTORY)
                mapsDirectoryFile.parentFile?.mkdirs()
                if (!mapsDirectoryFile.exists()) {
                    mapsDirectoryFile.mkdir()
                }
                filenameFilter = FilenameFilter { _, name ->
                    name.endsWith(".json")
                }
                directory = EditorController.MAPS_DIRECTORY
                if (!isForLoading) {
                    file = EditorController.currentFileName.value
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