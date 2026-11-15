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
import com.pandulapeter.kubrikoCreator.actors.codegen.ActorCountCode.appendScenesMembers

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

    /**
     * The field of the generated class counting Scene changes, which Actors compare with what it was when they were added.
     */
    internal const val SCENE_CHANGES_FIELD = "sceneChanges"

    private const val LEFT = "LEFT"
    private const val RIGHT = "RIGHT"
    private const val TOP = "TOP"
    private const val BOTTOM = "BOTTOM"

    /**
     * The constant of the generated class naming [edge], which its edge functions take.
     */
    internal fun edgeConstantOf(edge: SceneEdge) = "$SCENES_NAME." + when (edge) {
        SceneEdge.LEFT -> LEFT
        SceneEdge.RIGHT -> RIGHT
        SceneEdge.TOP -> TOP
        SceneEdge.BOTTOM -> BOTTOM
    }

    /**
     * Where the edges are, read from the settings once per call, so a Scene that finishes loading meanwhile can't mix its
     * bounds with the screen's.
     */
    private fun edgeMembers() = """
            |
            |    /**
            |     * Where an edge of the shown Scene's bounds is, or of the screen's while it has none: [$LEFT], [$RIGHT], [$TOP], or [$BOTTOM].
            |     */
            |    fun edge(edge: Int): Float = edgeOf(settings, edge)
            |
            |    /**
            |     * A random number between two edges, each plus its offset, of the same bounds even while a Scene is being loaded.
            |     */
            |    fun randomBetweenEdges(from: Int, fromOffset: Float, to: Int, toOffset: Float): Float {
            |        val settings = settings
            |        val start = edgeOf(settings, from) + fromOffset
            |        return start + KubrikoWiring.random.nextFloat() * (edgeOf(settings, to) + toOffset - start)
            |    }
            |
            |    private fun edgeOf(settings: ${SceneSettingsCodeGenerator.SETTINGS_NAME}?, edge: Int): Float = when (edge) {
            |        $LEFT -> if (settings == null) viewportManager.topLeft.value.x.raw else settings.topLeft.x.raw
            |        $RIGHT -> if (settings == null) viewportManager.bottomRight.value.x.raw else settings.bottomRight.x.raw
            |        $TOP -> if (settings == null) viewportManager.topLeft.value.y.raw else settings.topLeft.y.raw
            |        else -> if (settings == null) viewportManager.bottomRight.value.y.raw else settings.bottomRight.y.raw
            |    }
    """.trimMargin()

    /**
     * How a Scene fits the game's view to its bounds while it plays ([SceneSettingsCodeGenerator.FITS_VIEW_KEY]). Its
     * settings may be added before the last Scene's are removed, so the aspect ratio from before any fit is kept here.
     */
    private fun viewFitMembers() = """
            |
            |    /**
            |     * The aspect ratio the game's view had before a Scene fitted it to its bounds, or null while none does.
            |     */
            |    private var aspectRatioModeBeforeFit: ViewportManager.AspectRatioMode? = null
            |
            |    /**
            |     * Makes the game's view show a Scene's [bounds], as wide as they are, centered on them, keeping the aspect ratio it
            |     * had before the first Scene that fitted it, or does nothing and returns false if the bounds have no area.
            |     */
            |    internal fun fitView(bounds: com.pandulapeter.kubriko.actor.body.AxisAlignedBoundingBox): Boolean {
            |        val width = bounds.maxXRaw - bounds.minXRaw
            |        val height = bounds.maxYRaw - bounds.minYRaw
            |        if (!(width > 0f && height > 0f)) return false
            |        if (aspectRatioModeBeforeFit == null) aspectRatioModeBeforeFit = viewportManager.aspectRatioMode.value
            |        viewportManager.setAspectRatioMode(ViewportManager.AspectRatioMode.Fixed(ratio = width / height, width = width.sceneUnit))
            |        viewportManager.setCameraPosition(SceneOffset((bounds.minXRaw + width / 2f).sceneUnit, (bounds.minYRaw + height / 2f).sceneUnit))
            |        return true
            |    }
            |
            |    /**
            |     * Gives the game's view back the aspect ratio it had before a Scene fitted it, if one did.
            |     */
            |    internal fun unfitView() {
            |        aspectRatioModeBeforeFit?.let(viewportManager::setAspectRatioMode)
            |        aspectRatioModeBeforeFit = null
            |    }
    """.trimMargin()

    private fun importsOf(packageName: String) = listOf(
        "com.pandulapeter.kubriko.Kubriko",
        "com.pandulapeter.kubriko.actor.Actor",
        "com.pandulapeter.kubriko.helpers.extensions.sceneUnit",
        "com.pandulapeter.kubriko.logger.Logger",
        "com.pandulapeter.kubriko.manager.ActorManager",
        "com.pandulapeter.kubriko.manager.Manager",
        "com.pandulapeter.kubriko.manager.ViewportManager",
        "com.pandulapeter.kubriko.sceneEditor.Editable",
        "com.pandulapeter.kubriko.sceneEditor.EditableMetadata",
        "com.pandulapeter.kubriko.serialization.SerializationManager",
        "com.pandulapeter.kubriko.types.SceneOffset",
        "kotlin.concurrent.Volatile",
        "kotlinx.coroutines.CancellationException",
        "kotlinx.coroutines.Job",
        "kotlinx.coroutines.launch",
        "$packageName.resources.Res",
    )

    /**
     * @param countedTypes The Actor types the actorCount conditions count ([ActorCountCode]), whose counts it keeps.
     */
    internal fun generate(packageName: String, countedTypes: List<String> = emptyList()) = GeneratedFile.withHeader("", buildString {
        appendLine("package $packageName.${ActorTypeCodeGenerator.GENERATED_PACKAGE}")
        appendLine()
        importsOf(packageName).forEach { appendLine("import $it") }
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
            |${if (countedTypes.isEmpty()) "" else ActorCountCode.OPT_IN + "\n"}class $SCENES_NAME(
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
            |     * How many times every Actor was removed for another Scene: an Actor that saw fewer when it was added leaves with
            |     * its Scene, rather than being removed while the Scene plays.
            |     */
            |    @Volatile
            |    var $SCENE_CHANGES_FIELD = 0
            |        private set
            """.trimMargin(),
        )
        appendLine(edgeMembers())
        appendLine(viewFitMembers())
        if (countedTypes.isNotEmpty()) appendScenesMembers(countedTypes)
        appendLine(
            """
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
            |                $SCENE_CHANGES_FIELD++
            |                actorManager.removeAll()
            |                actorManager.add(managerActors)
            |            }
            |            actorManager.add(actors)
            |        }
            |    }
            |
            |    companion object {
            |        const val $LEFT = 0
            |        const val $RIGHT = 1
            |        const val $TOP = 2
            |        const val $BOTTOM = 3
            """.trimMargin(),
        )
        ActorCountCode.constantsOf(countedTypes).forEach(::appendLine)
        appendLine("    }")
        appendLine("}")
    })
}
