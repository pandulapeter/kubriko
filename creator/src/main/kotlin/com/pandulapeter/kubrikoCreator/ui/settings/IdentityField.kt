/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.settings

import com.pandulapeter.kubrikoCreator.commands.project.SetProjectSettings
import com.pandulapeter.kubrikoCreator.project.GameIdentity
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_identity_version_number_invalid
import kubriko.creator.generated.resources.project_settings_application_id
import kubriko.creator.generated.resources.project_settings_description
import kubriko.creator.generated.resources.project_settings_name
import kubriko.creator.generated.resources.project_settings_vendor
import kubriko.creator.generated.resources.project_settings_version_name
import kubriko.creator.generated.resources.project_settings_version_number
import org.jetbrains.compose.resources.StringResource

/**
 * One field of the Identity section of the project settings.
 */
internal enum class IdentityField(
    val label: StringResource,
) {
    NAME(Res.string.project_settings_name),
    VERSION_NAME(Res.string.project_settings_version_name),
    VERSION_NUMBER(Res.string.project_settings_version_number),
    APPLICATION_ID(Res.string.project_settings_application_id),
    VENDOR(Res.string.project_settings_vendor),
    DESCRIPTION(Res.string.project_settings_description),
    ;

    fun valueOf(identity: GameIdentity) = when (this) {
        NAME -> identity.name
        VERSION_NAME -> identity.versionName
        VERSION_NUMBER -> identity.versionNumber.toString()
        APPLICATION_ID -> identity.applicationId
        VENDOR -> identity.vendor
        DESCRIPTION -> identity.description
    }

    /**
     * What typing [text] into this field changes, or why it can't be, with the message's arguments.
     */
    fun changeOf(identity: GameIdentity, text: String): IdentityFieldChange {
        val input = when (this) {
            NAME -> SetProjectSettings.Input(name = text)
            VERSION_NAME -> SetProjectSettings.Input(versionName = text.trim())
            VERSION_NUMBER -> SetProjectSettings.Input(versionNumber = text.trim().toIntOrNull() ?: return invalidVersionNumber)
            APPLICATION_ID -> SetProjectSettings.Input(applicationId = text.trim())
            VENDOR -> SetProjectSettings.Input(vendor = text)
            DESCRIPTION -> SetProjectSettings.Input(description = text)
        }
        return input.appliedTo(identity).validate()?.let { (reason, arguments) -> IdentityFieldChange.Invalid(reason, arguments) }
            ?: IdentityFieldChange.Valid(input)
    }

    private companion object {
        val invalidVersionNumber = IdentityFieldChange.Invalid(Res.string.error_identity_version_number_invalid, listOf(GameIdentity.MAXIMUM_VERSION_NUMBER.toString()))
    }
}

internal sealed interface IdentityFieldChange {

    data class Valid(
        val input: SetProjectSettings.Input,
    ) : IdentityFieldChange

    data class Invalid(
        val reason: StringResource,
        val arguments: List<String>,
    ) : IdentityFieldChange
}
