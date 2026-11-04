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
 * What changed in a project, outside Kubriko Creator or inside it.
 */
public enum class ProjectChange {

    /**
     * Files in `.kubriko/actors` and `.kubriko/managers`.
     */
    BLOCK_TYPE_DEFINITIONS,

    /**
     * `.kubriko/project.json`.
     */
    SETTINGS,

    /**
     * Scene files.
     */
    SCENES,

    /**
     * Anything else in the project's source folders (code and resources), apart from Creator's generated code.
     */
    SOURCES,

    /**
     * The build files that make folders Gradle modules, which the resource tree groups files by, the root settings file
     * that includes them, which together decide the game's module, and the version catalog, through which a module's
     * build file names the Kubriko libraries that make it one of the game's.
     */
    MODULES,
}
