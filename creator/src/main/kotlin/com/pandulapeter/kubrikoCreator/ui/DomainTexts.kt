/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import androidx.compose.runtime.Composable
import com.pandulapeter.kubrikoCreator.actors.NameProblem
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.PlacementProblem
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.foundation.DomainText
import com.pandulapeter.kubrikoCreator.git.GitStatus
import com.pandulapeter.kubrikoCreator.project.GameSettingsProblem
import com.pandulapeter.kubrikoCreator.project.IdentityProblem
import com.pandulapeter.kubrikoCreator.project.ProjectProblem
import com.pandulapeter.kubrikoCreator.sceneRuntime.SceneProblem
import com.pandulapeter.kubrikoCreator.sceneRuntime.SceneStatus
import com.pandulapeter.kubrikoCreator.screenplay.ScreenplayStyle
import com.pandulapeter.kubrikoCreator.template.TargetProblem
import com.pandulapeter.kubrikoCreator.template.TemplateProblem
import com.pandulapeter.kubrikoCreator.ui.components.FieldError
import com.pandulapeter.kubrikoCreator.ui.texts.toResource
import com.pandulapeter.kubrikoCreator.workspace.WorkspaceStatus
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The string the user is shown for this text; each enum's own mapping (in `ui/texts`) names every entry, so a new entry
 * without a string doesn't compile.
 */
internal val DomainText.resource: StringResource
    get() = when (this) {
        is NameProblem -> toResource()
        is IdentityProblem -> toResource()
        is GameSettingsProblem -> toResource()
        is ProjectProblem -> toResource()
        is TemplateProblem -> toResource()
        is TargetProblem -> toResource()
        is SceneProblem -> toResource()
        is SceneStatus -> toResource()
        is GitStatus -> toResource()
        is CommandProblem -> toResource()
        is PlacementProblem -> toResource()
        is WorkspaceStatus -> toResource()
        is ScreenplayStyle.Rule -> toResource()
        else -> error("No string for ${this::class.simpleName}.$this")
    }

/**
 * This text as the user is shown it, with [arguments] put into its placeholders.
 */
@Composable
internal fun DomainText.text(arguments: List<String> = emptyList()) = stringResource(resource, *arguments.toTypedArray())

/**
 * This text as a field shows it under what was typed, with [arguments] put into its placeholders.
 */
internal fun DomainText.toFieldError(arguments: List<String> = emptyList()) = FieldError(resource, arguments)

/**
 * Why the command was rejected, as a field shows it under what was typed.
 */
internal fun Rejection.toFieldError() = reason.toFieldError(arguments)
