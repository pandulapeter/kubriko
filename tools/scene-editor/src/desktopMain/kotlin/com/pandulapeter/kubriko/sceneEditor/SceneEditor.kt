package com.pandulapeter.kubriko.sceneEditor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.window.AwtWindow
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.pandulapeter.kubriko.sceneEditor.implementation.EditorController
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.EditorUserInterface
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.sceneEditorIntegration.EditableMetadata
import java.awt.Dimension
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FilenameFilter

fun openSceneEditor(
    defaultMapFilename: String? = null,
    vararg editableMetadata: EditableMetadata<*>,
) = application {
    val editorController = remember { EditorController(Kubriko.newInstance(editableMetadata = editableMetadata)) }
    LaunchedEffect(Unit) {
        defaultMapFilename?.let { editorController.loadMap("${EditorController.SCENES_DIRECTORY}/$it.json") }
    }
    Window(
        onCloseRequest = ::exitApplication,
        title = "Scene Editor",
    ) {
        val isLoadFileChooserOpen = remember { mutableStateOf(false) }
        val isSaveFileChooserOpen = remember { mutableStateOf(false) }
        window.minimumSize = Dimension(600, 400)
        EditorUserInterface(
            editorController = editorController,
            openFilePickerForLoading = { isLoadFileChooserOpen.value = true },
            openFilePickerForSaving = { isSaveFileChooserOpen.value = true },
        )
        if (isLoadFileChooserOpen.value) {
            FileDialog(
                parent = window,
                currentFileName = editorController.currentFileName.value,
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
                isForLoading = false,
                onCloseRequest = { directory, fileName ->
                    isSaveFileChooserOpen.value = false
                    if (fileName != null) {
                        editorController.saveMap("$directory/$fileName")
                    }
                }
            )
        }
    }
}

@Composable
private fun FileDialog(
    parent: Frame? = null,
    currentFileName: String?,
    isForLoading: Boolean,
    onCloseRequest: (directory: String?, fileName: String?) -> Unit
) = AwtWindow(
    create = {
        object : FileDialog(parent, "Choose a file", if (isForLoading) LOAD else SAVE) {
            init {
                val scenesDirectoryFile = File(EditorController.SCENES_DIRECTORY)
                scenesDirectoryFile.parentFile?.mkdirs()
                if (!scenesDirectoryFile.exists()) {
                    scenesDirectoryFile.mkdir()
                }
                filenameFilter = FilenameFilter { _, name ->
                    name.endsWith(".json")
                }
                directory = EditorController.SCENES_DIRECTORY
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