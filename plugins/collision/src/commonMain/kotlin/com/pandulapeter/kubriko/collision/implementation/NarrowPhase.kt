/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.collision.implementation

import com.pandulapeter.kubriko.collision.CollisionResult
import com.pandulapeter.kubriko.collision.mask.CircleCollisionMask
import com.pandulapeter.kubriko.collision.mask.CollisionMask
import com.pandulapeter.kubriko.collision.mask.ComplexCollisionMask
import com.pandulapeter.kubriko.collision.mask.PolygonCollisionMask
import com.pandulapeter.kubriko.helpers.extensions.distanceTo
import com.pandulapeter.kubriko.helpers.extensions.dot
import com.pandulapeter.kubriko.helpers.extensions.isOverlapping
import com.pandulapeter.kubriko.helpers.extensions.normal
import com.pandulapeter.kubriko.helpers.extensions.normalized
import com.pandulapeter.kubriko.helpers.extensions.scalar
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit
import kotlin.math.sqrt

/**
 * Pre-allocated marker returned by the narrow-phase checks instead of a real result in the [RESULT_NONE] and
 * [RESULT_SCRATCH] modes. Never escapes this plugin: callers using those modes only null-check it.
 */
private val COLLISION_DETECTED = CollisionResult(
    contact = SceneOffset.Zero,
    contactNormal = SceneOffset.Zero,
    penetration = SceneUnit.Zero,
)

/** The narrow phase only reports whether the masks overlap. */
internal const val RESULT_NONE = 0

/** The narrow phase overwrites the caller's reusable [CollisionResult], or allocates one when there is none. */
internal const val RESULT_OBJECT = 1

/** The narrow phase writes the contact normal and penetration into [scratchContactNormal] and [scratchPenetration]. */
internal const val RESULT_SCRATCH = 2

internal var scratchContactNormal = SceneOffset.Zero
internal var scratchPenetration = SceneUnit.Zero

private fun collisionResult(
    resultMode: Int,
    reusableResult: CollisionResult?,
    contact: SceneOffset,
    contactNormal: SceneOffset,
    penetration: SceneUnit,
): CollisionResult = if (resultMode == RESULT_SCRATCH) {
    scratchContactNormal = contactNormal
    scratchPenetration = penetration
    COLLISION_DETECTED
} else if (reusableResult != null) {
    reusableResult.contact = contact
    reusableResult.contactNormal = contactNormal
    reusableResult.penetration = penetration
    reusableResult
} else {
    CollisionResult(
        contact = contact,
        contactNormal = contactNormal,
        penetration = penetration,
    )
}

