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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import com.pandulapeter.kubrikoCreator.actors.GeneratedCodeLocation
import com.pandulapeter.kubrikoCreator.actors.codegen.ManagerComposables
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.ui.components.openInSystemEditor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Opens the file declaring the hand-written Composable of a fully qualified name in the system's editor, calling back on
 * the main thread when there is no such file or it can't be opened, where the workspace provides it.
 */
internal val LocalComposableOpener = staticCompositionLocalOf<((name: String, onFailed: () -> Unit) -> Unit)?> { null }

/**
 * What opens a Composable's file among [resources]' Kotlin files, reading them off the main thread when asked, or the
 * file `create_composable` makes for it in the project at [rootDirectory] whose code is at [location], which the
 * resource scan may not know of yet.
 */
@Composable
internal fun rememberComposableOpener(
    resources: Map<ResourceGroup, List<ProjectResource>>,
    rootDirectory: File,
    location: GeneratedCodeLocation?,
): (String, () -> Unit) -> Unit {
    val scope = rememberCoroutineScope()
    val currentResources by rememberUpdatedState(resources)
    val currentLocation by rememberUpdatedState(location)
    return remember(rootDirectory) {
        { name, onFailed ->
            scope.launch {
                val isOpened = withContext(Dispatchers.IO) {
                    val madeFile = currentLocation?.let { made ->
                        File(rootDirectory, ManagerComposables.pathOf(made, name.substringAfterLast('.'))).takeIf { ManagerComposables.qualifiedNameOf(made, name.substringAfterLast('.')) == name }
                    }
                    val kotlinFiles = currentResources[ResourceGroup.KOTLIN].orEmpty() + listOfNotNull(madeFile?.let { ProjectResource(ResourceGroup.KOTLIN, it, it.path) })
                    composableFileOf(name, kotlinFiles) { file -> runCatching { file.readText() }.getOrNull() }
                        ?.let { file -> runCatching { openInSystemEditor(file) }.isSuccess } == true
                }
                if (!isOpened) onFailed()
            }
        }
    }
}

/**
 * The file among [kotlinFiles] declaring the Composable named [name] in full: one in its package (read through [readText])
 * declaring a function of its name, looking first at those named after it, as `create_composable` makes them; or null.
 */
internal fun composableFileOf(name: String, kotlinFiles: List<ProjectResource>, readText: (File) -> String?): File? {
    val packageName = name.substringBeforeLast('.', "")
    val functionName = name.substringAfterLast('.')
    val declaration = Regex("\\bfun\\s+" + Regex.escape(functionName) + "\\s*\\(")
    val packageLine = Regex("^\\s*package\\s+" + Regex.escape(packageName) + "\\s*;?\\s*(//.*|/\\*.*)?$", RegexOption.MULTILINE)
    return kotlinFiles.map { it.file }
        .sortedBy { it.nameWithoutExtension != functionName }
        .firstOrNull { file -> readText(file)?.let { packageLine.containsMatchIn(it) && declaration.containsMatchIn(it) } == true }
}
