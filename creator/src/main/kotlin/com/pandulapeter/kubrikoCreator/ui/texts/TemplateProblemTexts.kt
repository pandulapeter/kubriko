/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.texts

import com.pandulapeter.kubrikoCreator.template.TemplateProblem
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_package_name_invalid
import kubriko.creator.generated.resources.error_project_folder_not_empty
import kubriko.creator.generated.resources.error_project_name_empty
import kubriko.creator.generated.resources.error_project_name_invalid
import kubriko.creator.generated.resources.error_project_not_created
import org.jetbrains.compose.resources.StringResource

/**
 * The string the user is shown for this [TemplateProblem].
 */
internal fun TemplateProblem.toResource(): StringResource = when (this) {
    TemplateProblem.PACKAGE_NAME_INVALID -> Res.string.error_package_name_invalid
    TemplateProblem.FOLDER_NOT_EMPTY -> Res.string.error_project_folder_not_empty
    TemplateProblem.NAME_EMPTY -> Res.string.error_project_name_empty
    TemplateProblem.NAME_INVALID -> Res.string.error_project_name_invalid
    TemplateProblem.NOT_CREATED -> Res.string.error_project_not_created
}