internal fun CollisionMask.collisionCheck(
    other: CollisionMask,
    shouldSkipAxisAlignedBoundingBoxCheck: Boolean,
    resultMode: Int,
    reusableResult: CollisionResult? = null,
): CollisionResult? = if (shouldSkipAxisAlignedBoundingBoxCheck || axisAlignedBoundingBox.isOverlapping(other.axisAlignedBoundingBox)) {
    val collisionMaskA = this
    val collisionMaskB = other
    val isPointA = collisionMaskA.isPoint()
    val isPointB = collisionMaskB.isPoint()
    when {
        isPointA && isPointB -> null

        isPointA && collisionMaskB is CircleCollisionMask -> checkPointToCircleCollision(
            point = collisionMaskA.position,
            circle = collisionMaskB,
            shouldFlipContactNormal = false,
            resultMode = resultMode,
            reusableResult = reusableResult,
        )

        isPointB && collisionMaskA is CircleCollisionMask -> checkPointToCircleCollision(
            point = collisionMaskB.position,
            circle = collisionMaskA,
            shouldFlipContactNormal = true,
            resultMode = resultMode,
            reusableResult = reusableResult,
        )

        isPointA && collisionMaskB is PolygonCollisionMask -> checkPointToPolygonCollision(
            point = collisionMaskA.position,
            polygon = collisionMaskB,
            shouldFlipContactNormal = false,
            resultMode = resultMode,
            reusableResult = reusableResult,
        )

        isPointB && collisionMaskA is PolygonCollisionMask -> checkPointToPolygonCollision(
            point = collisionMaskB.position,
            polygon = collisionMaskA,
            shouldFlipContactNormal = true,
            resultMode = resultMode,
            reusableResult = reusableResult,
        )

        collisionMaskA is CircleCollisionMask && collisionMaskB is CircleCollisionMask -> checkCircleToCircleCollision(
            circleA = collisionMaskA,
            circleB = collisionMaskB,
            resultMode = resultMode,
            reusableResult = reusableResult,
        )

        collisionMaskA is CircleCollisionMask && collisionMaskB is PolygonCollisionMask -> checkCircleToPolygonCollision(
            circle = collisionMaskA,
            polygon = collisionMaskB,
            shouldFlipContactNormal = false,
            resultMode = resultMode,
            reusableResult = reusableResult,
        )

        collisionMaskA is PolygonCollisionMask && collisionMaskB is CircleCollisionMask -> checkCircleToPolygonCollision(
            circle = collisionMaskB,
            polygon = collisionMaskA,
            shouldFlipContactNormal = true,
            resultMode = resultMode,
            reusableResult = reusableResult,
        )

        collisionMaskA is PolygonCollisionMask && collisionMaskB is PolygonCollisionMask -> checkPolygonToPolygonCollision(
            polygonA = collisionMaskA,
            polygonB = collisionMaskB,
            resultMode = resultMode,
            reusableResult = reusableResult,
        )

        else -> null
    }
} else {
    null
}

/** A bare point mask, or a polygon without vertices, which behaves like a point at its position. */
private fun CollisionMask.isPoint() = this !is ComplexCollisionMask || (this is PolygonCollisionMask && vertices.isEmpty())

private val polygonPolygonAData = AxisData()
private val polygonPolygonBData = AxisData()
// Two-vertex scratch faces held as raw coordinates. SceneOffset is a value class, so an
// Array<SceneOffset> boxes every vertex written into it - and polygon clipping rewrites these slots
// for every polygon pair, including pairs the caller only wants a Boolean answer for.
private val incidentFaceVertexXs = FloatArray(2)
private val incidentFaceVertexYs = FloatArray(2)
private val contactVectorXs = FloatArray(2)
private val contactVectorYs = FloatArray(2)
private val clipOutXs = FloatArray(2)
private val clipOutYs = FloatArray(2)

// The most frequent narrow-phase pair; raw floats with a squared-distance early-out keep the
// common no-collision case free of square roots and boxing.
private fun checkCircleToCircleCollision(
    circleA: CircleCollisionMask,
    circleB: CircleCollisionMask,
    resultMode: Int,
    reusableResult: CollisionResult?,
): CollisionResult? {
    val normalX = circleB.position.x.raw - circleA.position.x.raw
    val normalY = circleB.position.y.raw - circleA.position.y.raw
    val radius = circleA.radius.raw + circleB.radius.raw
    val distanceSquared = normalX * normalX + normalY * normalY
    if (distanceSquared >= radius * radius) {
        return null
    }
    if (resultMode == RESULT_NONE) {
        return COLLISION_DETECTED
    }
    val distance = sqrt(distanceSquared)
    if (distance == 0f) {
        return collisionResult(
            resultMode = resultMode,
            reusableResult = reusableResult,
            contact = circleA.position,
            contactNormal = SceneOffset.Down,
            penetration = radius.sceneUnit,
        )
    }
    val contactNormal = SceneOffset((normalX / distance).sceneUnit, (normalY / distance).sceneUnit)
    return collisionResult(
        resultMode = resultMode,
        reusableResult = reusableResult,
        contact = contactNormal.scalar(circleA.radius) + circleB.position,
        contactNormal = contactNormal,
        penetration = (radius - distance).sceneUnit,
    )
}


