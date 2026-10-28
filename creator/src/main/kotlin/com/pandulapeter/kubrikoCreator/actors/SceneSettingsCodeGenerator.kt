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

import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.colorLiteral
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.unitLiteral

/**
 * The Actor that holds a Scene's settings: its bounds (the Actor's body), background color, where the camera
 * starts, and its music. Generated into every project and placed like any other Actor, at most one per Scene.
 */
internal object SceneSettingsCodeGenerator {

    const val SETTINGS_NAME = "SceneSettings"
    const val BACKGROUND_COLOR_KEY = "backgroundColor"
    const val CAMERA_START_KEY = "cameraStart"
    const val MUSIC_KEY = "music"

    /**
     * The background of a Scene whose settings don't name one.
     */
    const val DEFAULT_BACKGROUND_COLOR = 0xFF202124L

    /**
     * The size of newly placed Scene settings, in scene units.
     */
    const val DEFAULT_WIDTH = 1280f
    const val DEFAULT_HEIGHT = 720f

    fun generate(packageName: String) = GeneratedFile.withHeader("", buildString {
        appendLine("package $packageName.${ActorTypeCodeGenerator.GENERATED_PACKAGE}")
        appendLine()
        listOf(
            "androidx.compose.ui.graphics.Color",
            "androidx.compose.ui.graphics.drawscope.DrawScope",
            "com.pandulapeter.kubriko.Kubriko",
            "com.pandulapeter.kubriko.actor.body.BoxBody",
            "com.pandulapeter.kubriko.actor.traits.Unique",
            "com.pandulapeter.kubriko.actor.traits.Visible",
            "com.pandulapeter.kubriko.helpers.extensions.get",
            "com.pandulapeter.kubriko.helpers.extensions.sceneUnit",
            "com.pandulapeter.kubriko.manager.ViewportManager",
            "com.pandulapeter.kubriko.sceneEditor.Editable",
            "com.pandulapeter.kubriko.sceneEditor.EditableMetadata",
            "com.pandulapeter.kubriko.sceneEditor.Exposed",
            "com.pandulapeter.kubriko.serialization.Serializable",
            "com.pandulapeter.kubriko.serialization.typeSerializers.SerializableBoxBody",
            "com.pandulapeter.kubriko.serialization.typeSerializers.SerializableColor",
            "com.pandulapeter.kubriko.serialization.typeSerializers.SerializableSceneOffset",
            "com.pandulapeter.kubriko.types.SceneOffset",
            "com.pandulapeter.kubriko.types.SceneSize",
            "kotlinx.serialization.SerialName",
            "kotlinx.serialization.json.Json",
        ).forEach { appendLine("import $it") }
        appendLine()
        appendLine(
            """
            |/**
            | * The settings of a Scene. Its body is the Scene's bounds, which edge Events use instead of the edges of the screen,
            | * and which it fills with the background color, behind every other Actor. When the Scene starts, the camera moves
            | * to [$CAMERA_START_KEY], and its [$MUSIC_KEY] plays, if it has some and the game plays audio.
            | */
            |class $SETTINGS_NAME private constructor(state: State) : Visible, Unique, Editable<$SETTINGS_NAME> {
            |    override val body = state.body
            |    override val drawingOrder = Float.MAX_VALUE
            |
            |    @set:Exposed(name = "$BACKGROUND_COLOR_KEY")
            |    var $BACKGROUND_COLOR_KEY: Color = state.$BACKGROUND_COLOR_KEY
            |
            |    @set:Exposed(name = "$CAMERA_START_KEY")
            |    var $CAMERA_START_KEY: SceneOffset = state.$CAMERA_START_KEY
            |
            |    /**
            |     * The music file, by its path from `composeResources` (like `files/music/theme.mp3`), or empty for none.
            |     */
            |    @set:Exposed(name = "$MUSIC_KEY")
            |    var $MUSIC_KEY: String = state.$MUSIC_KEY
            |
            |    private lateinit var generatedScenes: ${ScenesCodeGenerator.SCENES_NAME}
            |
            |    /**
            |     * The top left corner of the Scene's bounds.
            |     */
            |    val topLeft get() = body.axisAlignedBoundingBox.min
            |
            |    /**
            |     * The bottom right corner of the Scene's bounds.
            |     */
            |    val bottomRight get() = body.axisAlignedBoundingBox.max
            |
            |    override fun onAdded(kubriko: Kubriko) {
            |        generatedScenes = kubriko.get()
            |        generatedScenes.settings = this
            |        if (!generatedScenes.isInSceneEditor) kubriko.get<ViewportManager>().setCameraPosition($CAMERA_START_KEY)
            |    }
            |
            |    override fun onRemoved() {
            |        if (generatedScenes.settings === this) generatedScenes.settings = null
            |    }
            |
            |    override fun DrawScope.draw() = drawRect(
            |        color = $BACKGROUND_COLOR_KEY,
            |        size = body.size.raw,
            |    )
            |
            |    override fun save() = State(
            |        body = body,
            |        $BACKGROUND_COLOR_KEY = $BACKGROUND_COLOR_KEY,
            |        $CAMERA_START_KEY = $CAMERA_START_KEY,
            |        $MUSIC_KEY = $MUSIC_KEY,
            |    )
            |
            |    @kotlinx.serialization.Serializable
            |    data class State(
            |        @SerialName("body") val body: SerializableBoxBody = BoxBody(),
            |        @SerialName("$BACKGROUND_COLOR_KEY") val $BACKGROUND_COLOR_KEY: SerializableColor = Color(${colorLiteral(DEFAULT_BACKGROUND_COLOR)}),
            |        @SerialName("$CAMERA_START_KEY") val $CAMERA_START_KEY: SerializableSceneOffset = SceneOffset.Zero,
            |        @SerialName("$MUSIC_KEY") val $MUSIC_KEY: String = "",
            |    ) : Serializable.State<$SETTINGS_NAME> {
            |
            |        override fun restore() = $SETTINGS_NAME(this)
            |
            |        override fun serialize() = Json.encodeToString(this)
            |    }
            |
            |    companion object {
            |        val metadata = EditableMetadata.create<$SETTINGS_NAME, State>(typeId = "$SETTINGS_NAME") { position ->
            |            State(body = BoxBody(initialPosition = position, initialSize = SceneSize(${unitLiteral(DEFAULT_WIDTH)}, ${unitLiteral(DEFAULT_HEIGHT)})))
            |        }
            |    }
            |}
            """.trimMargin(),
        )
    })
}
