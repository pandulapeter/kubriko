package com.pandulapeter.kubriko.extensions

import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize

val SceneSize.bottomRight get() = SceneOffset(width, height)