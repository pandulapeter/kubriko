/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.sceneRuntime
import com.pandulapeter.kubrikoCreator.session.ProjectClasspath

/**
 * Loads a project's classes and finds its entry point, or throws an [EntryPointLoadingException].
 */
public fun interface EntryPointLoader {

    /**
     * Loads the classes of [classpath] and finds the project's entry point among them.
     */
    public fun load(classpath: ProjectClasspath): LoadedEntryPoint
}
