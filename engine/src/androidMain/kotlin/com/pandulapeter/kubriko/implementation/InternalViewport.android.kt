package com.pandulapeter.kubriko.implementation

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.pandulapeter.kubriko.ActivityHolder

// TODO: There should be a way to get a the Activity earlier than the first composition
@Composable
internal actual fun initializePlatformSpecificComponents() {
    val currentActivity = LocalContext.current as? Activity
    LaunchedEffect(currentActivity) {
        currentActivity?.let { ActivityHolder.updateCurrentActivity(it) }
    }
}

internal actual fun disposePlatformSpecificComponents() = ActivityHolder.updateCurrentActivity(null)