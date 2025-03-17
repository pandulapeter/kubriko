/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.collision.extensions

import com.pandulapeter.kubriko.collision.Collidable
import com.pandulapeter.kubriko.collision.implementation.Vec2
import com.pandulapeter.kubriko.collision.mask.CircleCollisionMask
import com.pandulapeter.kubriko.collision.mask.PolygonCollisionMask
import com.pandulapeter.kubriko.helpers.extensions.distanceTo
import com.pandulapeter.kubriko.helpers.extensions.dot
import com.pandulapeter.kubriko.helpers.extensions.isOverlapping
import com.pandulapeter.kubriko.helpers.extensions.length
import com.pandulapeter.kubriko.helpers.extensions.normal
import com.pandulapeter.kubriko.helpers.extensions.normalize
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit

fun Collidable.isCollidingWith(
    other: Collidable
) = if (collisionMask.axisAlignedBoundingBox.isOverlapping(other.collisionMask.axisAlignedBoundingBox)) {
    val collisionMaskA = collisionMask
    val collisionMaskB = other.collisionMask
    when {
        (collisionMaskA is CircleCollisionMask && collisionMaskB is CircleCollisionMask) -> checkCircleToCircleCollision(collisionMaskA, collisionMaskB)
        (collisionMaskA is CircleCollisionMask && collisionMaskB is PolygonCollisionMask) -> checkCircleToPolygonCollision(collisionMaskA, collisionMaskB)
        (collisionMaskA is PolygonCollisionMask && collisionMaskB is CircleCollisionMask) -> checkCircleToPolygonCollision(collisionMaskB, collisionMaskA)
        (collisionMaskA is PolygonCollisionMask && collisionMaskB is PolygonCollisionMask) -> checkPolygonToPolygonCollision(collisionMaskA, collisionMaskB)
        else -> true
    }
} else {
    false
}

private fun checkCircleToCircleCollision(
    circleA: CircleCollisionMask,
    circleB: CircleCollisionMask,
): Boolean {
    val normal = circleB.position.minus(circleA.position)
    val distance = normal.length()
    val radius = circleA.radius + circleB.radius
    return distance < radius
}


