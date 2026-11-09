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
import kubriko.creator.generated.resources.error_package_name_characters
import kubriko.creator.generated.resources.error_package_name_first_character
import kubriko.creator.generated.resources.error_package_name_keyword
import kubriko.creator.generated.resources.error_package_name_letter_beyond_a_to_z
import kubriko.creator.generated.resources.error_package_name_part_empty
import kubriko.creator.generated.resources.error_package_name_parts
import kubriko.creator.generated.resources.error_project_folder_not_empty
import kubriko.creator.generated.resources.error_project_kubriko_refused
import kubriko.creator.generated.resources.error_project_kubriko_unavailable
import kubriko.creator.generated.resources.error_project_name_characters
import kubriko.creator.generated.resources.error_project_name_empty
import kubriko.creator.generated.resources.error_project_name_first_character
import kubriko.creator.generated.resources.error_project_not_created
import kubriko.creator.generated.resources.error_project_not_created_disk_full
import kubriko.creator.generated.resources.error_project_not_created_not_allowed
import kubriko.creator.generated.resources.error_project_not_created_offline
import kubriko.creator.generated.resources.error_project_target_not_set_up
import org.jetbrains.compose.resources.StringResource

/**
 * The string the user is shown for this [TemplateProblem].
 */
internal fun TemplateProblem.toResource(): StringResource = when (this) {
    TemplateProblem.PACKAGE_NAME_PARTS -> Res.string.error_package_name_parts
    TemplateProblem.PACKAGE_NAME_PART_EMPTY -> Res.string.error_package_name_part_empty
    TemplateProblem.PACKAGE_NAME_FIRST_CHARACTER -> Res.string.error_package_name_first_character
    TemplateProblem.PACKAGE_NAME_CHARACTERS -> Res.string.error_package_name_characters
    TemplateProblem.PACKAGE_NAME_LETTER_BEYOND_A_TO_Z -> Res.string.error_package_name_letter_beyond_a_to_z
    TemplateProblem.PACKAGE_NAME_KEYWORD -> Res.string.error_package_name_keyword
    TemplateProblem.FOLDER_NOT_EMPTY -> Res.string.error_project_folder_not_empty
    TemplateProblem.NAME_EMPTY -> Res.string.error_project_name_empty
    TemplateProblem.NAME_FIRST_CHARACTER -> Res.string.error_project_name_first_character
    TemplateProblem.NAME_CHARACTERS -> Res.string.error_project_name_characters
    TemplateProblem.NOT_CREATED -> Res.string.error_project_not_created
    TemplateProblem.NOT_CREATED_OFFLINE -> Res.string.error_project_not_created_offline
    TemplateProblem.KUBRIKO_REFUSED -> Res.string.error_project_kubriko_refused
    TemplateProblem.NOT_CREATED_DISK_FULL -> Res.string.error_project_not_created_disk_full
    TemplateProblem.NOT_CREATED_NOT_ALLOWED -> Res.string.error_project_not_created_not_allowed
    TemplateProblem.KUBRIKO_UNAVAILABLE -> Res.string.error_project_kubriko_unavailable
    TemplateProblem.TARGET_NOT_SET_UP -> Res.string.error_project_target_not_set_up
}
