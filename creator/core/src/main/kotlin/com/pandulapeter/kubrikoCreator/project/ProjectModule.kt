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
 * A Gradle module of a project.
 *
 * @param usesKubriko Whether its build file names a Kubriko library itself, through the version catalog (a library or a
 *   bundle holding one) or by its coordinates. One that only depends on a module that does (an app packaging the game)
 *   doesn't.
 * @param isBuildRoot Whether it is a build of its own rather than a part of the project's (`buildSrc`, or an included
 *   build with a settings file of its own), so it isn't inside the folders above it.
 */
public data class ProjectModule(
    val usesKubriko: Boolean,
    val isBuildRoot: Boolean = false,
)
