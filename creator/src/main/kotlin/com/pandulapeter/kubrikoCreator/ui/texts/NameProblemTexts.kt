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

import com.pandulapeter.kubrikoCreator.actors.NameProblem
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_folder_name_invalid
import kubriko.creator.generated.resources.error_property_name_invalid
import kubriko.creator.generated.resources.error_property_name_reserved
import kubriko.creator.generated.resources.error_property_name_taken
import kubriko.creator.generated.resources.error_type_name_base_suffix
import kubriko.creator.generated.resources.error_type_name_invalid
import kubriko.creator.generated.resources.error_type_name_reserved
import kubriko.creator.generated.resources.error_type_name_taken
import org.jetbrains.compose.resources.StringResource

/**
 * The string the user is shown for this [NameProblem].
 */
internal fun NameProblem.toResource(): StringResource = when (this) {
    NameProblem.FOLDER_INVALID -> Res.string.error_folder_name_invalid
    NameProblem.PROPERTY_INVALID -> Res.string.error_property_name_invalid
    NameProblem.PROPERTY_RESERVED -> Res.string.error_property_name_reserved
    NameProblem.PROPERTY_TAKEN -> Res.string.error_property_name_taken
    NameProblem.TYPE_BASE_SUFFIX -> Res.string.error_type_name_base_suffix
    NameProblem.TYPE_INVALID -> Res.string.error_type_name_invalid
    NameProblem.TYPE_RESERVED -> Res.string.error_type_name_reserved
    NameProblem.TYPE_TAKEN -> Res.string.error_type_name_taken
}
