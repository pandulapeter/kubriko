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
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.project.GameIdentity
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_set_project_settings
import kubriko.creator.generated.resources.error_command_project_settings_empty
import kubriko.creator.generated.resources.error_command_project_settings_unavailable

/**
 * Changes the game's identity, which lives in `gradle.properties` where the build reads it (see [GameIdentity]).
 */
@OptIn(EditorMutation::class)
internal object SetProjectSettings : WriteCommand<SetProjectSettings.Input> {
    override val name = "set_project_settings"
    override val description = "Changes the settings get_project_settings reads (the name is the window, web page, and installers' title). " +
        "Settings left out stay as they are."
    override val title = Res.string.command_set_project_settings
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.Project

    override fun validate(context: CommandContext, input: Input): Rejection? {
        if (input == Input()) return Rejection(Res.string.error_command_project_settings_empty)
        val identity = GameIdentity.read(context.project.rootDirectory) ?: return Rejection(Res.string.error_command_project_settings_unavailable)
        return input.appliedTo(identity).validate()?.let { (reason, arguments) -> Rejection(reason, arguments) }
    }

    override suspend fun apply(context: CommandContext, input: Input): Applied = withContext(context.ioDispatcher) {
        val root = context.project.rootDirectory
        val previous = checkNotNull(GameIdentity.read(root))
        GameIdentity.write(root, input.appliedTo(previous))
        Applied(inverse = Invocation(SetProjectSettings, input.revertedTo(previous)))
    }

    override fun mergeKey(input: Input) = "project-settings:" + input.namedSettings().joinToString()

    @Serializable
    data class Input(
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
    ) {
        fun appliedTo(identity: GameIdentity) = identity.copy(
            name = name ?: identity.name,
            versionName = versionName ?: identity.versionName,
            versionNumber = versionNumber ?: identity.versionNumber,
            applicationId = applicationId ?: identity.applicationId,
            vendor = vendor ?: identity.vendor,
            description = description ?: identity.description,
        )

        /**
         * What sets the settings this changes back to their values in [identity].
         */
        fun revertedTo(identity: GameIdentity) = Input(
            name = name?.let { identity.name },
            versionName = versionName?.let { identity.versionName },
            versionNumber = versionNumber?.let { identity.versionNumber },
            applicationId = applicationId?.let { identity.applicationId },
            vendor = vendor?.let { identity.vendor },
            description = description?.let { identity.description },
        )

        fun namedSettings() = listOfNotNull(
            name?.let { "name" },
            versionName?.let { "versionName" },
            versionNumber?.let { "versionNumber" },
            applicationId?.let { "applicationId" },
            vendor?.let { "vendor" },
            description?.let { "description" },
        )
    }
}
