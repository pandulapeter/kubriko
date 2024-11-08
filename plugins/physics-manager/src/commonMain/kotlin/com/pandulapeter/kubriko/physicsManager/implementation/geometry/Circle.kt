package com.pandulapeter.kubriko.physicsManager.implementation.geometry

import com.pandulapeter.kubriko.physicsManager.implementation.collision.AxisAlignedBoundingBox
import com.pandulapeter.kubriko.physicsManager.implementation.dynamics.bodies.PhysicalBodyInterface
import com.pandulapeter.kubriko.physicsManager.implementation.geometry.bodies.TranslatableBody
import com.pandulapeter.kubriko.physicsManager.implementation.math.Vec2
import kotlin.math.PI
import kotlin.math.sqrt

/**
 * Circle class to create a circle object.
 */
class Circle
/**
 * Constructor for a circle.
 *
 * @param radius Desired radius of the circle.
 */(var radius: Double) : Shape() {
    /**
     * Calculates the mass of a circle.
     *
     * @param density The desired density to factor into the calculation.
     */
    override fun calcMass(density: Double) {
        val physicalBody = this.body
        if (physicalBody !is PhysicalBodyInterface) return
        physicalBody.mass = PI * radius * radius * density
        physicalBody.invMass = if (physicalBody.mass != 0.0) 1.0f / physicalBody.mass else 0.0
        physicalBody.inertia = physicalBody.mass * radius * radius
        physicalBody.invInertia = if (physicalBody.inertia != 0.0) 1.0f / physicalBody.inertia else 0.0
    }

    /**
     * Generates an AABB and binds it to the body.
     */
    override fun createAABB() {
        this.body.aabb = AxisAlignedBoundingBox(
            Vec2(-radius, -radius),
            Vec2(radius, radius)
        )
    }

    /**
     * Method to check if point is inside a body in world space.
     *
     * @param startPoint Vector point to check if its inside the first body.
     * @return boolean value whether the point is inside the first body.
     */
    override fun isPointInside(startPoint: Vec2): Boolean {
        val d = this.body.position.minus(startPoint)
        return d.length() <= radius
    }

    override fun rayIntersect(startPoint: Vec2, endPoint: Vec2, maxDistance: Double, rayLength: Double): IntersectionReturnElement {
        var minPx = 0.0
        var minPy = 0.0
        var intersectionFound = false
        var closestBody: TranslatableBody? = null
        var maxD = maxDistance

        val ray = endPoint.copy().minus(startPoint)
        val circleCenter = body.position.copy()
        val r = radius
        val difInCenters = startPoint.minus(circleCenter)
        val a = ray.dot(ray)
        val b = 2 * difInCenters.dot(ray)
        val c = difInCenters.dot(difInCenters) - r * r
        var discriminant = b * b - 4 * a * c
        if (discriminant >= 0) {
            discriminant = sqrt(discriminant)
            val t1 = (-b - discriminant) / (2 * a)
            if (t1 in 0.0..1.0) {
                if (t1 < maxDistance) {
                    maxD = t1
                    minPx = startPoint.x + endPoint.x * t1
                    minPy = startPoint.y + endPoint.y * t1
                    intersectionFound = true
                    closestBody = body
                }
            }
        }
        return IntersectionReturnElement(minPx, minPy, intersectionFound, closestBody, maxD)
    }
}