/**
 * The contact normal points from the point towards the circle's center, or the other way around when
 * [shouldFlipContactNormal] is set (the circle is mask A).
 */
private fun checkPointToCircleCollision(
    point: SceneOffset,
    circle: CircleCollisionMask,
    shouldFlipContactNormal: Boolean,
    resultMode: Int,
    reusableResult: CollisionResult?,
): CollisionResult? {
    val normalX = circle.position.x.raw - point.x.raw
    val normalY = circle.position.y.raw - point.y.raw
    val radius = circle.radius.raw
    val distanceSquared = normalX * normalX + normalY * normalY
    if (distanceSquared >= radius * radius) {
        return null
    }
    if (resultMode == RESULT_NONE) {
        return COLLISION_DETECTED
    }
    val distance = sqrt(distanceSquared)
    val contactNormal = if (distance == 0f) {
        SceneOffset.Down
    } else {
        SceneOffset((normalX / distance).sceneUnit, (normalY / distance).sceneUnit)
    }
    return collisionResult(
        resultMode = resultMode,
        reusableResult = reusableResult,
        contact = point,
        contactNormal = if (shouldFlipContactNormal) -contactNormal else contactNormal,
        penetration = (radius - distance).sceneUnit,
    )
}

/**
 * The contact normal is the inward normal of the polygon face closest to the point, or the outward one when
 * [shouldFlipContactNormal] is set (the polygon is mask A).
 */
private fun checkPointToPolygonCollision(
    point: SceneOffset,
    polygon: PolygonCollisionMask,
    shouldFlipContactNormal: Boolean,
    resultMode: Int,
    reusableResult: CollisionResult?,
): CollisionResult? {
    if (!polygon.isSceneOffsetInside(point)) {
        return null
    }
    if (resultMode == RESULT_NONE) {
        return COLLISION_DETECTED
    }
    val pointInPolygonSpace = polygon.transposedRotationMatrix.times(point - polygon.position)
    var separation = (-Float.MAX_VALUE).sceneUnit
    var faceNormalIndex = 0
    for (i in polygon.vertices.indices) {
        val distance = polygon.normals[i].dot(pointInPolygonSpace - polygon.vertices[i])
        if (distance > separation) {
            separation = distance
            faceNormalIndex = i
        }
    }
    val outwardNormal = polygon.rotationMatrix.times(polygon.normals[faceNormalIndex])
    return collisionResult(
        resultMode = resultMode,
        reusableResult = reusableResult,
        contact = point,
        contactNormal = if (shouldFlipContactNormal) outwardNormal else -outwardNormal,
        penetration = -separation,
    )
}

