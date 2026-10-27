/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.creatorSession.implementation

import com.pandulapeter.kubriko.creator.CreatorEntryPoint
import com.pandulapeter.kubriko.creatorSession.SessionFailure
import com.pandulapeter.kubriko.sceneEditor.SceneEditor
import java.util.ServiceConfigurationError
import java.util.ServiceLoader

/**
 * Opens the Scene editor with the Actor types of the project's [CreatorEntryPoint].
 */
internal object EditSession {

    fun run(arguments: SessionArguments.Edit) {
        val entryPoint = findEntryPoint()
        SceneEditor.show(
            defaultSceneFilename = arguments.sceneFileName,
            defaultSceneFolderPath = arguments.sceneFolderPath,
            serializationManager = entryPoint.createSerializationManager(),
            customManagers = entryPoint.createCustomManagers(),
            title = arguments.sceneFileName ?: "Kubriko Creator",
        )
    }

    private fun findEntryPoint() = selectEntryPoint(
        try {
            ServiceLoader.load(CreatorEntryPoint::class.java).toList()
        } catch (error: ServiceConfigurationError) {
            throw SessionFailure("The project's CreatorEntryPoint could not be loaded: ${error.message}", error)
        },
    )

    internal fun selectEntryPoint(entryPoints: List<CreatorEntryPoint>): CreatorEntryPoint =
        when (entryPoints.size) {
            0 -> throw SessionFailure("The project has no CreatorEntryPoint, so its Scenes can't be edited.")
            1 -> entryPoints.single()
            else -> throw SessionFailure("The project registers more than one CreatorEntryPoint: ${entryPoints.joinToString { it::class.java.name }}.")
        }
}
