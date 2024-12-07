package com.pandulapeter.kubriko.physics.implementation.physics.rays

import com.pandulapeter.kubriko.implementation.extensions.sceneUnit
import com.pandulapeter.kubriko.physics.implementation.physics.collision.bodies.CollisionBodyInterface
import com.pandulapeter.kubriko.physics.implementation.physics.geometry.bodies.TranslatableBody
import com.pandulapeter.kubriko.physics.implementation.physics.math.Vec2
import com.pandulapeter.kubriko.types.SceneUnit

/**
 * Ray class to define and project rays in a world.
 *
 * @param startPoint The origin of the rays projection.
 * @param direction  The direction of the ray points in radians.
 * @param distance   The distance the ray is projected
 */
class Ray(var startPoint: Vec2, direction: Vec2, distance: SceneUnit) {
    val distance: SceneUnit

    /**
     * Gets the direction of the ray in radians.
     *
     * @return direction variable of type Vec2.
     */
    var direction: Vec2

    /**
     * Convenience constructor with ray set at origin. Similar to
     * [.Ray]
     *
     * @param direction The direction of the ray points in radians.
     * @param distance  The distance the ray is projected
     */
    constructor(direction: Float, distance: SceneUnit) : this(
        Vec2(),
        Vec2(direction),
        distance
    )

    /**
     * Convenience constructor with ray set at origin. Similar to
     * [.Ray]
     *
     * @param direction The direction of the ray points.
     * @param distance  The distance the ray is projected
     */
    constructor(direction: Vec2, distance: SceneUnit) : this(Vec2(), direction, distance)

    /**
     * Convenience constructor. Similar to
     * [.Ray]
     *
     * @param startPoint The origin of the rays projection.
     * @param direction  The direction of the ray points in radians.
     * @param distance   The distance the ray is projected
     */
    constructor(startPoint: Vec2, direction: Float, distance: SceneUnit) : this(
        startPoint,
        Vec2(direction),
        distance
    )

    var rayInformation: RayInformation? = null
        private set

    init {
        this.direction = direction.normalized
        this.distance = distance
    }

    /**
     * Updates the projection in world space and acquires information about the closest intersecting object with the ray projection.
     *
     * @param bodiesToEvaluate Arraylist of bodies to check if they intersect with the ray projection.
     */
    fun updateProjection(bodiesToEvaluate: List<TranslatableBody>) {
        rayInformation = null
        val endPoint = direction.scalar(distance).plus(startPoint)
        var minT1 = Float.POSITIVE_INFINITY.sceneUnit
        var minPx = SceneUnit.Zero
        var minPy = SceneUnit.Zero
        var intersectionFound = false
        var closestBody: TranslatableBody? = null
        for (body in bodiesToEvaluate) {
            if (body !is CollisionBodyInterface) continue
            val shape = body.shape
            val intersectionReturnElement = shape.rayIntersect(startPoint, endPoint, minT1, distance)
            if (intersectionReturnElement.intersectionFound) {
                minT1 = intersectionReturnElement.maxDistance
                minPx = intersectionReturnElement.minPx
                minPy = intersectionReturnElement.minPy
                intersectionFound = true
                closestBody = intersectionReturnElement.closestBody
            }
        }
        if (intersectionFound) {
            rayInformation = closestBody?.let { RayInformation(it, minPx, minPy, -1) }
        }
    }
}