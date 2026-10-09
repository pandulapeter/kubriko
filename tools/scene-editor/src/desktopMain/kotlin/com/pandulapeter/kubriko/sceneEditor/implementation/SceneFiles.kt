/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation

import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.sceneEditor.SceneEditorMode
import com.pandulapeter.kubriko.sceneEditor.implementation.helpers.deserializeSceneOrNull
import com.pandulapeter.kubriko.sceneEditor.implementation.helpers.loadFile
import com.pandulapeter.kubriko.sceneEditor.implementation.helpers.saveFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Loading, saving and syncing the scene, the current file and the error to show when one of these fails.
 * Results are applied on [mainDispatcher]; the file access itself runs on [ioDispatcher].
 */
internal class SceneFiles(
    private val scope: CoroutineScope,
    private val sceneEditorMode: SceneEditorMode,
    defaultSceneFilename: String?,
    defaultSceneFolderPath: String,
    private val serializeScene: () -> String,
    private val deserializeScene: (String) -> List<Editable<*>>,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val _currentFolderPath = MutableStateFlow(defaultSceneFolderPath)
    val currentFolderPath = _currentFolderPath.asStateFlow()
    private val _currentFileName = MutableStateFlow(defaultSceneFilename ?: DEFAULT_SCENE_FILE_NAME)
    val currentFileName = _currentFileName.asStateFlow()
    private val _shouldShowLoadingIndicator = MutableStateFlow(false)
    val shouldShowLoadingIndicator = _shouldShowLoadingIndicator.asStateFlow()
    private val _fileOperationError = MutableStateFlow<Pair<FileOperationError, String>?>(null)
    val fileOperationError = _fileOperationError.asStateFlow()

    /**
     * Returns the actors of a connected scene's [sceneJson], or null when it is blank or unreadable (reported as an error).
     */
    fun readConnectedScene(sceneJson: String): List<Editable<*>>? {
        if (sceneJson.isBlank()) {
            return null
        }
        val actors = deserializeSceneOrNull(sceneJson, deserializeScene)
        if (actors == null) {
            _fileOperationError.update { FileOperationError.CONNECTED_SCENE_INVALID to "" }
        }
        return actors
    }

    fun resetFileName() = _currentFileName.update { DEFAULT_SCENE_FILE_NAME }

    fun loadMap(path: String, onLoaded: (List<Editable<*>>) -> Unit) {
        _shouldShowLoadingIndicator.update { true }
        scope.launch(mainDispatcher) {
            try {
                val json = loadFile(path, ioDispatcher)
                val actors = withContext(Dispatchers.Default) {
                    deserializeSceneOrNull(json, deserializeScene)
                }
                if (actors == null) {
                    reportFileOperationError(FileOperationError.LOAD_FAILED, path)
                } else {
                    onLoaded(actors)
                    updateCurrentFolderPathAndFileName(path)
                }
            } catch (_: IOException) {
                reportFileOperationError(FileOperationError.LOAD_FAILED, path)
            } finally {
                _shouldShowLoadingIndicator.update { false }
            }
        }
    }

    fun onFileOperationErrorShown() = _fileOperationError.update { null }

    private fun reportFileOperationError(error: FileOperationError, path: String) = _fileOperationError.update { error to path.split('/').last() }

    fun syncScene() {
        val onSceneJsonChanged = (sceneEditorMode as? SceneEditorMode.Connected)?.onSceneJsonChanged ?: return
        val content = serializeScene()
        scope.launch {
            onSceneJsonChanged(content)
        }
    }

    fun saveScene(path: String, onSaved: () -> Unit) {
        val content = serializeScene()
        scope.launch(mainDispatcher) {
            try {
                saveFile(
                    path = path,
                    content = content,
                    dispatcher = ioDispatcher,
                )
                updateCurrentFolderPathAndFileName(path)
                onSaved()
            } catch (_: IOException) {
                reportFileOperationError(FileOperationError.SAVE_FAILED, path)
            }
        }
    }

    private fun updateCurrentFolderPathAndFileName(path: String) {
        _currentFolderPath.update { path.split('/').let { it.take(it.size - 1) }.joinToString("/") }
        _currentFileName.update { path.split('/').last() }
    }

    private companion object {
        const val DEFAULT_SCENE_FILE_NAME = "scene_untitled.json"
    }
}
