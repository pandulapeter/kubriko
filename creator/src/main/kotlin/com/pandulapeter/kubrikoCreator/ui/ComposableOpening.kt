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
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.ui.components.openInSystemEditor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * Opens the file declaring the hand-written Composable of a fully qualified name in the system's editor, where the
 * workspace provides it.
 */
internal val LocalComposableOpener = staticCompositionLocalOf<((String) -> Unit)?> { null }

/**
 * What opens a Composable's file among [resources]' Kotlin files, reading them off the main thread when asked.
 */
@Composable
internal fun rememberComposableOpener(resources: Map<ResourceGroup, List<ProjectResource>>): (String) -> Unit {
    val scope = rememberCoroutineScope()
    val currentResources by rememberUpdatedState(resources)
    return remember {
        { name ->
            scope.launch(Dispatchers.IO) {
                composableFileOf(name, currentResources[ResourceGroup.KOTLIN].orEmpty()) { file -> runCatching { file.readText() }.getOrNull() }
                    ?.let { file -> runCatching { openInSystemEditor(file) } }
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
    val packageLine = Regex("^\\s*package\\s+" + Regex.escape(packageName) + "\\s*;?\\s*$", RegexOption.MULTILINE)
    return kotlinFiles.map { it.file }
        .sortedBy { it.nameWithoutExtension != functionName }
        .firstOrNull { file -> readText(file)?.let { packageLine.containsMatchIn(it) && declaration.containsMatchIn(it) } == true }
}
