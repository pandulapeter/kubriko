/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.collision.extensions

import com.pandulapeter.kubriko.collision.Collidable
import com.pandulapeter.kubriko.collision.CollisionResult
import com.pandulapeter.kubriko.collision.mask.CircleCollisionMask
import com.pandulapeter.kubriko.collision.mask.CollisionMask
import com.pandulapeter.kubriko.collision.mask.PolygonCollisionMask
import com.pandulapeter.kubriko.helpers.extensions.distanceTo
import com.pandulapeter.kubriko.helpers.extensions.dot
import com.pandulapeter.kubriko.helpers.extensions.isOverlapping
import com.pandulapeter.kubriko.helpers.extensions.length
import com.pandulapeter.kubriko.helpers.extensions.normal
import com.pandulapeter.kubriko.helpers.extensions.normalized
import com.pandulapeter.kubriko.helpers.extensions.scalar
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit

fun Collidable.isCollidingWith(
    other: Collidable
) = collisionMask.hasCollisionWith(other.collisionMask)

fun CollisionMask.collisionResultWith(
    other: CollisionMask,
    shouldSkipAxisAlignedBoundingBoxCheck: Boolean,
): CollisionResult? = collisionCheck(
    other = other,
    shouldSkipAxisAlignedBoundingBoxCheck = shouldSkipAxisAlignedBoundingBoxCheck,
    shouldCollectResult = true,
)

/**
 * Computes the part of [desiredMovement] this mask can travel without overlapping any of the given
 * [obstacles], sliding along their surfaces so an angled approach glides around an obstacle's edge
 * instead of stopping dead against it.
 *
 * When the full movement is blocked, the component pointing into the closest obstacle is removed and
 * the remaining tangential movement is retried, so the actor keeps as much of its speed as it can
 * while following the obstacle's contour. Only a head-on approach (movement aimed straight at the
 * contact) comes to a full stop. Intended for kinematic actors (ones moved by writing their position
 * directly, without the physics plugin) that should be blocked by solid scenery: apply the returned
 * offset to the actor's position instead of [desiredMovement]. The mask is probed at candidate
 * positions during the call and restored to its original position before returning, so the caller
 * stays in control of when the movement is committed.
 *
 * @param desiredMovement The movement the actor would make if nothing were in the way.
 * @param obstacles The masks that should block movement. The receiver is ignored if it is present.
 * @return The largest collision-free movement: [desiredMovement] when the path is clear, a shorter
 * offset tangential to the blocking obstacle when it can slide, or [SceneOffset.Zero] when it cannot.
 */
fun CollisionMask.slidingMovement(
    desiredMovement: SceneOffset,
    obstacles: List<CollisionMask>,
): SceneOffset {
    val origin = position
    try {
        position = origin + desiredMovement
        if (!collidesWithAny(obstacles)) {
            return desiredMovement
        }
        var attempt = desiredMovement
        // Each pass slides the movement along the surface it currently hits; a second pass handles
        // the corner where the slid movement runs into another obstacle. Movement that still
        // collides after that settles to a stop rather than risk passing through.
        repeat(MAXIMUM_SLIDE_ITERATIONS) {
            val blocking = deepestCollision(obstacles) ?: return attempt
            val penetratingAmount = attempt.dot(blocking.contactNormal)
            if (penetratingAmount <= SceneUnit.Zero) {
                return SceneOffset.Zero
            }
            attempt -= blocking.contactNormal * penetratingAmount
            position = origin + attempt
        }
        return if (collidesWithAny(obstacles)) SceneOffset.Zero else attempt
    } finally {
        position = origin
    }
}

/**
 * Computes an offset that pushes this mask out of any of the given [obstacles] it currently
 * overlaps, summing the resolution vector (contact normal scaled by penetration depth) of each
 * overlap.
 *
 * This recovers from overlaps a [slidingMovement] sweep cannot prevent, such as an actor that
 * spawned inside scenery or scenery that was placed on top of it: apply the returned offset to the
 * actor's position to separate it.
 *
 * @param obstacles The masks to separate from. The receiver is ignored if it is present.
 * @return The offset that resolves the overlaps, or [SceneOffset.Zero] when there is none.
 */
fun CollisionMask.depenetrationFrom(
    obstacles: List<CollisionMask>,
): SceneOffset {
    var push = SceneOffset.Zero
    for (index in obstacles.indices) {
        val obstacle = obstacles[index]
        if (obstacle !== this) {
            collisionResultWith(
                other = obstacle,
                shouldSkipAxisAlignedBoundingBoxCheck = false,
            )?.let { result ->
                push -= result.contactNormal * result.penetration
            }
        }
    }
    return push
}

