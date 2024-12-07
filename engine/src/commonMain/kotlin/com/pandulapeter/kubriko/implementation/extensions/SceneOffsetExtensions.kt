package com.pandulapeter.kubriko.implementation.extensions

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.pandulapeter.kubriko.actor.body.AxisAlignedBoundingBox
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

fun SceneOffset.angleTowards(offset: SceneOffset): AngleRadians = atan2((offset.y - y).raw, (offset.x - x).raw).rad

fun SceneOffset.constrainedWithin(topLeft: SceneOffset, bottomRight: SceneOffset): SceneOffset {
    var offset = this
    if (offset.x < topLeft.x) {
        offset = SceneOffset(topLeft.x, offset.y)
    }
    if (offset.x > bottomRight.x) {
        offset = SceneOffset(bottomRight.x, offset.y)
    }
    if (offset.y < topLeft.y) {
        offset = SceneOffset(offset.x, topLeft.y)
    }
    if (offset.y > bottomRight.y) {
        offset = SceneOffset(offset.x, bottomRight.y)
    }
    return offset
}

fun SceneOffset.clamp(
    min: SceneOffset? = null,
    max: SceneOffset? = null,
) = SceneOffset(
    x = max((min ?: this).x.raw, min((max ?: this).x.raw, x.raw)).sceneUnit,
    y = max((min ?: this).y.raw, min((max ?: this).y.raw, y.raw)).sceneUnit
)

fun SceneOffset.wrapWithin(topLeft: SceneOffset, bottomRight: SceneOffset): SceneOffset {
    var offset = this
    if (offset.x < topLeft.x) {
        offset = SceneOffset(bottomRight.x, offset.y)
    }
    if (offset.x > bottomRight.x) {
        offset = SceneOffset(topLeft.x, offset.y)
    }
    if (offset.y < topLeft.y) {
        offset = SceneOffset(offset.x, bottomRight.y)
    }
    if (offset.y > bottomRight.y) {
        offset = SceneOffset(offset.x, topLeft.y)
    }
    return offset
}

fun SceneOffset.distanceTo(other: SceneOffset): SceneUnit = (x.raw - other.x.raw).let { dx ->
    (y.raw - other.y.raw).let { dy ->
        sqrt(dx * dx + dy * dy).sceneUnit
    }
}

fun SceneOffset.length(): SceneUnit = distanceTo(SceneOffset.Zero)

fun SceneOffset.dot(v1: SceneOffset): SceneUnit = (v1.x.raw * x.raw + v1.y.raw * y.raw).sceneUnit

fun SceneOffset.normal(): SceneOffset = SceneOffset(-y, x)

fun SceneOffset.normalize(): SceneOffset = length().let { length ->
    (if (length == SceneUnit.Zero) SceneUnit.Unit else length).let { d ->
        SceneOffset(
            x = x / d,
            y = y / d,
        )
    }
}

fun SceneOffset.cross(v1: SceneOffset): SceneUnit = x * v1.y - y * v1.x

fun SceneOffset.cross(a: Float): SceneOffset = normal().scalar(a)

fun SceneOffset.scalar(a: Float): SceneOffset = SceneOffset(x * a, y * a)

fun SceneOffset.scalar(a: SceneUnit): SceneOffset = SceneOffset(x * a, y * a)

fun SceneOffset.isWithin(
    axisAlignedBoundingBox: AxisAlignedBoundingBox
): Boolean = x.raw in axisAlignedBoundingBox.left..axisAlignedBoundingBox.right && y.raw in axisAlignedBoundingBox.top..axisAlignedBoundingBox.bottom

val List<SceneOffset>.center
    get(): SceneOffset {
        if (isEmpty()) return SceneOffset.Zero
        if (size == 1) return first()
        if (size == 2) return SceneOffset(
            x = (first().x + last().x) / 2,
            y = (first().y + last().y) / 2,
        )
        var signedArea = SceneUnit.Zero
        var centroidX = SceneUnit.Zero
        var centroidY = SceneUnit.Zero
        for (i in indices) {
            val current = this[i]
            val next = this[(i + 1) % size]

            val crossProduct = current.x * next.y - next.x * current.y
            signedArea += crossProduct
            centroidX += (current.x + next.x) * crossProduct
            centroidY += (current.y + next.y) * crossProduct
        }
        signedArea *= 0.5f
        centroidX /= (6 * signedArea)
        centroidY /= (6 * signedArea)
        return SceneOffset(centroidX, centroidY)
    }

fun SceneOffset.toOffset(viewportManager: ViewportManager): Offset = toOffset(
    viewportCenter = viewportManager.cameraPosition.value,
    viewportSize = viewportManager.size.value,
    viewportScaleFactor = viewportManager.scaleFactor.value,
)

fun SceneOffset.toOffset(
    viewportCenter: SceneOffset,
    viewportSize: Size,
    viewportScaleFactor: Float,
): Offset {
    val localSceneOffset = (this - viewportCenter) * viewportScaleFactor
    return Offset(
        x = localSceneOffset.x.raw + viewportSize.width / 2,
        y = localSceneOffset.y.raw + viewportSize.height / 2
    )
}