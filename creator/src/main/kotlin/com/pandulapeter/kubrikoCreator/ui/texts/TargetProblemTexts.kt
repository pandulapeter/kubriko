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

import com.pandulapeter.kubrikoCreator.template.TargetProblem
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_target_already_off
import kubriko.creator.generated.resources.error_target_already_on
import kubriko.creator.generated.resources.error_target_android_sdk_directory_invalid
import kubriko.creator.generated.resources.error_target_android_sdk_missing
import kubriko.creator.generated.resources.error_target_android_sdk_missing_while_on
import kubriko.creator.generated.resources.error_target_application_id_not_for_ios
import kubriko.creator.generated.resources.error_target_changed_by_hand
import kubriko.creator.generated.resources.error_target_changed_since_shown
import kubriko.creator.generated.resources.error_target_changed_while_applying
import kubriko.creator.generated.resources.error_target_desktop_always_on
import kubriko.creator.generated.resources.error_target_template_outdated
import kubriko.creator.generated.resources.error_target_xcode_missing
import org.jetbrains.compose.resources.StringResource

/**
 * The string the user is shown for this [TargetProblem].
 */
internal fun TargetProblem.toResource(): StringResource = when (this) {
    TargetProblem.ALREADY_OFF -> Res.string.error_target_already_off
    TargetProblem.ALREADY_ON -> Res.string.error_target_already_on
    TargetProblem.ANDROID_SDK_MISSING -> Res.string.error_target_android_sdk_missing
    TargetProblem.ANDROID_SDK_MISSING_WHILE_ON -> Res.string.error_target_android_sdk_missing_while_on
    TargetProblem.ANDROID_SDK_DIRECTORY_INVALID -> Res.string.error_target_android_sdk_directory_invalid
    TargetProblem.APPLICATION_ID_NOT_FOR_IOS -> Res.string.error_target_application_id_not_for_ios
    TargetProblem.CHANGED_BY_HAND -> Res.string.error_target_changed_by_hand
    TargetProblem.CHANGED_SINCE_SHOWN -> Res.string.error_target_changed_since_shown
    TargetProblem.CHANGED_WHILE_APPLYING -> Res.string.error_target_changed_while_applying
    TargetProblem.DESKTOP_ALWAYS_ON -> Res.string.error_target_desktop_always_on
    TargetProblem.TEMPLATE_OUTDATED -> Res.string.error_target_template_outdated
    TargetProblem.XCODE_MISSING -> Res.string.error_target_xcode_missing
}
