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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `.kubriko/project.json`: what Kubriko Creator needs to know to generate code into a project that opted in to
 * block-built Actor types.
 *
 * @param packageName The package of the project's code; generated classes go into its `generated` subpackage.
 * @param sourceDirectory The Kotlin source folder generated classes are written to, relative to the project root.
 * @param templateVersion The version of the template files (`Game.kt` and its neighbors) the project has, which Kubriko
 *   Creator offers to bring up to date when it needs something newer (`template/ProjectUpgrader`). Projects from before
 *   it was recorded have version 1.
 */
@Serializable
internal data class CreatorProjectSettings(
    @SerialName("packageName") val packageName: String,
    @SerialName("sourceDirectory") val sourceDirectory: String = "src/commonMain/kotlin",
    @SerialName("templateVersion") val templateVersion: Int = 1,
)
