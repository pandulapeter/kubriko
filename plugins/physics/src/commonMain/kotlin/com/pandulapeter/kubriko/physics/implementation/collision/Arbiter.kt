/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.physics.implementation.collision

import com.pandulapeter.kubriko.collision.implementation.Vec2
import com.pandulapeter.kubriko.helpers.extensions.distanceTo
import com.pandulapeter.kubriko.helpers.extensions.dot
import com.pandulapeter.kubriko.helpers.extensions.length
import com.pandulapeter.kubriko.helpers.extensions.normal
import com.pandulapeter.kubriko.helpers.extensions.normalize
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.physics.implementation.collision.bodies.CollisionBodyInterface
import com.pandulapeter.kubriko.physics.implementation.dynamics.Physics
import com.pandulapeter.kubriko.physics.implementation.dynamics.bodies.PhysicalBodyInterface
import com.pandulapeter.kubriko.physics.implementation.geometry.Circle
import com.pandulapeter.kubriko.physics.implementation.geometry.Polygon
import com.pandulapeter.kubriko.physics.implementation.geometry.bodies.TranslatableBody
import com.pandulapeter.kubriko.physics.implementation.helpers.toVec2
import com.pandulapeter.kubriko.types.SceneUnit
import kotlin.math.abs

/**
 * Creates manifolds to detect collisions and apply forces to them. Discrete in nature and only evaluates pairs of bodies in a single manifold.
 */
