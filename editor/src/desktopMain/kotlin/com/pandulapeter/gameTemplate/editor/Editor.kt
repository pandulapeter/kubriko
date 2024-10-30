package com.pandulapeter.gameTemplate.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.window.AwtWindow
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.pandulapeter.gameTemplate.editor.implementation.EditorApp
import com.pandulapeter.gameTemplate.editor.implementation.EditorController
import com.pandulapeter.gameTemplate.engine.Engine
import com.pandulapeter.gameTemplate.engine.gameObject.traits.AvailableInEditor
import java.awt.Dimension
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FilenameFilter
import kotlin.reflect.KClass

fun openEditor(
    defaultMapFilename: String? = null,
    vararg typesAvailableInEditor: Triple<String, KClass<*>, (String) -> AvailableInEditor.State<*>>
) = application {
    val editorController = remember { EditorController(Engine.newInstance(typesAvailableInEditor = typesAvailableInEditor)) }
    LaunchedEffect(Unit) {
        defaultMapFilename?.let { editorController.loadMap("${EditorController.MAPS_DIRECTORY}/$it.json") }
    }
    Window(
        onCloseRequest = ::exitApplication,
        title = "Editor",
    ) {
        val isLoadFileChooserOpen = remember { mutableStateOf(false) }
        val isSaveFileChooserOpen = remember { mutableStateOf(false) }
        window.minimumSize = Dimension(600, 400)
        EditorApp(
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