private fun CollisionMask.collidesWithAny(
    obstacles: List<CollisionMask>,
): Boolean {
    for (index in obstacles.indices) {
        val obstacle = obstacles[index]
        if (obstacle !== this && hasCollisionWith(obstacle)) {
            return true
        }
    }
    return false
}

private fun CollisionMask.deepestCollision(
    obstacles: List<CollisionMask>,
): CollisionResult? {
    var deepest: CollisionResult? = null
    for (index in obstacles.indices) {
        val obstacle = obstacles[index]
        if (obstacle !== this) {
            val result = collisionResultWith(
                other = obstacle,
                shouldSkipAxisAlignedBoundingBoxCheck = false,
            )
            if (result != null && (deepest == null || result.penetration > deepest.penetration)) {
                deepest = result
            }
        }
    }
    return deepest
}

private const val MAXIMUM_SLIDE_ITERATIONS = 2

/**
 * Boolean-only collision test: runs the same broad and narrow phase as [collisionResultWith] but
 * never constructs a [CollisionResult] (the detection loop in CollisionManagerImpl only needs the
 * yes/no answer, and the result object would otherwise be allocated for every colliding pair on
 * every frame).
 */
internal fun CollisionMask.hasCollisionWith(other: CollisionMask): Boolean = collisionCheck(
    other = other,
    shouldSkipAxisAlignedBoundingBoxCheck = false,
    shouldCollectResult = false,
) != null

// Pre-allocated marker returned by the narrow-phase checks instead of a real result when
// shouldCollectResult is false. Never escapes this file: callers that pass false only null-check.
private val COLLISION_DETECTED = CollisionResult(
    contact = SceneOffset.Zero,
    contactNormal = SceneOffset.Zero,
    penetration = SceneUnit.Zero,
)

private fun CollisionMask.collisionCheck(
    other: CollisionMask,
    shouldSkipAxisAlignedBoundingBoxCheck: Boolean,
    shouldCollectResult: Boolean,
): CollisionResult? = if (shouldSkipAxisAlignedBoundingBoxCheck || axisAlignedBoundingBox.isOverlapping(other.axisAlignedBoundingBox)) {
    val collisionMaskA = this
    val collisionMaskB = other
    when {
        collisionMaskA is CircleCollisionMask && collisionMaskB is CircleCollisionMask -> checkCircleToCircleCollision(
            circleA = collisionMaskA,
            circleB = collisionMaskB,
            shouldCollectResult = shouldCollectResult,
        )

        collisionMaskA is CircleCollisionMask && collisionMaskB is PolygonCollisionMask -> checkCircleToPolygonCollision(
            circle = collisionMaskA,
            polygon = collisionMaskB,
            shouldCollectResult = shouldCollectResult,
        )

        collisionMaskA is PolygonCollisionMask && collisionMaskB is CircleCollisionMask -> checkCircleToPolygonCollision(
            circle = collisionMaskB,
            polygon = collisionMaskA,
            shouldCollectResult = shouldCollectResult,
        )?.let {
            if (shouldCollectResult) CollisionResult(
                contact = it.contact,
                contactNormal = -it.contactNormal,
                penetration = it.penetration,
            ) else it
        }

        collisionMaskA is PolygonCollisionMask && collisionMaskB is PolygonCollisionMask -> checkPolygonToPolygonCollision(
            polygonA = collisionMaskA,
            polygonB = collisionMaskB,
            shouldCollectResult = shouldCollectResult,
        )

        else -> null
    }
} else {
    null
}

private val polygonPolygonAData = AxisData()
private val polygonPolygonBData = AxisData()
private val incidentFaceVertexesBuffer = arrayOf(SceneOffset.Zero, SceneOffset.Zero)
private val contactVectorsFoundBuffer = arrayOf(SceneOffset.Zero, SceneOffset.Zero)
private val clipOutBuffer = arrayOf(SceneOffset.Zero, SceneOffset.Zero)

private fun checkCircleToCircleCollision(
    circleA: CircleCollisionMask,
    circleB: CircleCollisionMask,
    shouldCollectResult: Boolean,
): CollisionResult? {
    val normal = circleB.position.minus(circleA.position)
    val distance = normal.length()
    val radius = circleA.radius + circleB.radius
    return if (distance >= radius) null
    else if (!shouldCollectResult) COLLISION_DETECTED
    else if (distance == SceneUnit.Zero) CollisionResult(
        contact = circleA.position,
        contactNormal = SceneOffset.Down,
        penetration = radius,
    )
    else normal.normalized().let { contactNormal ->
        CollisionResult(
            contact = contactNormal.scalar(circleA.radius) + circleB.position,
            contactNormal = contactNormal,
            penetration = radius - distance
        )
    }
}


