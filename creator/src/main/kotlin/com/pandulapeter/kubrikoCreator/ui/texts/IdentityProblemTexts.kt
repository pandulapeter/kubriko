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
import kubriko.creator.generated.resources.error_identity_application_id_invalid
import kubriko.creator.generated.resources.error_identity_name_invalid
import kubriko.creator.generated.resources.error_identity_text_invalid
import kubriko.creator.generated.resources.error_identity_version_name_invalid
import kubriko.creator.generated.resources.error_identity_version_number_invalid
import org.jetbrains.compose.resources.StringResource

/**
 * The string the user is shown for this [IdentityProblem].
 */
internal fun IdentityProblem.toResource(): StringResource = when (this) {
    IdentityProblem.APPLICATION_ID_INVALID -> Res.string.error_identity_application_id_invalid
    IdentityProblem.NAME_INVALID -> Res.string.error_identity_name_invalid
    IdentityProblem.TEXT_INVALID -> Res.string.error_identity_text_invalid
    IdentityProblem.VERSION_NAME_INVALID -> Res.string.error_identity_version_name_invalid
    IdentityProblem.VERSION_NUMBER_INVALID -> Res.string.error_identity_version_number_invalid
}