private fun checkCircleToPolygonCollision(
    circle: CircleCollisionMask,
    polygon: PolygonCollisionMask,
    shouldFlipContactNormal: Boolean,
    resultMode: Int,
    reusableResult: CollisionResult?,
): CollisionResult? {

    //Transpose effectively removes the rotation thus allowing the OBB vs OBB detection to become AABB vs OBB
    val distOfBodies = circle.position.minus(polygon.position)
    val polyToCircleVec = polygon.transposedRotationMatrix.times(distOfBodies)
    var penetration = (-Float.MAX_VALUE).sceneUnit
    var faceNormalIndex = 0

    //Applies SAT to check for potential penetration
    //Retrieves best face of polygon
    for (i in polygon.vertices.indices) {
        val v = polyToCircleVec.minus(polygon.vertices[i])
        val distance = polygon.normals[i].dot(v)

        //If circle is outside of polygon, no collision detected.
        if (distance > circle.radius) {
            return null
        }
        if (distance > penetration) {
            faceNormalIndex = i
            penetration = distance
        }
    }

    //Get vertex's of best face
    val vector1 = polygon.vertices[faceNormalIndex]
    val vector2 = polygon.vertices[if (faceNormalIndex + 1 < polygon.vertices.size) faceNormalIndex + 1 else 0]
    val v1ToV2 = vector2.minus(vector1)
    val circleBodyTov1 = polyToCircleVec.minus(vector1)
    val firstPolyCorner = circleBodyTov1.dot(v1ToV2)

    //If first vertex is positive, v1 face region collision check
    if (firstPolyCorner <= SceneUnit.Zero) {
        val distBetweenObj = polyToCircleVec.distanceTo(vector1)

        //Check to see if vertex is within the circle
        return if (distBetweenObj >= circle.radius) null
        else if (resultMode == RESULT_NONE) COLLISION_DETECTED
        else polygon.rotationMatrix.times((vector1 - polyToCircleVec).normalized()).let { contactNormal ->
            collisionResult(
                resultMode = resultMode,
                reusableResult = reusableResult,
                contact = polygon.rotationMatrix.times(vector1) + polygon.position,
                contactNormal = if (shouldFlipContactNormal) -contactNormal else contactNormal,
                penetration = circle.radius - distBetweenObj,
            )
        }
    }
    val v2ToV1 = vector1.minus(vector2)
    val circleBodyTov2 = polyToCircleVec.minus(vector2)
    val secondPolyCorner = circleBodyTov2.dot(v2ToV1)

    //If second vertex is positive, v2 face region collision check
    //Else circle has made contact with the polygon face.
    if (secondPolyCorner < SceneUnit.Zero) {
        val distBetweenObj = polyToCircleVec.distanceTo(vector2)

        //Check to see if vertex is within the circle
        return if (distBetweenObj >= circle.radius) null
        else if (resultMode == RESULT_NONE) COLLISION_DETECTED
        else polygon.rotationMatrix.times(vector2.minus(polyToCircleVec).normalized()).let { contactNormal ->
            collisionResult(
                resultMode = resultMode,
                reusableResult = reusableResult,
                contact = polygon.rotationMatrix.times(vector2) + polygon.position,
                contactNormal = if (shouldFlipContactNormal) -contactNormal else contactNormal,
                penetration = circle.radius - distBetweenObj,
            )
        }
    } else {
        val distFromEdgeToCircle = polyToCircleVec.minus(vector1).dot(polygon.normals[faceNormalIndex])
        return if (distFromEdgeToCircle >= circle.radius) null
        else if (resultMode == RESULT_NONE) COLLISION_DETECTED
        else polygon.rotationMatrix.times(polygon.normals[faceNormalIndex]).let { contactNormal ->
            collisionResult(
                resultMode = resultMode,
                reusableResult = reusableResult,
                contact = circle.position.plus(-contactNormal.scalar(circle.radius)),
                contactNormal = if (shouldFlipContactNormal) contactNormal else -contactNormal,
                penetration = circle.radius - distFromEdgeToCircle,
            )
        }
    }
}

private data class AxisData(
    var penetration: SceneUnit = (-Float.MAX_VALUE).sceneUnit,
    var referenceFaceIndex: Int = 0,
)

