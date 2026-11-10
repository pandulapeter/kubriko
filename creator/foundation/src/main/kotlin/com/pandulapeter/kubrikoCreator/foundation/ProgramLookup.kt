/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.foundation

import java.io.File

/**
 * The folders a `PATH` variable's [value] lists, leaving out empty entries, which some shells read as the current folder.
 */
public fun pathFolders(value: String? = System.getenv("PATH")): List<File> = value.orEmpty().split(File.pathSeparator).filter { it.isNotBlank() }.map(::File)

/**
 * The first program named [name] in [folders] that can be run, or null if there is none.
 */
public fun findProgram(name: String, folders: List<File> = pathFolders()): File? = folders.map { File(it, name) }.firstOrNull { it.isFile && it.canExecute() }
