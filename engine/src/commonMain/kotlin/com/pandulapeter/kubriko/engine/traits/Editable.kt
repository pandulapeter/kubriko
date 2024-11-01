package com.pandulapeter.kubriko.engine.traits

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

interface Editable<T : Editable<T>> : Visible {

    // TODO: Use instance ID-s instead
    var isSelectedInEditor: Boolean

    override fun draw(scope: DrawScope) {
        if (isSelectedInEditor) {
            scope.drawRect(
                color = Color.Black.copy(alpha = 0.9f),
                topLeft = Offset(
                    x = -HIGHLIGHT_SIZE,
                    y = -HIGHLIGHT_SIZE,
                ),
                size = Size(
                    width = boundingBox.width + HIGHLIGHT_SIZE * 2,
                    height = boundingBox.height + HIGHLIGHT_SIZE * 2,
                ),
            )
        }
    }

    fun saveState(): State<T>

    interface State<T> {

        val typeId: String

        fun restore(): T

        fun serialize(): String
    }

    companion object {
        private const val HIGHLIGHT_SIZE = 4f
    }
}