private fun checkPolygonToPolygonCollision(
    polygonA: PolygonCollisionMask,
    polygonB: PolygonCollisionMask,
    resultMode: Int,
    reusableResult: CollisionResult?,
): CollisionResult? {
    findAxisOfMinPenetration(polygonPolygonAData, polygonA, polygonB)
    if (polygonPolygonAData.penetration >= SceneUnit.Zero) {
        return null
    }
    findAxisOfMinPenetration(polygonPolygonBData, polygonB, polygonA)
    if (polygonPolygonBData.penetration >= SceneUnit.Zero) {
        return null
    }
    val referenceFaceIndex: Int
    val referencePoly: PolygonCollisionMask
    val incidentPoly: PolygonCollisionMask
    val flip: Boolean
    if (selectionBias(polygonPolygonAData.penetration, polygonPolygonBData.penetration)) {
        referencePoly = polygonA
        incidentPoly = polygonB
        referenceFaceIndex = polygonPolygonAData.referenceFaceIndex
        flip = false
    } else {
        referencePoly = polygonB
        incidentPoly = polygonA
        referenceFaceIndex = polygonPolygonBData.referenceFaceIndex
        flip = true
    }

    var referenceNormal = referencePoly.normals[referenceFaceIndex]

    //Reference face of reference polygon in object space of incident polygon
    referenceNormal = referencePoly.rotationMatrix.times(referenceNormal)
    referenceNormal = incidentPoly.transposedRotationMatrix.times(referenceNormal)

    //Finds face of incident polygon angled best vs reference poly normal.
    //Best face is the incident face that is the most anti parallel (most negative dot product)
    var incidentIndex = 0
    var minDot = Float.MAX_VALUE.sceneUnit
    for (i in incidentPoly.vertices.indices) {
        val dot = referenceNormal.dot(incidentPoly.normals[i])
        if (dot < minDot) {
            minDot = dot
            incidentIndex = i
        }
    }

    //Incident faces vertexes in world space
    val incidentFaceVertex1 = incidentPoly.rotationMatrix.times(incidentPoly.vertices[incidentIndex]) + incidentPoly.position
    val incidentFaceVertex2 =
        incidentPoly.rotationMatrix.times(incidentPoly.vertices[if (incidentIndex + 1 >= incidentPoly.vertices.size) 0 else incidentIndex + 1]) + incidentPoly.position
    incidentFaceVertexXs[0] = incidentFaceVertex1.x.raw
    incidentFaceVertexYs[0] = incidentFaceVertex1.y.raw
    incidentFaceVertexXs[1] = incidentFaceVertex2.x.raw
    incidentFaceVertexYs[1] = incidentFaceVertex2.y.raw

    //Gets vertex's of reference polygon reference face in world space
    var v1 = referencePoly.vertices[referenceFaceIndex]
    var v2 =
        referencePoly.vertices[if (referenceFaceIndex + 1 == referencePoly.vertices.size) 0 else referenceFaceIndex + 1]

    //Rotate and translate vertex's of reference poly
    v1 = referencePoly.rotationMatrix.times(v1) + referencePoly.position
    v2 = referencePoly.rotationMatrix.times(v2) + referencePoly.position
    val refTangent = v2.minus(v1).normalized()
    val negSide = -refTangent.dot(v1)
    val posSide = refTangent.dot(v2)
    // Clips the incident face against the reference
    var np = clip(-refTangent, negSide)
    if (np < 2) {
        return null
    }
    np = clip(refTangent, posSide)
    if (np < 2) {
        return null
    }
    // Once both clips succeed the polygons are colliding; the remainder of this function only
    // computes contact details, so the boolean-only path can stop here.
    if (resultMode == RESULT_NONE) {
        return COLLISION_DETECTED
    }
    val refFaceNormal = -refTangent.normal()
    var totalPen = SceneUnit.Zero
    var contactsFound = 0

    //Discards points that are positive/above the reference face
    for (i in 0..1) {
        val separation = refFaceNormal.dot(incidentFaceVertexAt(i)) - refFaceNormal.dot(v1)
        if (separation <= SceneUnit.Zero) {
            contactVectorXs[contactsFound] = incidentFaceVertexXs[i]
            contactVectorYs[contactsFound] = incidentFaceVertexYs[i]
            totalPen += -separation
            contactsFound++
        }
    }
    val firstContactVector = SceneOffset(contactVectorXs[0].sceneUnit, contactVectorYs[0].sceneUnit)
    val contactPoint: SceneOffset
    val penetration: SceneUnit
    if (contactsFound == 1) {
        contactPoint = firstContactVector
        penetration = totalPen
    } else {
        contactPoint = SceneOffset(contactVectorXs[1].sceneUnit, contactVectorYs[1].sceneUnit).plus(firstContactVector).scalar(0.5f)
        penetration = totalPen / 2
    }
    return collisionResult(
        resultMode = resultMode,
        reusableResult = reusableResult,
        contact = contactPoint,
        contactNormal = if (flip) -refFaceNormal else refFaceNormal,
        penetration = penetration
    )
}

