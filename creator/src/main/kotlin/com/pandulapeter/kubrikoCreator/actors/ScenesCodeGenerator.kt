/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

/**
 * The Manager that loads Scenes for the goToScene and restartScene Blocks, and the first Scene when the game starts.
 * Generated into every project, so that `KubrikoWiring` can always create it.
 */
internal object ScenesCodeGenerator {

    const val SCENES_NAME = "GeneratedScenes"

    /**
     * Where Scene files are read from, relative to the project's `composeResources`, as the templates keep them.
     */
    const val SCENE_DIRECTORY = "files/scenes"

    fun generate(packageName: String) = GeneratedFile.withHeader("", buildString {
        appendLine("package $packageName.${ActorTypeCodeGenerator.GENERATED_PACKAGE}")
        appendLine()
        listOf(
            "com.pandulapeter.kubriko.Kubriko",
            "com.pandulapeter.kubriko.actor.Actor",
            "com.pandulapeter.kubriko.logger.Logger",
            "com.pandulapeter.kubriko.manager.ActorManager",
            "com.pandulapeter.kubriko.manager.Manager",
            "com.pandulapeter.kubriko.sceneEditor.Editable",
            "com.pandulapeter.kubriko.sceneEditor.EditableMetadata",
            "com.pandulapeter.kubriko.serialization.SerializationManager",
            "kotlinx.coroutines.CancellationException",
            "kotlinx.coroutines.Job",
            "kotlinx.coroutines.launch",
            "$packageName.resources.Res",
        ).forEach { appendLine("import $it") }
        appendLine()
        appendLine(
            """
            |/**
            | * Loads the Scenes of the game: the first one when it starts, and others when Blocks go to them or restart them.
            | *
            | * @param firstScene The name of the Scene loaded when the game starts, or null to load none.
            | * @param isInSceneEditor True in the Scene editor, which loads its Scenes itself.
            | * @param managerActors The Managers that are Actors too, which every Scene keeps.
            | */
            |class $SCENES_NAME(
            |    private val firstScene: String? = null,
            |    private val isInSceneEditor: Boolean = false,
            |    private val managerActors: List<Actor> = emptyList(),
            |) : Manager() {
            |    private val actorManager by manager<ActorManager>()
            |    private val serializationManager by manager<SerializationManager<EditableMetadata<*>, Editable<*>>>()
            |    private var loading: Job? = null
            |
            |    /**
            |     * The name of the Scene shown (or being loaded), or null before the first one.
            |     */
            |    var currentScene: String? = null
            |        private set
            |
            |    override fun onInitialize(kubriko: Kubriko) {
            |        if (!isInSceneEditor && firstScene != null) load(firstScene, isFirst = true)
            |    }
            |
            |    /**
            |     * Removes every Actor and adds the Actors of [scene] instead, once its file is read.
            |     */
            |    fun goTo(scene: String) {
            |        if (!isInSceneEditor) load(scene, isFirst = false)
            |    }
            |
            |    /**
            |     * Loads the current Scene again, as it was placed.
            |     */
            |    fun restart() {
            |        currentScene?.let(::goTo)
            |    }
            |
            |    private fun load(scene: String, isFirst: Boolean) {
            |        currentScene = scene
            |        loading?.cancel()
            |        loading = scope.launch {
            |            val actors = try {
            |                serializationManager.deserializeActors(Res.readBytes("$SCENE_DIRECTORY/${'$'}scene.json").decodeToString())
            |            } catch (exception: CancellationException) {
            |                throw exception
            |            } catch (exception: Exception) {
            |                Logger.log(message = "The Scene ${'$'}scene couldn't be loaded.", details = exception.message, source = "$SCENES_NAME")
            |                return@launch
            |            }
            |            // The first Scene keeps what the Managers added when the game started.
            |            if (!isFirst) {
            |                actorManager.removeAll()
            |                actorManager.add(managerActors)
            |            }
            |            actorManager.add(actors)
            |        }
            |    }
            |}
            """.trimMargin(),
        )
    })
}
