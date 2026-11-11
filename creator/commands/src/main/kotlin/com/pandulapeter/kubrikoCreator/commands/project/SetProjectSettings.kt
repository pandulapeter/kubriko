/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.project

import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Descriptions
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.commands.scenes.missingScene
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.project.AspectRatio
import com.pandulapeter.kubrikoCreator.project.GameIdentity
import com.pandulapeter.kubrikoCreator.project.GameSettings
import com.pandulapeter.kubrikoCreator.project.GradlePropertiesFile
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Changes the game's identity and settings, which live in `gradle.properties` where the build reads them (see
 * [GameIdentity] and [GameSettings]), in one write. The file is read only when applying, off the main thread, so
 * validating checks the input and the Scenes alone: `check_change` accepts a value that applying then refuses (an
 * invalid application ID, or a project whose template is too old for a setting).
 */
@OptIn(EditorMutation::class)
public object SetProjectSettings : WriteCommand<SetProjectSettings.Input> {
    override val name: String = "set_project_settings"
    override val description: String = "Changes the settings get_project_settings reads (the name is the window, web page, and installers' title). " +
        "Settings left out stay as they are."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget? = CommandTarget.Project

    override fun validate(context: CommandContext, input: Input): Rejection? = when {
        input == Input() -> Rejection(CommandProblem.PROJECT_SETTINGS_EMPTY)
        input.firstScene != null && !context.scenes.exists(input.firstScene) -> context.missingScene(input.firstScene)
        else -> null
    }

    /**
     * The first Scene undo brings back may have been deleted since, which the game survives (it logs that it can't load
     * it), so restoring it is allowed.
     */
    override fun validateRestoring(context: CommandContext, input: Input): Rejection? =
        if (input == Input()) Rejection(CommandProblem.PROJECT_SETTINGS_EMPTY) else null

    override suspend fun apply(context: CommandContext, input: Input): Applied = withContext(context.ioDispatcher) {
        val root = context.project.rootDirectory
        val identity = if (input.namesIdentity()) GameIdentity.read(root) ?: return@withContext Applied.failed(CommandProblem.PROJECT_SETTINGS_UNAVAILABLE) else null
        val reading = if (input.namesGame()) GameSettings.read(root) ?: return@withContext Applied.failed(CommandProblem.GAME_SETTINGS_UNAVAILABLE) else null
        val game = reading?.settings
        val changedIdentity = identity?.let(input::appliedTo)
        val changedGame = game?.let(input::appliedTo)
        changedIdentity?.validate()?.let { (reason, arguments) -> return@withContext Applied.failed(Rejection(reason, arguments)) }
        changedGame?.validate()?.let { (reason, arguments) -> return@withContext Applied.failed(Rejection(reason, arguments)) }
        val targets = context.buildFiles.targets
        val applicationId = changedIdentity?.applicationId?.takeIf { it != identity.applicationId }
        applicationId?.let { targets?.refusalOfApplicationId(it) }?.let { return@withContext Applied.failed(Rejection(it.problem, it.arguments)) }
        GradlePropertiesFile.update(root) { text ->
            val withIdentity = changedIdentity?.writtenTo(text) ?: text
            val sizeKeys = input.aspectRatio?.let { reading?.unreadableSizeKeysOf(it) }.orEmpty()
            changedGame?.writtenTo(withIdentity, input.gameKeys() + sizeKeys) ?: withIdentity
        }
        applicationId?.let { targets?.writeApplicationId(it) }
        Applied(inverse = Invocation(SetProjectSettings, input.revertedTo(identity, game)))
    }

    override fun mergeKey(input: Input): Any? = "project-settings:" + input.namedSettings().joinToString()

