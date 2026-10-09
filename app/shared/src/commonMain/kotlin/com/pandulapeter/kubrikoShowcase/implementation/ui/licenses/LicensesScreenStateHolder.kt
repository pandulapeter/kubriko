/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoShowcase.implementation.ui.licenses

import androidx.compose.runtime.Composable
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.shared.StateHolder
import com.pandulapeter.kubriko.uiComponents.utilities.preloadedString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kubriko.app.shared.generated.resources.Res
import kubriko.app.shared.generated.resources.other_licenses_content
import kubriko.app.shared.generated.resources.other_licenses_music_note

internal fun createLicensesScreenStateHolder(): LicensesScreenStateHolder = LicensesScreenStateHolderImpl()

internal sealed interface LicensesScreenStateHolder : StateHolder {

    companion object {
        @Composable
        fun areResourcesLoaded() = areStringResourcesLoaded()

        @Composable
        private fun areStringResourcesLoaded() = preloadedString(Res.string.other_licenses_content).value.isNotBlank()
                && LicenseType.entries.all { preloadedString(it.licenseName).value.isNotBlank() }
                && preloadedString(Res.string.other_licenses_music_note).value.isNotBlank()
    }
}

private class LicensesScreenStateHolderImpl : LicensesScreenStateHolder {
    override val kubriko: Flow<Kubriko?> = emptyFlow()

    override fun dispose() = Unit
}