private fun checkCircleToPolygonCollision(
    circle: CircleCollisionMask,
    polygon: PolygonCollisionMask,
): Boolean {

    //Transpose effectively removes the rotation thus allowing the OBB vs OBB detection to become AABB vs OBB
    val distOfBodies = circle.position.minus(polygon.position)
    val polyToCircleVec = polygon.orientation.transpose().mul(distOfBodies)
    var penetration = (-Float.MAX_VALUE).sceneUnit
    var faceNormalIndex = 0

    //Applies SAT to check for potential penetration
    //Retrieves best face of polygon
    for (i in polygon.vertices.indices) {
        val v = polyToCircleVec.minus(polygon.vertices[i])
        val distance = polygon.normals[i].dot(v)

        //If circle is outside of polygon, no collision detected.
        if (distance > circle.radius) {
            return false
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
        return distBetweenObj < circle.radius
    }
    val v2ToV1 = vector1.minus(vector2)
    val circleBodyTov2 = polyToCircleVec.minus(vector2)
    val secondPolyCorner = circleBodyTov2.dot(v2ToV1)

    //If second vertex is positive, v2 face region collision check
    //Else circle has made contact with the polygon face.
    if (secondPolyCorner < SceneUnit.Zero) {
        val distBetweenObj = polyToCircleVec.distanceTo(vector2)

        //Check to see if vertex is within the circle
        return distBetweenObj < circle.radius
    } else {
        val distFromEdgeToCircle = polyToCircleVec.minus(vector1).dot(polygon.normals[faceNormalIndex])
        return distFromEdgeToCircle < circle.radius
    }
}

private data class AxisData(
    var penetration: SceneUnit = (-Float.MAX_VALUE).sceneUnit,
    var referenceFaceIndex: Int = 0,
)

private fun checkPolygonToPolygonCollision(
    polygonA: PolygonCollisionMask,
    polygonB: PolygonCollisionMask,
): Boolean {
    val aData = AxisData()
    findAxisOfMinPenetration(aData, polygonA, polygonB)
    if (aData.penetration >= SceneUnit.Zero) {
        return false
    }
    val bData = AxisData()
    findAxisOfMinPenetration(bData, polygonB, polygonA)
    if (bData.penetration >= SceneUnit.Zero) {
        return false
    }
    val referenceFaceIndex: Int
    val referencePoly: PolygonCollisionMask
    val incidentPoly: PolygonCollisionMask
    val flip: Boolean
    if (selectionBias(aData.penetration, bData.penetration)) {
        referencePoly = polygonA
        incidentPoly = polygonB
        referenceFaceIndex = aData.referenceFaceIndex
        flip = false
    } else {
        referencePoly = polygonB
        incidentPoly = polygonA
        referenceFaceIndex = bData.referenceFaceIndex
        flip = true
    }

    var referenceNormal = referencePoly.normals[referenceFaceIndex]

    //Reference face of reference polygon in object space of incident polygon
    referenceNormal = referencePoly.orientation.mul(referenceNormal)
    referenceNormal = incidentPoly.orientation.transpose().mul(referenceNormal)

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
    val incidentFaceVertexes = arrayOf(
        incidentPoly.orientation.mul(incidentPoly.vertices[incidentIndex]) + incidentPoly.position,
        incidentPoly.orientation.mul(incidentPoly.vertices[if (incidentIndex + 1 >= incidentPoly.vertices.size) 0 else incidentIndex + 1]) + incidentPoly.position
    )

    //Gets vertex's of reference polygon reference face in world space
    var v1 = referencePoly.vertices[referenceFaceIndex]
    var v2 =
        referencePoly.vertices[if (referenceFaceIndex + 1 == referencePoly.vertices.size) 0 else referenceFaceIndex + 1]

    //Rotate and translate vertex's of reference poly
    v1 = referencePoly.orientation.mul(v1) + referencePoly.position
    v2 = referencePoly.orientation.mul(v2) + referencePoly.position
    val refTangent = v2.minus(v1)
    refTangent.normalize()
    val negSide = -refTangent.dot(v1)
    val posSide = refTangent.dot(v2)
    // Clips the incident face against the reference
    val refTangentNegativeCopy = Vec2(
        x = -refTangent.x,
        y = -refTangent.y,
    )
    var np = clip(refTangentNegativeCopy, negSide, incidentFaceVertexes)
    if (np < 2) {
        return false
    }
    val refTangentPositiveCopy = Vec2(
        x = refTangent.x,
        y = refTangent.y,
    )
    np = clip(refTangentPositiveCopy, posSide, incidentFaceVertexes)
    if (np < 2) {
        return false
    }
    val refFaceNormal = -refTangent.normal()
    val contactVectorsFound = MutableList(2) { SceneOffset.Zero }
    var totalPen = SceneUnit.Zero
    var contactsFound = 0

    //Discards points that are positive/above the reference face
    for (i in 0..1) {
        val separation = refFaceNormal.dot(incidentFaceVertexes[i]) - refFaceNormal.dot(v1)
        if (separation <= SceneUnit.Zero) {
            contactVectorsFound[contactsFound] = incidentFaceVertexes[i]
            totalPen += -separation
            contactsFound++
        }
    }
    return true
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
        val polyANormal = polygonA.orientation.mul(polygonA.normals[i])

        //Rotates the normal by the clock wise rotation matrix of B to put the normal relative to the object space of polygon B
        //Polygon b is axis aligned and the normal is located according to this in the correct position in object space
        val objectPolyANormal = polygonB.orientation.transpose().mul(polyANormal)
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
        val polyANormalVertex = polygonB.orientation.transpose().mul(polygonA.orientation.mul(polygonA.vertices[i]) + distanceOfBA)

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

private fun clip(planeTangent: Vec2, offset: SceneUnit, incidentFaces: Array<SceneOffset>): Int {
    val incidentFace = incidentFaces.map { Vec2(it.x, it.y) }.toMutableList()
    var num = 0
    val out = arrayOf(
        Vec2(incidentFace[0]),
        Vec2(incidentFace[1])
    )
    val dist = planeTangent.dot(incidentFace[0]) - offset
    val dist1 = planeTangent.dot(incidentFace[1]) - offset
    if (dist <= SceneUnit.Zero) out[num++].set(incidentFace[0])
    if (dist1 <= SceneUnit.Zero) out[num++].set(incidentFace[1])
    if (dist * dist1 < SceneUnit.Zero) {
        val interp = dist / (dist - dist1)
        if (num < 2) {
            out[num].set(incidentFace[1].minus(incidentFace[0]).scalar(interp).plus(incidentFace[0]))
            num++
        }
    }
    incidentFace[0] = out[0]
    incidentFace[1] = out[1]
    return num
}

private const val BIAS_RELATIVE = 0.95f
private const val BIAS_ABSOLUTE = 0.01f