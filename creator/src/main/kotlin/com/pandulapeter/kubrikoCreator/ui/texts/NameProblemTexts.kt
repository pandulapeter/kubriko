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
import kubriko.creator.generated.resources.error_document_in_left_out_folder
import kubriko.creator.generated.resources.error_document_in_screenplay
import kubriko.creator.generated.resources.error_document_name_characters
import kubriko.creator.generated.resources.error_document_name_end
import kubriko.creator.generated.resources.error_document_name_first_character
import kubriko.creator.generated.resources.error_document_not_markdown
import kubriko.creator.generated.resources.error_document_path_too_long
import kubriko.creator.generated.resources.error_folder_name_characters
import kubriko.creator.generated.resources.error_folder_name_end_space
import kubriko.creator.generated.resources.error_folder_name_first_character
import kubriko.creator.generated.resources.error_name_empty
import kubriko.creator.generated.resources.error_name_keyword
import kubriko.creator.generated.resources.error_name_letter_beyond_a_to_z
import kubriko.creator.generated.resources.error_page_name_too_long
import kubriko.creator.generated.resources.error_path_part_empty
import kubriko.creator.generated.resources.error_property_name_characters
import kubriko.creator.generated.resources.error_property_name_first_character
import kubriko.creator.generated.resources.error_property_name_reserved
import kubriko.creator.generated.resources.error_property_name_taken
import kubriko.creator.generated.resources.error_scene_name_characters
import kubriko.creator.generated.resources.error_scene_name_first_character
import kubriko.creator.generated.resources.error_timer_name_characters
import kubriko.creator.generated.resources.error_timer_name_first_character
import kubriko.creator.generated.resources.error_type_name_base_suffix
import kubriko.creator.generated.resources.error_type_name_characters
import kubriko.creator.generated.resources.error_type_name_first_character
import kubriko.creator.generated.resources.error_type_name_reserved
import kubriko.creator.generated.resources.error_type_name_taken
import org.jetbrains.compose.resources.StringResource

/**
 * The string the user is shown for this [NameProblem].
 */
internal fun NameProblem.toResource(): StringResource = when (this) {
    NameProblem.NAME_EMPTY -> Res.string.error_name_empty
    NameProblem.NAME_LETTER_BEYOND_A_TO_Z -> Res.string.error_name_letter_beyond_a_to_z
    NameProblem.NAME_KEYWORD -> Res.string.error_name_keyword
    NameProblem.PATH_PART_EMPTY -> Res.string.error_path_part_empty
    NameProblem.FOLDER_FIRST_CHARACTER -> Res.string.error_folder_name_first_character
    NameProblem.FOLDER_CHARACTERS -> Res.string.error_folder_name_characters
    NameProblem.FOLDER_END_SPACE -> Res.string.error_folder_name_end_space
    NameProblem.PROPERTY_FIRST_CHARACTER -> Res.string.error_property_name_first_character
    NameProblem.PROPERTY_CHARACTERS -> Res.string.error_property_name_characters
    NameProblem.PROPERTY_RESERVED -> Res.string.error_property_name_reserved
    NameProblem.PROPERTY_TAKEN -> Res.string.error_property_name_taken
    NameProblem.TIMER_FIRST_CHARACTER -> Res.string.error_timer_name_first_character
    NameProblem.TIMER_CHARACTERS -> Res.string.error_timer_name_characters
    NameProblem.TYPE_BASE_SUFFIX -> Res.string.error_type_name_base_suffix
    NameProblem.TYPE_FIRST_CHARACTER -> Res.string.error_type_name_first_character
    NameProblem.TYPE_CHARACTERS -> Res.string.error_type_name_characters
    NameProblem.TYPE_RESERVED -> Res.string.error_type_name_reserved
    NameProblem.TYPE_TAKEN -> Res.string.error_type_name_taken
    NameProblem.SCENE_FIRST_CHARACTER -> Res.string.error_scene_name_first_character
    NameProblem.SCENE_CHARACTERS -> Res.string.error_scene_name_characters
    NameProblem.PAGE_NAME_TOO_LONG -> Res.string.error_page_name_too_long
    NameProblem.DOCUMENT_PATH_TOO_LONG -> Res.string.error_document_path_too_long
    NameProblem.DOCUMENT_NOT_MARKDOWN -> Res.string.error_document_not_markdown
    NameProblem.DOCUMENT_IN_SCREENPLAY -> Res.string.error_document_in_screenplay
    NameProblem.DOCUMENT_IN_LEFT_OUT_FOLDER -> Res.string.error_document_in_left_out_folder
    NameProblem.DOCUMENT_FIRST_CHARACTER -> Res.string.error_document_name_first_character
    NameProblem.DOCUMENT_CHARACTERS -> Res.string.error_document_name_characters
    NameProblem.DOCUMENT_END -> Res.string.error_document_name_end
}
