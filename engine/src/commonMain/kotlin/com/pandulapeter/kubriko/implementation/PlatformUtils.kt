package com.pandulapeter.kubriko.implementation

import androidx.compose.runtime.Composable

@Composable
internal expect fun initializePlatformSpecificComponents()

internal expect fun disposePlatformSpecificComponents()

internal expect fun getDefaultFocusDebounce(): Long