class Arbiter(
    /**
     * Getter for Body A.
     *
     * @return Body A
     */
    val a: TranslatableBody,
    /**
     * Getter for Body B.
     *
     * @return Body B
     */
    val b: TranslatableBody
) {

    /**
     * Static fiction constant to be set during the construction of the arbiter.
     */
    private var staticFriction = 0f

    /**
     * Dynamic fiction constant to be set during the construction of the arbiter.
     */
    private var dynamicFriction = 0f

    init {
        if (a is CollisionBodyInterface && b is CollisionBodyInterface) {
            staticFriction = (a.staticFriction + b.staticFriction) / 2
            dynamicFriction = (a.dynamicFriction + b.dynamicFriction) / 2
        }
    }

    /**
     * Array to save the contact points of the objects body's in world space.
     */
    val contacts = arrayOf(Vec2(), Vec2())
    var contactNormal = Vec2()
    var contactCount = 0
    var restitution = 0f

    /**
     * Conducts a narrow phase detection and creates a contact manifold.
     */
    fun narrowPhase() {
        if (a !is CollisionBodyInterface || b !is CollisionBodyInterface) return

        if (a is PhysicalBodyInterface && b is PhysicalBodyInterface) {
            restitution = a.restitution.coerceAtMost(b.restitution)
        }
        if (a.shape is Circle && b.shape is Circle) {
            circleCircleCollision(a, b)
        } else if (a.shape is Circle && b.shape is Polygon) {
            circlePolygonCollision(a, b)
        } else if (a.shape is Polygon && b.shape is Circle) {
            circlePolygonCollision(b, a)
            if (contactCount > 0) {
                contactNormal.unaryMinus()
            }
        } else if (a.shape is Polygon && b.shape is Polygon) {
            polygonPolygonCollision(a, b)
        }
    }

    private var penetration = SceneUnit.Zero

    /**
     * Circle vs circle collision detection method
     */
    private fun circleCircleCollision(a: CollisionBodyInterface, b: CollisionBodyInterface) {
        val ca = a.shape as Circle
        val cb = b.shape as Circle
        val normal = b.position.minus(a.position)
        val distance = normal.length()
        val radius = ca.radius + cb.radius
        if (distance >= radius) {
            contactCount = 0
            return
        }
        contactCount = 1
        if (distance == SceneUnit.Zero) {
            penetration = radius
            contactNormal = Vec2(0.sceneUnit, 1.sceneUnit)
            contacts[0].set(a.position.toVec2())
        } else {
            penetration = radius - distance
            contactNormal = normal.toVec2().normalize()
            contacts[0].set(contactNormal.scalar(ca.radius) + a.position.toVec2())
        }
    }

    /**
     * Circle vs Polygon collision detection method
     *
     * @param circleBody Circle object
     * @param polygonBody Polygon Object
     */
    private fun circlePolygonCollision(circleBody: CollisionBodyInterface, polygonBody: CollisionBodyInterface) {
        val circle = circleBody.shape as Circle
        val polygon = polygonBody.shape as Polygon

        //Transpose effectively removes the rotation thus allowing the OBB vs OBB detection to become AABB vs OBB
        val distOfBodies = circleBody.position.minus(polygonBody.position)
        val polyToCircleVec = polygon.orientation.transpose().mul(distOfBodies)
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
            contactCount = 1
            contactNormal = polygon.orientation.mul((vector1 - polyToCircleVec).toVec2().normalize())
            contacts[0] = polygon.orientation.mul(vector1).plus(polygonBody.position).toVec2()
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
            contactCount = 1
            contactNormal = polygon.orientation.mul(vector2.minus(polyToCircleVec).toVec2().normalize())
            contacts[0] = polygon.orientation.mul(vector2).plus(polygonBody.position).toVec2()
        } else {
            val distFromEdgeToCircle = polyToCircleVec.minus(vector1).dot(polygon.normals[faceNormalIndex])
            if (distFromEdgeToCircle >= circle.radius) {
                return
            }
            this.penetration = circle.radius - distFromEdgeToCircle
            contactCount = 1
            contactNormal = polygon.orientation.mul(polygon.normals[faceNormalIndex]).toVec2()
            val circleContactPoint = circleBody.position.plus(contactNormal.unaryMinus().scalar(circle.radius).toSceneOffset())
            contacts[0].set(circleContactPoint.toVec2())
        }
    }

    /**
     * Polygon collision check
     */
    private fun polygonPolygonCollision(a: CollisionBodyInterface, b: CollisionBodyInterface) {
        val pa = a.shape as Polygon
        val pb = b.shape as Polygon
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
            (incidentPoly.orientation.mul(incidentPoly.vertices[incidentIndex]) + incidentPoly.body.position).toVec2(),
            (incidentPoly.orientation.mul(incidentPoly.vertices[if (incidentIndex + 1 >= incidentPoly.vertices.size) 0 else incidentIndex + 1]) + incidentPoly.body.position).toVec2(),
        )

        //Gets vertex's of reference polygon reference face in world space
        var v1 = referencePoly.vertices[referenceFaceIndex]
        var v2 =
            referencePoly.vertices[if (referenceFaceIndex + 1 == referencePoly.vertices.size) 0 else referenceFaceIndex + 1]

        //Rotate and translate vertex's of reference poly
        v1 = referencePoly.orientation.mul(v1) + referencePoly.body.position
        v2 = referencePoly.orientation.mul(v2) + referencePoly.body.position
        val refTangent = v2.minus(v1).normalize()
        val negSide = -refTangent.dot(v1)
        val posSide = refTangent.dot(v2)
        // Clips the incident face against the reference
        var np = clip((-refTangent).toVec2(), negSide, incidentFaceVertexes)
        if (np < 2) {
            return
        }
        np = clip(refTangent.toVec2(), posSide, incidentFaceVertexes)
        if (np < 2) {
            return
        }
        val refFaceNormal = -refTangent.normal()
        val contactVectorsFound = MutableList(2) { Vec2() }
        var totalPen = SceneUnit.Zero
        var contactsFound = 0

        //Discards points that are positive/above the reference face
        for (i in 0..1) {
            val separation = refFaceNormal.dot(incidentFaceVertexes[i].toSceneOffset()) - refFaceNormal.dot(v1)
            if (separation <= SceneUnit.Zero) {
                contactVectorsFound[contactsFound] = incidentFaceVertexes[i]
                totalPen += -separation
                contactsFound++
            }
        }
        val contactPoint: Vec2?
        if (contactsFound == 1) {
            contactPoint = contactVectorsFound[0]
            penetration = totalPen
        } else {
            contactPoint = contactVectorsFound[1].plus(contactVectorsFound[0]).scalar(0.5f)
            penetration = totalPen / 2
        }
        contactCount = 1
        contacts[0].set(contactPoint)
        contactNormal.set((if (flip) refFaceNormal.unaryMinus() else refFaceNormal).toVec2())
    }

    /**
     * Clipping for polygon collisions. Clips incident face against side planes of the reference face.
     *
     * @param planeTangent Plane to clip against
     * @param offset       Offset for clipping in world space to incident face.
     * @param incidentFace Clipped face vertex's
     * @return Number of clipped vertex's
     */
    private fun clip(planeTangent: Vec2, offset: SceneUnit, incidentFace: Array<Vec2>): Int {
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

    /**
     * Finds the incident face of polygon A in object space relative to polygons B position.
     *
     * @param data Data obtained from earlier penetration test.
     * @param A    Polygon A to test.
     * @param B    Polygon B to test.
     */
    private fun findAxisOfMinPenetration(data: AxisData, A: Polygon, B: Polygon) {
        var distance = (-Float.MAX_VALUE).sceneUnit
        var bestIndex = 0
        for (i in A.vertices.indices) {
            //Applies polygon A's orientation to its normals for calculation.
            val polyANormal = A.orientation.mul(A.normals[i])

            //Rotates the normal by the clock wise rotation matrix of B to put the normal relative to the object space of polygon B
            //Polygon b is axis aligned and the normal is located according to this in the correct position in object space
            val objectPolyANormal = B.orientation.transpose().mul(polyANormal)
            var bestProjection = Float.MAX_VALUE.sceneUnit
            var bestVertex = B.vertices[0]

            //Finds the index of the most negative vertex relative to the normal of polygon A
            for (x in B.vertices.indices) {
                val vertex = B.vertices[x]
                val projection = vertex.dot(objectPolyANormal)
                if (projection < bestProjection) {
                    bestVertex = vertex
                    bestProjection = projection
                }
            }

            //Distance of B to A in world space space
            val distanceOfBA = A.body.position.minus(B.body.position)

            //Best vertex relative to polygon B in object space
            val polyANormalVertex = B.orientation.transpose().mul(A.orientation.mul(A.vertices[i]) + distanceOfBA)

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
        val penetrationTolerance = penetration - Physics.PenetrationAllowance
        if (penetrationTolerance <= SceneUnit.Zero) {
            return
        }
        if (a !is PhysicalBodyInterface || b !is PhysicalBodyInterface) return
        val totalMass = a.mass + b.mass
        val correction = penetrationTolerance * Physics.PENETRATION_CORRECTION / totalMass
        a.position = a.position + contactNormal.scalar(-a.mass.sceneUnit * correction).toSceneOffset()
        b.position = b.position + contactNormal.scalar(b.mass.sceneUnit * correction).toSceneOffset()
    }

    /**
     * Solves the current contact manifold and applies impulses based on any contacts found.
     */
    fun solve() {
        val contactA = contacts[0].minus(a.position.toVec2())
        val contactB = contacts[0].minus(b.position.toVec2())

        if (a !is PhysicalBodyInterface || b !is PhysicalBodyInterface) return

        //Relative velocity created from equation found in GDC talk of box2D lite.
        var relativeVel = b.velocity.plus(contactB.cross(b.angularVelocity)).minus(a.velocity).minus(
            contactA.cross(
                a.angularVelocity
            )
        )

        //Positive = converging Negative = diverging
        val contactVel = relativeVel.dot(contactNormal)

        //Prevents objects colliding when they are moving away from each other.
        //If not, objects could still be overlapping after a contact has been resolved and cause objects to stick together
        if (contactVel >= SceneUnit.Zero) {
            return
        }
        val acn = contactA.cross(contactNormal)
        val bcn = contactB.cross(contactNormal)
        val inverseMassSum = a.invMass + b.invMass + acn * acn * a.invInertia + bcn * bcn * b.invInertia
        var j = -(restitution.sceneUnit + SceneUnit.Unit) * contactVel
        j /= inverseMassSum
        val impulse = contactNormal.scalar(j)
        b.applyLinearImpulse(impulse, contactB)
        a.applyLinearImpulse(impulse.copyNegative(), contactA)
        relativeVel = b.velocity.plus(contactB.cross(b.angularVelocity)).minus(a.velocity).minus(
            contactA.cross(
                a.angularVelocity
            )
        )
        val t = relativeVel.copy()
        t.add(contactNormal.scalar(-relativeVel.dot(contactNormal))).normalize()
        var jt = -relativeVel.dot(t)
        jt /= inverseMassSum
        val tangentImpulse: Vec2 = if (abs(jt.raw).sceneUnit < j * staticFriction) {
            t.scalar(jt)
        } else {
            t.scalar(j).scalar(-dynamicFriction)
        }
        b.applyLinearImpulse(tangentImpulse, contactB)
        a.applyLinearImpulse(tangentImpulse.copyNegative(), contactA)
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