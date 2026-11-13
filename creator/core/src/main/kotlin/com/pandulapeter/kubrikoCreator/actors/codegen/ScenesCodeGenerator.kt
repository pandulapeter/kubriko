/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors.codegen

import com.pandulapeter.kubrikoCreator.actors.GeneratedNames
import com.pandulapeter.kubrikoCreator.actors.SceneEdge

/**
 * The Manager that loads Scenes for the goToScene and restartScene Blocks, and the first Scene when the game starts.
 * Generated into every project, so that `KubrikoWiring` can always create it.
 */
public object ScenesCodeGenerator {

    public const val SCENES_NAME: String = GeneratedNames.SCENES

    /**
     * Where Scene files are read from, relative to the project's `composeResources`, as the templates keep them.
     */
    public const val SCENE_DIRECTORY: String = "files/scenes"

    private const val LEFT_EDGE = "leftEdge"
    private const val RIGHT_EDGE = "rightEdge"
    private const val TOP_EDGE = "topEdge"
    private const val BOTTOM_EDGE = "bottomEdge"

    /**
     * The getter of the generated class saying where [edge] is, in scene units.
     */
    internal fun edgeGetterOf(edge: SceneEdge) = when (edge) {
        SceneEdge.LEFT -> LEFT_EDGE
        SceneEdge.RIGHT -> RIGHT_EDGE
        SceneEdge.TOP -> TOP_EDGE
        SceneEdge.BOTTOM -> BOTTOM_EDGE
    }

    internal fun generate(packageName: String) = GeneratedFile.withHeader("", buildString {
        appendLine("package $packageName.${ActorTypeCodeGenerator.GENERATED_PACKAGE}")
        appendLine()
        listOf(
            "com.pandulapeter.kubriko.Kubriko",
            "com.pandulapeter.kubriko.actor.Actor",
            "com.pandulapeter.kubriko.logger.Logger",
            "com.pandulapeter.kubriko.manager.ActorManager",
            "com.pandulapeter.kubriko.manager.Manager",
            "com.pandulapeter.kubriko.manager.ViewportManager",
            "com.pandulapeter.kubriko.sceneEditor.Editable",
            "com.pandulapeter.kubriko.sceneEditor.EditableMetadata",
            "com.pandulapeter.kubriko.serialization.SerializationManager",
            "com.pandulapeter.kubriko.types.SceneUnit",
            "kotlin.concurrent.Volatile",
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
            |    val isInSceneEditor: Boolean = false,
            |    private val managerActors: List<Actor> = emptyList(),
            |) : Manager() {
            |    private val actorManager by manager<ActorManager>()
            |    private val serializationManager by manager<SerializationManager<EditableMetadata<*>, Editable<*>>>()
            |    private val viewportManager by manager<ViewportManager>()
            |    private var loading: Job? = null
            |
            |    /**
            |     * The name of the Scene shown (or being loaded), or null before the first one.
            |     */
            |    var currentScene: String? = null
            |        private set
            |
            |    /**
            |     * The settings of the Scene shown, or null if it has none (or while it is being loaded).
            |     */
            |    @Volatile
            |    var settings: ${SceneSettingsCodeGenerator.SETTINGS_NAME}? = null
            |
            |    /**
            |     * The left edge of the shown Scene's bounds, or of the screen while it has none.
            |     */
            |    val $LEFT_EDGE: SceneUnit get() = settings.let { if (it == null) viewportManager.topLeft.value.x else it.topLeft.x }
            |
            |    /**
            |     * The right edge of the shown Scene's bounds, or of the screen while it has none.
            |     */
            |    val $RIGHT_EDGE: SceneUnit get() = settings.let { if (it == null) viewportManager.bottomRight.value.x else it.bottomRight.x }
            |
            |    /**
            |     * The top edge of the shown Scene's bounds, or of the screen while it has none.
            |     */
            |    val $TOP_EDGE: SceneUnit get() = settings.let { if (it == null) viewportManager.topLeft.value.y else it.topLeft.y }
            |
            |    /**
            |     * The bottom edge of the shown Scene's bounds, or of the screen while it has none.
            |     */
            |    val $BOTTOM_EDGE: SceneUnit get() = settings.let { if (it == null) viewportManager.bottomRight.value.y else it.bottomRight.y }
            |
            |    override fun onInitialize(kubriko: Kubriko) {
            |        if (!isInSceneEditor && firstScene != null) load(firstScene, isFirst = true)
            |    }
            |
            |    /**
            |     * Removes every Actor and adds the Actors of [scene] instead, once its file is read. Asking for the Scene that is
            |     * being loaded again (from an Event that runs every frame, for example) doesn't start it over.
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
            |        if (scene == currentScene && loading?.isActive == true) return
            |        currentScene = scene
            |        loading?.cancel()
            |        loading = scope.launch {
            |            val actors = try {
            |                serializationManager.deserializeActors(Res.readBytes("$SCENE_DIRECTORY/${'$'}scene.json").decodeToString())
            |            } catch (exception: CancellationException) {
            |                throw exception
            |            } catch (exception: Exception) {
            |                Logger.log(message = "The Scene ${'$'}scene couldn't be loaded.", details = exception.message, source = "$SCENES_NAME")
            |                println("[$SCENES_NAME] The Scene ${'$'}scene couldn't be loaded. ${'$'}{exception.message}")
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
