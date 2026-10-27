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

import java.io.File
import javax.swing.JFileChooser

/**
 * Asks the user for a folder, returning null if they cancel.
 */
internal fun pickFolder(title: String, initialDirectory: File? = null): File? = JFileChooser(initialDirectory).run {
    dialogTitle = title
    fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
    if (showOpenDialog(null) == JFileChooser.APPROVE_OPTION) selectedFile else null
}
