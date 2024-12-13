package com.pandulapeter.kubriko.implementation.extensions

import androidx.compose.ui.input.pointer.PointerIcon
import com.pandulapeter.kubriko.ActivityHolder

internal actual val pointerIconInvisible = PointerIcon(android.view.PointerIcon.getSystemIcon(ActivityHolder.currentActivity.value!!, android.view.PointerIcon.TYPE_NULL))