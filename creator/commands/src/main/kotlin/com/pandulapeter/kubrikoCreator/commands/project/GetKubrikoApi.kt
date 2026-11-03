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

import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.ReadCommand
import com.pandulapeter.kubrikoCreator.context.KubrikoApi
import com.pandulapeter.kubrikoCreator.context.KubrikoSources
import com.pandulapeter.kubrikoCreator.foundation.CreatorConfiguration
import com.pandulapeter.kubrikoCreator.foundation.settingsDirectory
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import java.io.File

public object GetKubrikoApi : ReadCommand<GetKubrikoApi.Input> {
    override val name: String = "get_kubriko_api"
    override val description: String = "Reads Kubriko's API, for writing Kotlin Blocks and Kotlin UI: without className, an overview of the engine (its " +
        "instance, Managers, Actors and their Traits, scene units) and its plugins; with one, the source file declaring that class, interface, or " +
        "object, with its documentation, at the project's Kubriko version."
    override val inputSerializer: KSerializer<Input> = Input.serializer()
    override val readsProject: Boolean = false
    private val sources by lazy { KubrikoSources(CreatorConfiguration.load().kubrikoVersion, File(settingsDirectory(), "tools/kubriko-sources")) }

    override suspend fun read(context: CommandContext, input: Input): CommandOutcome {
        val className = input.className?.trim()?.substringAfterLast('.')?.takeIf { it.isNotEmpty() } ?: return CommandOutcome.Success(JsonPrimitive(KubrikoApi.overview))
        val found = withContext(context.ioDispatcher) { sources.find(className) }
        return CommandOutcome.Success(
            JsonPrimitive(
                found?.let { "${it.artifact.artifactId}: ${it.path}\n\n" + it.text.take(MAXIMUM_LENGTH) + if (it.text.length > MAXIMUM_LENGTH) "\n…" else "" }
                    ?: "No class, interface, or object called $className was found in Kubriko's sources.",
            ),
        )
    }

    private const val MAXIMUM_LENGTH = 16_000

    @Serializable
    public data class Input(
        @Description("The simple name of a Kubriko class, interface, or object, like ActorManager or BoxBody, or none for the overview.")
        @SerialName("className") val className: String? = null,
    )
}
