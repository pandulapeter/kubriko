/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoShowcase.implementation.ui.licenses

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.shared.StateHolder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kubriko.app.generated.resources.Res
import kubriko.app.generated.resources.other_licenses_content
import org.jetbrains.compose.resources.stringResource


fun createLicensesScreenStateHolder(): LicensesScreenStateHolder = LicensesScreenStateHolderImpl()

@Composable
internal fun LicensesScreen(
    modifier: Modifier = Modifier,
    stateHolder: LicensesScreenStateHolder = createLicensesScreenStateHolder(),
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
    scrollState: ScrollState = rememberScrollState(),
) = Column(
    modifier = modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .windowInsetsPadding(windowInsets.only(WindowInsetsSides.Bottom + WindowInsetsSides.Right))
        .padding(16.dp),
) {
    Text(
        text = stringResource(Res.string.other_licenses_content),
    )
}

sealed interface LicensesScreenStateHolder : StateHolder

private class LicensesScreenStateHolderImpl : LicensesScreenStateHolder {
    override val kubriko: Flow<Kubriko?> = emptyFlow()

    override fun dispose() = Unit
}