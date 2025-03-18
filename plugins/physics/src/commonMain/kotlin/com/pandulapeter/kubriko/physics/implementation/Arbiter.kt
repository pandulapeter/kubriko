/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.physics.implementation

import com.pandulapeter.kubriko.helpers.extensions.cross
import com.pandulapeter.kubriko.helpers.extensions.distanceTo
import com.pandulapeter.kubriko.helpers.extensions.dot
import com.pandulapeter.kubriko.helpers.extensions.length
import com.pandulapeter.kubriko.helpers.extensions.normal
import com.pandulapeter.kubriko.helpers.extensions.normalized
import com.pandulapeter.kubriko.helpers.extensions.scalar
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.physics.PhysicsBody
import com.pandulapeter.kubriko.physics.implementation.geometry.Circle
import com.pandulapeter.kubriko.physics.implementation.geometry.Polygon
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit
import kotlin.math.abs

/**
 * Creates manifolds to detect collisions and apply forces to them. Discrete in nature and only evaluates pairs of bodies in a single manifold.
 */
internal data class Arbiter(
    val bodyA: PhysicsBody,
    val bodyB: PhysicsBody,
    private val penetrationCorrection: Float,
) {
    private var staticFriction = (bodyA.staticFriction + bodyB.staticFriction) / 2
    private var dynamicFriction = (bodyA.dynamicFriction + bodyB.dynamicFriction) / 2
    val contacts = arrayOf(SceneOffset.Zero, SceneOffset.Zero)
    var contactNormal = SceneOffset.Zero
    var isColliding = false
    var restitution = 0f

    fun narrowPhaseCheck() {
        restitution = bodyA.restitution.coerceAtMost(bodyB.restitution)
        if (bodyA.shape is Circle && bodyB.shape is Circle) {
            circleToCircleCollision(bodyA, bodyB)
        } else if (bodyA.shape is Circle && bodyB.shape is Polygon) {
            circleToPolygonCollision(bodyA, bodyB)
        } else if (bodyA.shape is Polygon && bodyB.shape is Circle) {
            circleToPolygonCollision(bodyB, bodyA)
            if (isColliding) {
                contactNormal = contactNormal.unaryMinus()
            }
        } else if (bodyA.shape is Polygon && bodyB.shape is Polygon) {
            polygonToPolygonCollision(bodyA, bodyB)
        }
    }

    private var penetration = SceneUnit.Zero

    private fun circleToCircleCollision(bodyA: PhysicsBody, bodyB: PhysicsBody) {
        val ca = bodyA.shape as Circle
        val cb = bodyB.shape as Circle
        val normal = bodyB.position.minus(bodyA.position)
        val distance = normal.length()
        val radius = ca.radius + cb.radius
        if (distance >= radius) {
            isColliding = false
            return
        }
        isColliding = true
        if (distance == SceneUnit.Zero) {
            penetration = radius
            contactNormal = SceneOffset.Down
            contacts[0] = bodyA.position
        } else {
            penetration = radius - distance
            contactNormal = normal.normalized()
            contacts[0] = contactNormal.scalar(ca.radius) + bodyA.position
        }
    }

    private fun circleToPolygonCollision(circleBody: PhysicsBody, polygonBody: PhysicsBody) {
        val circle = circleBody.shape as Circle
        val polygon = polygonBody.shape as Polygon

        //Transpose effectively removes the rotation thus allowing the OBB vs OBB detection to become AABB vs OBB
        val distOfBodies = circleBody.position.minus(polygonBody.position)
        val polyToCircleVec = polygon.orientation.transpose().times(distOfBodies)
        var penetration = (-Float.MAX_VALUE).sceneUnit
        var faceNormalIndex = 0

        //Applies SAT to check for potential penetration
        //Retrieves best face of polygon
        for (i in polygon.vertices.indices) {
            val v = polyToCircleVec - polygon.vertices[i]
            val distance = polygon.normals[i].dot(v)

            //If circle is outside of polygon, no collision detected.
            if (distance > circle.radius) {
                return
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
            if (distBetweenObj >= circle.radius) {
                return
            }
            this.penetration = circle.radius - distBetweenObj
            isColliding = true
            contactNormal = polygon.orientation.times((vector1 - polyToCircleVec).normalized())
            contacts[0] = polygon.orientation.times(vector1) + polygonBody.position
            return
        }
        val v2ToV1 = vector1.minus(vector2)
        val circleBodyTov2 = polyToCircleVec.minus(vector2)
        val secondPolyCorner = circleBodyTov2.dot(v2ToV1)

        //If second vertex is positive, v2 face region collision check
        //Else circle has made contact with the polygon face.
        if (secondPolyCorner < SceneUnit.Zero) {
            val distBetweenObj = polyToCircleVec.distanceTo(vector2)

            //Check to see if vertex is within the circle
            if (distBetweenObj >= circle.radius) {
                return
            }
            this.penetration = circle.radius - distBetweenObj
            isColliding = true
            contactNormal = polygon.orientation.times(vector2.minus(polyToCircleVec).normalized())
            contacts[0] = polygon.orientation.times(vector2) + polygonBody.position
        } else {
            val distFromEdgeToCircle = polyToCircleVec.minus(vector1).dot(polygon.normals[faceNormalIndex])
            if (distFromEdgeToCircle >= circle.radius) {
                return
            }
            this.penetration = circle.radius - distFromEdgeToCircle
            isColliding = true
            contactNormal = polygon.orientation.times(polygon.normals[faceNormalIndex])
            contacts[0] = circleBody.position.plus(-contactNormal.scalar(circle.radius))
            contactNormal = -contactNormal
        }
    }

    private fun polygonToPolygonCollision(bodyA: PhysicsBody, bodyB: PhysicsBody) {
        val pa = bodyA.shape as Polygon
        val pb = bodyB.shape as Polygon
        val aData = AxisData()
        findAxisOfMinPenetration(aData, pa, pb)
        if (aData.penetration >= SceneUnit.Zero) {
            return
        }
        val bData = AxisData()
        findAxisOfMinPenetration(bData, pb, pa)
        if (bData.penetration >= SceneUnit.Zero) {
            return
        }
        val referenceFaceIndex: Int
        val referencePoly: Polygon
        val incidentPoly: Polygon
        val flip: Boolean
        if (selectionBias(aData.penetration, bData.penetration)) {
            referencePoly = pa
            incidentPoly = pb
            referenceFaceIndex = aData.referenceFaceIndex
            flip = false
        } else {
            referencePoly = pb
            incidentPoly = pa
            referenceFaceIndex = bData.referenceFaceIndex
            flip = true
        }

        var referenceNormal = referencePoly.normals[referenceFaceIndex]

        //Reference face of reference polygon in object space of incident polygon
        referenceNormal = referencePoly.orientation.times(referenceNormal)
        referenceNormal = incidentPoly.orientation.transpose().times(referenceNormal)

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
            incidentPoly.orientation.times(incidentPoly.vertices[incidentIndex]) + incidentPoly.body.position,
            incidentPoly.orientation.times(incidentPoly.vertices[if (incidentIndex + 1 >= incidentPoly.vertices.size) 0 else incidentIndex + 1]) + incidentPoly.body.position,
        )

        //Gets vertex's of reference polygon reference face in world space
        var v1 = referencePoly.vertices[referenceFaceIndex]
        var v2 =
            referencePoly.vertices[if (referenceFaceIndex + 1 == referencePoly.vertices.size) 0 else referenceFaceIndex + 1]

        //Rotate and translate vertex's of reference poly
        v1 = referencePoly.orientation.times(v1) + referencePoly.body.position
        v2 = referencePoly.orientation.times(v2) + referencePoly.body.position
        val refTangent = v2.minus(v1).normalized()
        val negSide = -refTangent.dot(v1)
        val posSide = refTangent.dot(v2)
        // Clips the incident face against the reference
        var np = clip(-refTangent, negSide, incidentFaceVertexes)
        if (np < 2) {
            return
        }
        np = clip(refTangent, posSide, incidentFaceVertexes)
        if (np < 2) {
            return
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
        val contactPoint: SceneOffset?
        if (contactsFound == 1) {
            contactPoint = contactVectorsFound[0]
            penetration = totalPen
        } else {
            contactPoint = contactVectorsFound[1].plus(contactVectorsFound[0]).scalar(0.5f)
            penetration = totalPen / 2
        }
        isColliding = true
        contacts[0] = contactPoint
        contactNormal = if (flip) -refFaceNormal else refFaceNormal
    }

    /**
     * Clipping for polygon collisions. Clips incident face against side planes of the reference face.
     *
     * @param planeTangent Plane to clip against
     * @param offset       Offset for clipping in world space to incident face.
     * @param incidentFace Clipped face vertex's
     * @return Number of clipped vertex's
     */
    private fun clip(planeTangent: SceneOffset, offset: SceneUnit, incidentFace: Array<SceneOffset>): Int {
        var num = 0
        val out = arrayOf(
            incidentFace[0],
            incidentFace[1],
        )
        val dist = planeTangent.dot(incidentFace[0]) - offset
        val dist1 = planeTangent.dot(incidentFace[1]) - offset
        if (dist <= SceneUnit.Zero) out[num++] = incidentFace[0]
        if (dist1 <= SceneUnit.Zero) out[num++] = incidentFace[1]
        if (dist * dist1 < SceneUnit.Zero) {
            val interp = dist / (dist - dist1)
            if (num < 2) {
                out[num] = incidentFace[1].minus(incidentFace[0]).scalar(interp).plus(incidentFace[0])
                num++
            }
        }
        incidentFace[0] = out[0]
        incidentFace[1] = out[1]
        return num
    }

    private fun findAxisOfMinPenetration(data: AxisData, polygonA: Polygon, polygonB: Polygon) {
        var distance = (-Float.MAX_VALUE).sceneUnit
        var bestIndex = 0
        for (i in polygonA.vertices.indices) {
            //Applies polygon A's orientation to its normals for calculation.
            val polyANormal = polygonA.orientation.times(polygonA.normals[i])

            //Rotates the normal by the clock wise rotation matrix of B to put the normal relative to the object space of polygon B
            //Polygon b is axis aligned and the normal is located according to this in the correct position in object space
            val objectPolyANormal = polygonB.orientation.transpose().times(polyANormal)
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
            val distanceOfBA = polygonA.body.position.minus(polygonB.body.position)

            //Best vertex relative to polygon B in object space
            val polyANormalVertex = polygonB.orientation.transpose().times(polygonA.orientation.times(polygonA.vertices[i]) + distanceOfBA)

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

    /**
     * Resolves any penetrations that are left overlapping between shapes. This can be cause due to integration errors of the solvers integration method.
     * Based on linear projection to move the shapes away from each other based on a correction constant and scaled relative to the inverse mass of the objects.
     */
    fun penetrationResolution() {
        val penetrationTolerance = penetration
        if (penetrationTolerance <= SceneUnit.Zero) {
            return
        }
        val totalMass = bodyA.mass + bodyB.mass
        val correction = penetrationTolerance * penetrationCorrection / totalMass
        bodyA.position = bodyA.position + contactNormal.scalar(-bodyA.mass.sceneUnit * correction)
        bodyB.position = bodyB.position + contactNormal.scalar(bodyB.mass.sceneUnit * correction)
    }

    /**
     * Solves the current contact manifold and applies impulses based on any contacts found.
     */
    fun solve() {
        val contactA = contacts[0] - bodyA.position
        val contactB = contacts[0] - bodyB.position

        //Relative velocity created from equation found in GDC talk of box2D lite.
        var relativeVel = bodyB.velocity
            .plus(contactB.cross(bodyB.angularVelocity.raw))
            .minus(bodyA.velocity)
            .minus(contactA.cross(bodyA.angularVelocity.raw))

        //Positive = converging Negative = diverging
        val contactVel = relativeVel.dot(contactNormal)

        //Prevents objects colliding when they are moving away from each other.
        //If not, objects could still be overlapping after a contact has been resolved and cause objects to stick together
        if (contactVel >= SceneUnit.Zero) {
            return
        }
        val acn = contactA.cross(contactNormal).raw
        val bcn = contactB.cross(contactNormal).raw
        val inverseMassSum = bodyA.invMass + bodyB.invMass + acn * acn * bodyA.invInertia + bcn * bcn * bodyB.invInertia
        var j = -(restitution.sceneUnit + SceneUnit.Unit) * contactVel
        j /= inverseMassSum
        val impulse = contactNormal.scalar(j)
        bodyB.applyLinearImpulse(impulse, contactB)
        bodyA.applyLinearImpulse(-impulse, contactA)
        relativeVel = bodyB.velocity
            .plus(contactB.cross(bodyB.angularVelocity.raw))
            .minus(bodyA.velocity)
            .minus(contactA.cross(bodyA.angularVelocity.raw))
        val t = (relativeVel + contactNormal.scalar(-relativeVel.dot(contactNormal))).normalized()
        var jt = -relativeVel.dot(t)
        jt /= inverseMassSum
        val tangentImpulse = if (abs(jt.raw).sceneUnit < j * staticFriction) {
            t.scalar(jt)
        } else {
            t.scalar(j).scalar(-dynamicFriction)
        }
        bodyB.applyLinearImpulse(tangentImpulse, contactB)
        bodyA.applyLinearImpulse(-tangentImpulse, contactA)
    }

    companion object {
        const val BIAS_RELATIVE = 0.95f
        const val BIAS_ABSOLUTE = 0.01f

        /**
         * Selects one value over another. Intended for polygon collisions to aid in choosing which axis of separation intersects the other in a consistent manner.
         * Floating point error can occur in the rotation calculations thus this method helps with choosing one axis over another in a consistent manner for stability.
         *
         * @param a penetration value a
         * @param b penetration value b
         * @return boolean value whether a is to be preferred or not.
         */
        private fun selectionBias(a: SceneUnit, b: SceneUnit) = a >= b * BIAS_RELATIVE + a * BIAS_ABSOLUTE
    }
}

/**
 * Class for data related to axis
 */
class AxisData {
    /**
     * Gets the penetration value stored.
     *
     * @return double penetration value.
     */
    var penetration: SceneUnit = (-Float.MAX_VALUE).sceneUnit

    /**
     * Gets the referenceFaceIndex value stored
     *
     * @return int referenceFaceIndex value.
     */
    var referenceFaceIndex: Int = 0
}