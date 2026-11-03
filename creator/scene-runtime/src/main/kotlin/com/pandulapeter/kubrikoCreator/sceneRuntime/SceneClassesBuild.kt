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
import com.pandulapeter.kubrikoCreator.session.SessionInitScript
import java.io.File

/**
 * How the project's classes are built for the Scene editor, one build at a time.
 */
public interface SceneClassesBuild {

    /**
     * Where the classpath task writes the project's classpath, the same file for every build ([SessionInitScript.classpathFileOf]).
     */
    public val classpathFile: File

    /**
     * Runs one of the init script's tasks with the given properties, reporting problems in the console; whether it worked.
     */
    public suspend fun runTask(taskName: String, properties: Map<String, String>): Boolean

    /**
     * Makes the next build compile everything from scratch, for when the classes may not match the sources: on Reload, and
     * when they couldn't be loaded.
     */
    public fun requestCleanBuild() {}

    /**
     * Gets the compiler ready for the first change, once [SceneEditingController.prepare] has loaded the classes.
     */
    public suspend fun warmUpCompiler() {}

    /**
     * Returns once the Actor types are loaded and their changes written, so that a build knows which looks it compiles.
     */
    public suspend fun awaitWrites() {}
}
