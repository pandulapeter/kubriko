package com.pandulapeter.kubriko

import android.app.Activity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object ActivityHolder {
    private val _currentActivity = MutableStateFlow<Activity?>(null)
    val currentActivity = _currentActivity.asStateFlow()

    internal fun updateCurrentActivity(activity: Activity?) = _currentActivity.update { activity }
}