    @Serializable
    public data class Input(
        @Description("The game's name.")
        @SerialName("name") val name: String? = null,
        @Description("The version players see, like 1.2.0.")
        @SerialName("versionName") val versionName: String? = null,
        @Description("The version number stores compare, from 1.")
        @SerialName("versionNumber") val versionNumber: Int? = null,
        @Description("The application ID, like com.example.mygame.")
        @SerialName("applicationId") val applicationId: String? = null,
        @Description("Who makes the game, empty for none.")
        @SerialName("vendor") val vendor: String? = null,
        @Description("One line about the game, empty for none.")
        @SerialName("description") val description: String? = null,
        @Description("The Scene the game starts with. " + Descriptions.SCENE_NAME)
        @SerialName("firstScene") val firstScene: String? = null,
        @Description(
            "How the viewport fits the window: dynamic shows as many scene units as the window has pixels; fitHorizontal always " +
                "shows viewportWidth across; fitVertical viewportHeight down; fixed exactly viewportWidth by viewportHeight with bars " +
                "around it; stretched the same, stretched to the window.",
        )
        @SerialName("aspectRatio") val aspectRatio: AspectRatio? = null,
        @Description("The viewport's width in scene units, from 1, for fitHorizontal, fixed, and stretched.")
        @SerialName("viewportWidth") val viewportWidth: Float? = null,
        @Description("The viewport's height in scene units, from 1, for fitVertical, fixed, and stretched.")
        @SerialName("viewportHeight") val viewportHeight: Float? = null,
        @Description("The updates per second the game is limited to, from 1, or 0 for every frame the display shows.")
        @SerialName("frameRate") val frameRate: Int? = null,
    ) {
        public fun appliedTo(identity: GameIdentity): GameIdentity = identity.copy(
            name = name ?: identity.name,
            versionName = versionName ?: identity.versionName,
            versionNumber = versionNumber ?: identity.versionNumber,
            applicationId = applicationId ?: identity.applicationId,
            vendor = vendor ?: identity.vendor,
            description = description ?: identity.description,
        )

        public fun appliedTo(settings: GameSettings): GameSettings = settings.copy(
            firstScene = firstScene ?: settings.firstScene,
            aspectRatio = aspectRatio ?: settings.aspectRatio,
            viewportWidth = viewportWidth ?: settings.viewportWidth,
            viewportHeight = viewportHeight ?: settings.viewportHeight,
            frameRate = frameRate ?: settings.frameRate,
        )

        /**
         * What sets the settings this changes back to their values in [identity] and [settings].
         */
        internal fun revertedTo(identity: GameIdentity?, settings: GameSettings?) = Input(
            name = name?.let { identity?.name },
            versionName = versionName?.let { identity?.versionName },
            versionNumber = versionNumber?.let { identity?.versionNumber },
            applicationId = applicationId?.let { identity?.applicationId },
            vendor = vendor?.let { identity?.vendor },
            description = description?.let { identity?.description },
            firstScene = firstScene?.let { settings?.firstScene },
            aspectRatio = aspectRatio?.let { settings?.aspectRatio },
            viewportWidth = viewportWidth?.let { settings?.viewportWidth },
            viewportHeight = viewportHeight?.let { settings?.viewportHeight },
            frameRate = frameRate?.let { settings?.frameRate },
        )

        internal fun namesIdentity() = listOf(name, versionName, versionNumber, applicationId, vendor, description).any { it != null }

        internal fun namesGame() = gameKeys().isNotEmpty()

        /**
         * The `gradle.properties` keys of the game's settings this changes.
         */
        internal fun gameKeys() = listOfNotNull(
            firstScene?.let { GameSettings.FIRST_SCENE },
            aspectRatio?.let { GameSettings.ASPECT_RATIO },
            viewportWidth?.let { GameSettings.VIEWPORT_WIDTH },
            viewportHeight?.let { GameSettings.VIEWPORT_HEIGHT },
            frameRate?.let { GameSettings.FRAME_RATE },
        )

        internal fun namedSettings() = listOfNotNull(
            name?.let { "name" },
            versionName?.let { "versionName" },
            versionNumber?.let { "versionNumber" },
            applicationId?.let { "applicationId" },
            vendor?.let { "vendor" },
            description?.let { "description" },
        ) + gameKeys()
    }
}