private fun checkCircleToPolygonCollision(
    circle: CircleCollisionMask,
    polygon: PolygonCollisionMask,
    shouldCollectResult: Boolean,
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
        else if (!shouldCollectResult) COLLISION_DETECTED
        else CollisionResult(
            contact = polygon.rotationMatrix.times(vector1) + polygon.position,
            contactNormal = polygon.rotationMatrix.times((vector1 - polyToCircleVec).normalized()),
            penetration = circle.radius - distBetweenObj,
        )
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
        else if (!shouldCollectResult) COLLISION_DETECTED
        else CollisionResult(
            contact = polygon.rotationMatrix.times(vector2) + polygon.position,
            contactNormal = polygon.rotationMatrix.times(vector2.minus(polyToCircleVec).normalized()),
            penetration = circle.radius - distBetweenObj,
        )
    } else {
        val distFromEdgeToCircle = polyToCircleVec.minus(vector1).dot(polygon.normals[faceNormalIndex])
        return if (distFromEdgeToCircle >= circle.radius) null
        else if (!shouldCollectResult) COLLISION_DETECTED
        else polygon.rotationMatrix.times(polygon.normals[faceNormalIndex]).let { contactNormal ->
            CollisionResult(
                contact = circle.position.plus(-contactNormal.scalar(circle.radius)),
                contactNormal = -contactNormal,
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
    shouldCollectResult: Boolean,
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
    incidentFaceVertexesBuffer[0] = incidentPoly.rotationMatrix.times(incidentPoly.vertices[incidentIndex]) + incidentPoly.position
    incidentFaceVertexesBuffer[1] = incidentPoly.rotationMatrix.times(incidentPoly.vertices[if (incidentIndex + 1 >= incidentPoly.vertices.size) 0 else incidentIndex + 1]) + incidentPoly.position

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
    var np = clip(-refTangent, negSide, incidentFaceVertexesBuffer)
    if (np < 2) {
        return null
    }
    np = clip(refTangent, posSide, incidentFaceVertexesBuffer)
    if (np < 2) {
        return null
    }
    // Once both clips succeed the polygons are colliding; the remainder of this function only
    // computes contact details, so the boolean-only path can stop here.
    if (!shouldCollectResult) {
        return COLLISION_DETECTED
    }
    val refFaceNormal = -refTangent.normal()
    var totalPen = SceneUnit.Zero
    var contactsFound = 0

    //Discards points that are positive/above the reference face
    for (i in 0..1) {
        val separation = refFaceNormal.dot(incidentFaceVertexesBuffer[i]) - refFaceNormal.dot(v1)
        if (separation <= SceneUnit.Zero) {
            contactVectorsFoundBuffer[contactsFound] = incidentFaceVertexesBuffer[i]
            totalPen += -separation
            contactsFound++
        }
    }
    val contactPoint: SceneOffset?
    val penetration: SceneUnit
    if (contactsFound == 1) {
        contactPoint = contactVectorsFoundBuffer[0]
        penetration = totalPen
    } else {
        contactPoint = contactVectorsFoundBuffer[1].plus(contactVectorsFoundBuffer[0]).scalar(0.5f)
        penetration = totalPen / 2
    }
    return CollisionResult(
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

private fun clip(planeTangent: SceneOffset, offset: SceneUnit, incidentFaces: Array<SceneOffset>): Int {
    var num = 0
    clipOutBuffer[0] = incidentFaces[0]
    clipOutBuffer[1] = incidentFaces[1]
    val dist = planeTangent.dot(incidentFaces[0]) - offset
    val dist1 = planeTangent.dot(incidentFaces[1]) - offset
    if (dist <= SceneUnit.Zero) clipOutBuffer[num++] = incidentFaces[0]
    if (dist1 <= SceneUnit.Zero) clipOutBuffer[num++] = incidentFaces[1]
    if (dist * dist1 < SceneUnit.Zero) {
        val interp = dist / (dist - dist1)
        if (num < 2) {
            clipOutBuffer[num] = incidentFaces[1].minus(incidentFaces[0]).scalar(interp).plus(incidentFaces[0])
            num++
        }
    }
    incidentFaces[0] = clipOutBuffer[0]
    incidentFaces[1] = clipOutBuffer[1]
    return num
}

private const val BIAS_RELATIVE = 0.95f
private const val BIAS_ABSOLUTE = 0.01f