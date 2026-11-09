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

import com.pandulapeter.kubrikoCreator.project.IdentityProblem
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_identity_application_id_characters
import kubriko.creator.generated.resources.error_identity_application_id_first_character
import kubriko.creator.generated.resources.error_identity_application_id_letter_beyond_a_to_z
import kubriko.creator.generated.resources.error_identity_application_id_part_empty
import kubriko.creator.generated.resources.error_identity_application_id_parts
import kubriko.creator.generated.resources.error_identity_name_empty
import kubriko.creator.generated.resources.error_identity_name_invalid
import kubriko.creator.generated.resources.error_identity_text_invalid
import kubriko.creator.generated.resources.error_identity_version_name_invalid
import kubriko.creator.generated.resources.error_identity_version_number_invalid
import org.jetbrains.compose.resources.StringResource

/**
 * The string the user is shown for this [IdentityProblem].
 */
internal fun IdentityProblem.toResource(): StringResource = when (this) {
    IdentityProblem.APPLICATION_ID_PARTS -> Res.string.error_identity_application_id_parts
    IdentityProblem.APPLICATION_ID_PART_EMPTY -> Res.string.error_identity_application_id_part_empty
    IdentityProblem.APPLICATION_ID_FIRST_CHARACTER -> Res.string.error_identity_application_id_first_character
    IdentityProblem.APPLICATION_ID_CHARACTERS -> Res.string.error_identity_application_id_characters
    IdentityProblem.APPLICATION_ID_LETTER_BEYOND_A_TO_Z -> Res.string.error_identity_application_id_letter_beyond_a_to_z
    IdentityProblem.NAME_EMPTY -> Res.string.error_identity_name_empty
    IdentityProblem.NAME_INVALID -> Res.string.error_identity_name_invalid
    IdentityProblem.TEXT_INVALID -> Res.string.error_identity_text_invalid
    IdentityProblem.VERSION_NAME_INVALID -> Res.string.error_identity_version_name_invalid
    IdentityProblem.VERSION_NUMBER_INVALID -> Res.string.error_identity_version_number_invalid
}
