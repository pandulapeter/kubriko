package com.pandulapeter.kubriko.engine.implementation.extensions

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.engine.types.WorldCoordinates

private const val GRID_CELL_SIZE = 100f
private const val ALPHA_MAJOR = 0.4f
private const val ALPHA_MINOR = 0.2f

// TODO: Should be moved to the editor module
// TODO: Secondary lines should fade out gradually
internal fun DrawScope.drawEditorGrid(
    viewportCenter: WorldCoordinates,
    viewportSize: Size,
    viewportScaleFactor: Float,
)  {
    // Calculate the viewport boundaries in world coordinates
    val viewportTopLeft = Offset.Zero.toWorldCoordinates(
        viewportCenter = viewportCenter,
        viewportSize = viewportSize,
        viewportScaleFactor = viewportScaleFactor,
    )
    val viewportBottomRight = Offset(viewportSize.width, viewportSize.height).toWorldCoordinates(
        viewportCenter = viewportCenter,
        viewportSize = viewportSize,
        viewportScaleFactor = viewportScaleFactor,
    )

    // Precomputed values for major and minor lines
    val strokeWidth = 1f / viewportScaleFactor

    // Calculate the starting point for vertical lines and ensure alignment with (0,0)
    var startX = (viewportTopLeft.x / GRID_CELL_SIZE).toInt() * GRID_CELL_SIZE
    if (startX > viewportTopLeft.x) startX -= GRID_CELL_SIZE
    val startXLineIndex = (startX / GRID_CELL_SIZE).toInt() // Adjust to always align with origin

    // Draw vertical grid lines
    var currentX = startX
    var iterationX = 0
    while (currentX <= viewportBottomRight.x) {
        val alpha = if ((startXLineIndex + iterationX) % 10 == 0) ALPHA_MAJOR else ALPHA_MINOR
        drawLine(
            color = Color.Gray.copy(alpha = alpha),
            start = Offset(currentX, viewportTopLeft.y),
            end = Offset(currentX, viewportBottomRight.y),
            strokeWidth = strokeWidth
        )
        currentX += GRID_CELL_SIZE
        iterationX++
    }

    // Calculate the starting point for horizontal lines, aligning with (0,0)
    var startY = (viewportTopLeft.y / GRID_CELL_SIZE).toInt() * GRID_CELL_SIZE
    if (startY > viewportTopLeft.y) startY -= GRID_CELL_SIZE
    val startYLineIndex = (startY / GRID_CELL_SIZE).toInt() // Align with origin

    // Draw horizontal grid lines
    var currentY = startY
    var iterationY = 0
    while (currentY <= viewportBottomRight.y) {
        val alpha = if ((startYLineIndex + iterationY) % 10 == 0) ALPHA_MAJOR else ALPHA_MINOR
        drawLine(
            color = Color.Gray.copy(alpha = alpha),
            start = Offset(viewportTopLeft.x, currentY),
            end = Offset(viewportBottomRight.x, currentY),
            strokeWidth = strokeWidth
        )
        currentY += GRID_CELL_SIZE
        iterationY++
    }
}