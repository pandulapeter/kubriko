/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.scenes

import com.pandulapeter.kubrikoCreator.actors.HexColorSerializer
import com.pandulapeter.kubrikoCreator.actors.SceneSettingsCodeGenerator
import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Description
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.scenes.SceneActorEntry
import com.pandulapeter.kubrikoCreator.scenes.SceneSettingsState
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_set_scene_settings
import kubriko.creator.generated.resources.error_command_number_invalid
import kubriko.creator.generated.resources.error_command_size_invalid

internal object SetSceneSettings : WriteCommand<SetSceneSettings.Input> {
    override val name = "set_scene_settings"
    override val description = "Changes the settings of a Scene, kept by its ${SceneSettingsCodeGenerator.SETTINGS_NAME} Actor, which this adds " +
        "at the end of the Scene's Actor list if it has none. Its bounds are where edge Events happen (instead of the edges of the " +
        "screen) and are filled with the background color; the camera starts at the camera start when the Scene does. Positions " +
        "are in scene units, with y growing downwards. Values left out stay as they are."
    override val title = Res.string.command_set_scene_settings
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.Scene(input.scene)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateScene(input.scene) ?: when {
        listOfNotNull(input.x, input.y, input.width, input.height, input.cameraX, input.cameraY).any { !it.isFinite() } -> Rejection(Res.string.error_command_number_invalid)
        listOfNotNull(input.width, input.height).any { it <= 0f } -> Rejection(Res.string.error_command_size_invalid)
        else -> null
    }

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val actors = context.readScene(input.scene)
        val index = SceneSettingsState.indexIn(actors)
        val previous = actors.getOrNull(index)?.state
        val state = SceneSettingsState.withChanges(
            state = previous ?: "{}",
            x = input.x,
            y = input.y,
            width = input.width,
            height = input.height,
            backgroundColor = input.backgroundColor,
            cameraX = input.cameraX,
            cameraY = input.cameraY,
        )
        val settings = SceneActorEntry(SceneSettingsCodeGenerator.SETTINGS_NAME, state)
        if (previous == null) {
            context.writeScene(input.scene, actors + settings)
            return Applied(inverse = Invocation(RemoveActor, RemoveActor.Input(input.scene, actors.size)))
        }
        context.writeScene(input.scene, actors.mapIndexed { actorIndex, actor -> if (actorIndex == index) settings else actor })
        val values = SceneSettingsState.read(previous)
        return Applied(
            inverse = Invocation(
                command = SetSceneSettings,
                input = Input(
                    scene = input.scene,
                    x = input.x?.let { values.x },
                    y = input.y?.let { values.y },
                    width = input.width?.let { values.width },
                    height = input.height?.let { values.height },
                    backgroundColor = input.backgroundColor?.let { values.backgroundColor },
                    cameraX = input.cameraX?.let { values.cameraX },
                    cameraY = input.cameraY?.let { values.cameraY },
                ),
            ),
        )
    }

    override fun mergeKey(input: Input) = listOf(
        input.scene, input.x != null, input.y != null, input.width != null, input.height != null, input.backgroundColor != null, input.cameraX != null, input.cameraY != null,
    )

    @Serializable
    data class Input(
        @Description("The name of the Scene: its file name without .json.")
        @SerialName("scene") val scene: String,
        @Description("The horizontal center of the Scene's bounds, in scene units.")
        @SerialName("x") val x: Float? = null,
        @Description("The vertical center of the Scene's bounds, in scene units, positive downwards.")
        @SerialName("y") val y: Float? = null,
        @Description("The width of the Scene's bounds, in scene units.")
        @SerialName("width") val width: Float? = null,
        @Description("The height of the Scene's bounds, in scene units.")
        @SerialName("height") val height: Float? = null,
        @Description("The background color, as #RRGGBB, or #AARRGGBB when it isn't opaque.")
        @SerialName("backgroundColor") @Serializable(with = HexColorSerializer::class) val backgroundColor: Long? = null,
        @Description("Where the camera's center starts horizontally, in scene units.")
        @SerialName("cameraX") val cameraX: Float? = null,
        @Description("Where the camera's center starts vertically, in scene units, positive downwards.")
        @SerialName("cameraY") val cameraY: Float? = null,
    )
}
