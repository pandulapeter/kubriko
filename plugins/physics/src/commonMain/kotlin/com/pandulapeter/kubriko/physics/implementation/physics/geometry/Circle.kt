package com.pandulapeter.kubriko.physics.implementation.physics.geometry

import com.pandulapeter.kubriko.implementation.extensions.sceneUnit
import com.pandulapeter.kubriko.physics.implementation.physics.collision.AxisAlignedBoundingBox
import com.pandulapeter.kubriko.physics.implementation.physics.dynamics.bodies.PhysicalBodyInterface
import com.pandulapeter.kubriko.physics.implementation.physics.geometry.bodies.TranslatableBody
import com.pandulapeter.kubriko.physics.implementation.physics.math.Vec2
import com.pandulapeter.kubriko.types.SceneUnit
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
 */(var radius: SceneUnit) : Shape() {
    /**
     * Calculates the mass of a circle.
     *
     * @param density The desired density to factor into the calculation.
     */
    override fun calcMass(density: Float) {
        val physicalBody = this.body
        if (physicalBody !is PhysicalBodyInterface) return
        physicalBody.mass = PI.toFloat() * radius.raw * radius.raw * density
        physicalBody.invMass = if (physicalBody.mass != 0f) 1.0f / physicalBody.mass else 0f
        physicalBody.inertia = physicalBody.mass * radius.raw * radius.raw
        physicalBody.invInertia = if (physicalBody.inertia != 0f) 1.0f / physicalBody.inertia else 0f
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
        val d = body.position.minus(startPoint)
        return d.length() <= radius
    }

    override fun rayIntersect(startPoint: Vec2, endPoint: Vec2, maxDistance: SceneUnit, rayLength: SceneUnit): IntersectionReturnElement {
        var minPx = SceneUnit.Zero
        var minPy = SceneUnit.Zero
        var intersectionFound = false
        var closestBody: TranslatableBody? = null
        var maxD = maxDistance

        val ray = endPoint.copy().minus(startPoint)
        val circleCenter = body.position.copy()
        val r = radius
        val difInCenters = startPoint.minus(circleCenter)
        val a = ray.dot(ray)
        val b = difInCenters.dot(ray) * 2
        val c = difInCenters.dot(difInCenters) - r * r
        var discriminant = b * b - a * c * 4
        if (discriminant >= SceneUnit.Zero) {
            discriminant = sqrt(discriminant.raw).sceneUnit
            val t1 = (-b - discriminant) / (a * 2)
            if (t1.raw in 0.0..1.0) {
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