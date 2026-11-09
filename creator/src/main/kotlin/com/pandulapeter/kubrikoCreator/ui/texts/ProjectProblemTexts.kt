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

import com.pandulapeter.kubrikoCreator.project.ProjectProblem
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_no_desktop_module
import kubriko.creator.generated.resources.error_not_a_gradle_project
import kubriko.creator.generated.resources.error_project_file_empty
import kubriko.creator.generated.resources.error_project_file_not_an_object
import kubriko.creator.generated.resources.error_project_file_not_decoded
import kubriko.creator.generated.resources.error_project_file_not_json
import kubriko.creator.generated.resources.error_project_file_not_json_somewhere
import kubriko.creator.generated.resources.error_project_file_not_read
import kubriko.creator.generated.resources.error_project_file_value_missing
import kubriko.creator.generated.resources.error_project_file_value_not_number
import kubriko.creator.generated.resources.error_project_file_value_not_text
import kubriko.creator.generated.resources.error_project_file_value_too_large
import kubriko.creator.generated.resources.error_project_format_not_upgraded
import kubriko.creator.generated.resources.error_project_format_not_upgraded_on_open
import kubriko.creator.generated.resources.error_project_newer_format
import kubriko.creator.generated.resources.error_project_newer_format_unknown_version
import org.jetbrains.compose.resources.StringResource

/**
 * The string the user is shown for this [ProjectProblem].
 */
internal fun ProjectProblem.toResource(): StringResource = when (this) {
    ProjectProblem.NO_DESKTOP_MODULE -> Res.string.error_no_desktop_module
    ProjectProblem.NOT_A_GRADLE_PROJECT -> Res.string.error_not_a_gradle_project
    ProjectProblem.FORMAT_NOT_UPGRADED -> Res.string.error_project_format_not_upgraded
    ProjectProblem.FORMAT_NOT_UPGRADED_ON_OPEN -> Res.string.error_project_format_not_upgraded_on_open
    ProjectProblem.NEWER_FORMAT -> Res.string.error_project_newer_format
    ProjectProblem.NEWER_FORMAT_UNKNOWN_VERSION -> Res.string.error_project_newer_format_unknown_version
    ProjectProblem.PROJECT_FILE_NOT_READ -> Res.string.error_project_file_not_read
    ProjectProblem.PROJECT_FILE_EMPTY -> Res.string.error_project_file_empty
    ProjectProblem.PROJECT_FILE_NOT_JSON -> Res.string.error_project_file_not_json
    ProjectProblem.PROJECT_FILE_NOT_AN_OBJECT -> Res.string.error_project_file_not_an_object
    ProjectProblem.PROJECT_FILE_VALUE_MISSING -> Res.string.error_project_file_value_missing
    ProjectProblem.PROJECT_FILE_VALUE_NOT_TEXT -> Res.string.error_project_file_value_not_text
    ProjectProblem.PROJECT_FILE_VALUE_NOT_NUMBER -> Res.string.error_project_file_value_not_number
    ProjectProblem.PROJECT_FILE_NOT_JSON_SOMEWHERE -> Res.string.error_project_file_not_json_somewhere
    ProjectProblem.PROJECT_FILE_VALUE_TOO_LARGE -> Res.string.error_project_file_value_too_large
    ProjectProblem.PROJECT_FILE_NOT_DECODED -> Res.string.error_project_file_not_decoded
}
