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
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.scenes.SceneActorEntry
import com.pandulapeter.kubrikoCreator.scenes.SceneFileProblem
import com.pandulapeter.kubrikoCreator.scenes.SceneStore
import java.io.File

/**
 * What the open Scene is made of, read from the project as it is now.
 */
public interface SceneSources {

    /**
     * The Actors of a Scene file, empty if it can't be read. Called off the main thread, while the project builds, so that the
     * [SceneStore] it reads from has the Scene parsed for the first command on it.
     */
    public fun readScene(scene: ProjectResource): List<SceneActorEntry>

    /**
     * Why a Scene file can't be read, or null if it can, called right after [readScene].
     */
    public fun readSceneProblem(scene: ProjectResource): SceneFileProblem? = null

    /**
     * The project's sprite files by the name of their resource accessor, which the Scene editor reads itself
     * ([SpriteLoadingEntryPoint]).
     */
    public fun spriteFiles(): Map<String, File> = emptyMap()

    /**
     * The looks of the block-built Actor types as they are now, whose changes the open Scene shows before they are compiled
     * ([SceneEditingState.Open.previews], kept current by [SceneEditingController.onActorTypesChanged]).
     */
    public fun actorTypeLooks(): Map<String, ActorTypeLook> = emptyMap()
}