private fun findAxisOfMinPenetration(
    data: AxisData,
    polygonA: PolygonCollisionMask,
    polygonB: PolygonCollisionMask,
) {
    var distance = (-Float.MAX_VALUE).sceneUnit
    var bestIndex = 0
    for (i in polygonA.vertices.indices) {
        //Applies polygon A's orientation to its normals for calculation.
        val polyANormal = polygonA.rotationMatrix.times(polygonA.normals[i])

        //Rotates the normal by the clock wise rotation matrix of B to put the normal relative to the object space of polygon B
        //Polygon b is axis aligned and the normal is located according to this in the correct position in object space
        val objectPolyANormal = polygonB.transposedRotationMatrix.times(polyANormal)
        var bestProjection = Float.MAX_VALUE.sceneUnit
        var bestVertex = polygonB.vertices[0]

        //Finds the index of the most negative vertex relative to the normal of polygon A
        for (x in polygonB.vertices.indices) {
            val vertex = polygonB.vertices[x]
            val projection = vertex.dot(objectPolyANormal)
            if (projection < bestProjection) {
                bestVertex = vertex
                bestProjection = projection
            }
        }

        //Distance of B to A in world space space
        val distanceOfBA = polygonA.position.minus(polygonB.position)

        //Best vertex relative to polygon B in object space
        val polyANormalVertex = polygonB.transposedRotationMatrix.times(polygonA.rotationMatrix.times(polygonA.vertices[i]) + distanceOfBA)

        //Distance between best vertex and polygon A's plane in object space
        val d = objectPolyANormal.dot(bestVertex.minus(polyANormalVertex))

        //Records penetration and vertex
        if (d > distance) {
            distance = d
            bestIndex = i
        }
    }
    data.penetration = distance
    data.referenceFaceIndex = bestIndex
}

private fun selectionBias(a: SceneUnit, b: SceneUnit) = a >= b * BIAS_RELATIVE + a * BIAS_ABSOLUTE

private fun incidentFaceVertexAt(index: Int) = SceneOffset(incidentFaceVertexXs[index].sceneUnit, incidentFaceVertexYs[index].sceneUnit)

private fun clip(planeTangent: SceneOffset, offset: SceneUnit): Int {
    var num = 0
    clipOutXs[0] = incidentFaceVertexXs[0]
    clipOutYs[0] = incidentFaceVertexYs[0]
    clipOutXs[1] = incidentFaceVertexXs[1]
    clipOutYs[1] = incidentFaceVertexYs[1]
    val incidentFaceVertex1 = incidentFaceVertexAt(0)
    val incidentFaceVertex2 = incidentFaceVertexAt(1)
    val dist = planeTangent.dot(incidentFaceVertex1) - offset
    val dist1 = planeTangent.dot(incidentFaceVertex2) - offset
    if (dist <= SceneUnit.Zero) {
        clipOutXs[num] = incidentFaceVertexXs[0]
        clipOutYs[num] = incidentFaceVertexYs[0]
        num++
    }
    if (dist1 <= SceneUnit.Zero) {
        clipOutXs[num] = incidentFaceVertexXs[1]
        clipOutYs[num] = incidentFaceVertexYs[1]
        num++
    }
    if (dist * dist1 < SceneUnit.Zero) {
        val interp = dist / (dist - dist1)
        if (num < 2) {
            val clippedVertex = incidentFaceVertex2.minus(incidentFaceVertex1).scalar(interp).plus(incidentFaceVertex1)
            clipOutXs[num] = clippedVertex.x.raw
            clipOutYs[num] = clippedVertex.y.raw
            num++
        }
    }
    incidentFaceVertexXs[0] = clipOutXs[0]
    incidentFaceVertexYs[0] = clipOutYs[0]
    incidentFaceVertexXs[1] = clipOutXs[1]
    incidentFaceVertexYs[1] = clipOutYs[1]
    return num
}

private const val BIAS_RELATIVE = 0.95f
private const val BIAS_ABSOLUTE = 0.01f