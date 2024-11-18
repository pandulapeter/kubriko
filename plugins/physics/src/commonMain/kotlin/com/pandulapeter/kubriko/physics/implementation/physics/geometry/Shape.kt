package com.pandulapeter.kubriko.physics.implementation.physics.geometry

import com.pandulapeter.kubriko.physics.implementation.physics.collision.bodies.CollisionBodyInterface
import com.pandulapeter.kubriko.physics.implementation.physics.geometry.bodies.TranslatableBody
import com.pandulapeter.kubriko.physics.implementation.physics.math.Mat2
import com.pandulapeter.kubriko.physics.implementation.physics.math.Vec2

/**
 * Abstract class presenting a geometric shape.
 */
abstract class Shape {
    lateinit var body: CollisionBodyInterface
    var orientation: Mat2 = Mat2()

    /**
     * Calculates the mass of a shape.
     *
     * @param density The desired density to factor into the calculation.
     */
    abstract fun calcMass(density: Float)

    /**
     * Generates an AABB for the shape.
     */
    abstract fun createAABB()

    abstract fun isPointInside(startPoint: Vec2): Boolean

    /**
     * Checks if a ray intersects with the shape.
     *
     * @param startPoint The start point of the ray.
     * @param endPoint The end point of the ray.
     * @param maxDistance The ray information.
     * @param rayInformation The object to store the information in.
     * @return Float Returns the distance to the intersection point. maxDistance if no intersection was found.
     */
    abstract fun rayIntersect(startPoint: Vec2, endPoint: Vec2, maxDistance: Float, rayLength: Float): IntersectionReturnElement

    class IntersectionReturnElement(val minPx: Float, val minPy: Float, val intersectionFound: Boolean, val closestBody: TranslatableBody?, val maxDistance: Float)
}