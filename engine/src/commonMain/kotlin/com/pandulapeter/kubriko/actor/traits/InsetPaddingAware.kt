package com.pandulapeter.kubriko.actor.traits

import androidx.compose.ui.geometry.Rect
import com.pandulapeter.kubriko.actor.Actor

interface InsetPaddingAware : Actor {

    fun onInsetPaddingChanged(insetPadding: Rect)
}