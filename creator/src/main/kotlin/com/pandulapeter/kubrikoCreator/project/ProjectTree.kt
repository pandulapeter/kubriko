/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.project

/**
 * What every walk through a project's files leaves out.
 */
internal object ProjectTree {

    /**
     * The folders build tools, IDEs, and version control keep their own files in, wherever they are.
     */
    val ignoredDirectoryNames = setOf("build", "out", ".gradle", ".git", ".idea", ".fleet", ".vscode", ".kotlin", ".cxx", "kotlin-js-store", "node_modules", "Pods", "DerivedData")
}
