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

import com.pandulapeter.kubrikoCreator.actors.codegen.PluginReason
import com.pandulapeter.kubrikoCreator.actors.codegen.PluginUse
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.NoInput
import com.pandulapeter.kubrikoCreator.commands.ReadCommand
import com.pandulapeter.kubrikoCreator.project.GameIdentity
import com.pandulapeter.kubrikoCreator.project.GameSettingsReading
import com.pandulapeter.kubrikoCreator.project.LibraryInUse
import com.pandulapeter.kubrikoCreator.project.ProjectSettingsSnapshot
import com.pandulapeter.kubrikoCreator.template.TargetState
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

public object GetProjectSettings : ReadCommand<NoInput> {
    override val name: String = "get_project_settings"
    override val description: String = "Reads the game's identity (name, version name and number, application ID, vendor, and description), how it runs " +
        "(first Scene, aspect ratio, viewport size, and frame rate), the targets it is built for (see get_target_change), and the Kubriko libraries its build uses, with what uses each. A group " +
        "whose template is too old to have it is null (see get_project_upgrade)."
    override val inputSerializer: KSerializer<NoInput> = NoInput.serializer()

    override fun target(context: CommandContext, input: NoInput): CommandTarget? = CommandTarget.Project

    override suspend fun read(context: CommandContext, input: NoInput): CommandOutcome {
        val definitions = context.blockTypes?.types?.value.orEmpty()
        val (snapshot, targets) = withContext(context.ioDispatcher) {
            ProjectSettingsSnapshot.read(context.project.rootDirectory, definitions, context.scenes) to context.buildFiles.targets?.read()
        }
        return CommandOutcome.Success(
            buildJsonObject {
                put("identity", snapshot.identity?.let(::identityJson) ?: JsonNull)
                put("game", snapshot.game?.let(::gameJson) ?: JsonNull)
                put("targets", targets?.let(::targetsJson) ?: JsonNull)
                putJsonArray("libraries") { snapshot.libraries.forEach { add(libraryJson(it)) } }
            },
        )
    }

    private fun identityJson(identity: GameIdentity) = buildJsonObject {
        put("name", identity.name)
        put("versionName", identity.versionName)
        put("versionNumber", identity.versionNumber)
        put("applicationId", identity.applicationId)
        put("vendor", identity.vendor)
        put("description", identity.description)
    }

    private fun gameJson(reading: GameSettingsReading) = buildJsonObject {
        val settings = reading.settings
        put("firstScene", settings.firstScene)
        put("aspectRatio", settings.aspectRatio.key)
        if (settings.aspectRatio.usesWidth) put("viewportWidth", settings.viewportWidth)
        if (settings.aspectRatio.usesHeight) put("viewportHeight", settings.viewportHeight)
        put("frameRate", settings.frameRate)
        if (reading.invalidKeys.isNotEmpty()) {
            putJsonArray("unreadable") { reading.invalidKeys.forEach { add(it.removePrefix(GAME_KEY_PREFIX)) } }
        }
    }

    private fun targetsJson(targets: List<TargetState>) = buildJsonArray {
        targets.forEach { state ->
            add(
                buildJsonObject {
                    put("target", state.target.key)
                    put("isOn", state.isOn)
                    state.problem?.let { put("note", it.english) }
                },
            )
        }
    }

    private fun libraryJson(library: LibraryInUse): JsonObject = buildJsonObject {
        put("library", library.artifact.artifactId)
        when {
            library.isAlwaysUsed -> put("usedBy", "every game")
            else -> {
                if (library.uses.isNotEmpty()) putJsonArray("usedBy") { library.uses.forEach { add(useText(it)) } }
                if (library.handWrittenFiles.isNotEmpty()) putJsonArray("files") { library.handWrittenFiles.forEach { add(it) } }
                if (library.isUsedByGeneratedCode && library.uses.isEmpty()) put("generatedCode", true)
            }
        }
    }

    /**
     * What [use] is, as the AI reads it.
     */
    internal fun useText(use: PluginUse) = when (use.reason) {
        PluginReason.COLLISION_EVENT -> "${use.owner}: collision with ${use.subject}"
        PluginReason.KEY_PRESSED_EVENT -> "${use.owner}: key pressed ${use.subject}"
        PluginReason.KEY_HELD_CONDITION -> "${use.owner}: if key held ${use.subject}"
        PluginReason.PLAYS_SOUND -> "${use.owner}: plays ${use.subject}"
        PluginReason.PLAYS_MUSIC -> "${use.owner}: plays music ${use.subject}"
        PluginReason.STOPS_MUSIC -> "${use.owner}: stops the music"
        PluginReason.SCENE_MUSIC -> "Scene ${use.owner}: music ${use.subject}"
        PluginReason.SPRITE -> "${use.owner}: drawn as ${use.subject}"
    }

    private const val GAME_KEY_PREFIX = "game."
}
