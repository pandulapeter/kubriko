package com.pandulapeter.kubriko.implementation.extensions

import androidx.compose.ui.input.pointer.PointerIcon
import java.awt.Point
import java.awt.Toolkit
import java.awt.image.BufferedImage

internal actual val pointerIconInvisible: PointerIcon by lazy {
    PointerIcon(
        Toolkit.getDefaultToolkit().createCustomCursor(
            BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB),
            Point(0, 0),
            "Invisible Cursor"
        )
    )
}