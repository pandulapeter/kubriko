package com.pandulapeter.kubriko.uiComponents.utilities

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.FontResource
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.vectorResource

@Composable
actual fun preloadedFont(
    resource: FontResource,
    weight: FontWeight,
    style: FontStyle
): State<Font?> {
    val font = Font(
        resource = resource,
        weight = weight,
        style = style
    )
    return remember { derivedStateOf { font } }
}

@Composable
actual fun preloadedImageBitmap(
    resource: DrawableResource,
): State<ImageBitmap?> {
    val image = imageResource(resource)
    return remember { derivedStateOf { image } }
}

@Composable
actual fun preloadedImageVector(
    resource: DrawableResource,
): State<ImageVector?> {
    val vector = vectorResource(resource)
    return remember { derivedStateOf { vector } }
}