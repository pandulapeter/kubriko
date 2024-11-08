package com.pandulapeter.kubriko.physics.implementation.physics.collision

import com.pandulapeter.kubriko.physics.implementation.physics.collision.bodies.CollisionBodyInterface
import com.pandulapeter.kubriko.physics.implementation.physics.math.Vec2

/**
 * Axis aligned bounding box volume class. Allows the creation of bounding volumes to make broad phase collision check possible and easy to do.
 *
 * Constructor to generate an AABB given a minimum and maximum bound in the form of two vectors.
 *
 * @param min Lower bound of AABB vertex.
 * @param max Higher bound of AABB vertex.
 */
class AxisAlignedBoundingBox(min: Vec2 = Vec2(), max: Vec2 = Vec2()) {
    /**
     * Getter for min variable for lower bound vertex.
     *
     * @return AABB min
     */
    /**
     * Lower left vertex of bounding box.
     */
    var min: Vec2 = min.copy()
        private set
    /**
     * Getter for max variable for upper bound vertex.
     *
     * @return AABB max
     */
    /**
     * Top right vertex of bounding box.
     */
    var max: Vec2 = max.copy()
        private set

    /**
     * Sets the current objects bounds equal to that of the passed AABB argument.
     *
     * @param aabb An AABB bounding box.
     */
    fun set(aabb: AxisAlignedBoundingBox) {
        val v = aabb.min
        min.x = v.x
        min.y = v.y
        val v1 = aabb.max
        max.x = v1.x
        max.y = v1.y
    }

    /**
     * Method to check if an AABB is valid.
     * Makes sure the bounding volume is not; a point, has order of vertex's backwards and valid values have been used for the bounds.
     *
     * @return boolean value of the validity of the AABB.
     */
    val isValid: Boolean
        get() {
            if (max.x - min.x < 0) {
                return false
            }
            return if (max.y - min.y < 0) {
                false
            } else min.isValid && max.isValid
        }

    /**
     * Method to check if a point resides inside an AABB in object space.
     *
     * @param point A point to check if its inside the AABB's object space. Point needs to also be in object space.
     * @return Boolean value whether or not the point lies inside the AABB bounds.
     */
    fun aabbOverlap(point: Vec2): Boolean {
        val x = point.x
        val y = point.y
        return x <= max.x && x >= min.x && y >= max.y && y <= min.y
    }

    /**
     * Method to add offset to the AABB's bounds. Can be useful to convert from object to world space .
     *
     * @param offset A vector to apply to the min and max vectors to translate the bounds and therefore AABB to desired position.
     */
    fun addOffset(offset: Vec2) {
        min.add(offset)
        max.add(offset)
    }

    override fun toString(): String {
        return "AABB[$min . $max]"
    }

    /**
     * Copy method to return a new AABB that's the same as the current object.
     *
     * @return New AABB that's the same as the current object.
     */
    fun copy(): AxisAlignedBoundingBox {
        return AxisAlignedBoundingBox(min, max)
    }

    companion object {
        /**
         * Checks whether two body's AABB's overlap in world space.
         *
         * @param bodyA First body to evaluate.
         * @param bodyB Second body to evaluate.
         * @return Boolean value of whether the two bodies AABB's overlap in world space.
         */

        fun aabbOverlap(bodyA: CollisionBodyInterface, bodyB: CollisionBodyInterface): Boolean {
            val aCopy = bodyA.aabb.copy()
            val bCopy = bodyB.aabb.copy()
            aCopy.addOffset(bodyA.position)
            bCopy.addOffset(bodyB.position)
            return aabbOverlap(aCopy, bCopy)
        }

        /**
         * Method to check if two AABB's overlap. Can be seen as world space.
         *
         * @param boxA First AABB to evaluate.
         * @param boxB Second AABB to evaluate.
         * @return Boolean value of whether two bounds of the AABB's overlap.
         */

        fun aabbOverlap(boxA: AxisAlignedBoundingBox, boxB: AxisAlignedBoundingBox): Boolean {
            return boxA.min.x <= boxB.max.x && boxA.max.x >= boxB.min.x && boxA.min.y <= boxB.max.y && boxA.max.y >= boxB.min.y
        }
    }
}