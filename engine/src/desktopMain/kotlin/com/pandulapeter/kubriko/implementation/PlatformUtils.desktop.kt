package com.pandulapeter.kubriko.implementation

import androidx.compose.runtime.Composable

@Composable
internal actual fun initializePlatformSpecificComponents() = Unit

internal actual fun disposePlatformSpecificComponents() = Unit

internal actual fun getDefaultFocusDebounce() = 0L