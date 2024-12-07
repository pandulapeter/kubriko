package com.pandulapeter.kubriko.physics.implementation.physics.joints

import com.pandulapeter.kubriko.physics.implementation.physics.dynamics.Body
import com.pandulapeter.kubriko.physics.implementation.physics.math.Mat2
import com.pandulapeter.kubriko.physics.implementation.physics.math.Vec2
import com.pandulapeter.kubriko.types.SceneUnit

/**
 * Class for a joint between two bodies.
 */
class JointToBody
/**
 * Constructor for a joint between two bodies.
 *
 * @param body1            First body the joint is attached to
 * @param body2            Second body the joint is attached to
 * @param jointLength   The desired distance of the joint between two points/bodies
 * @param jointConstant The strength of the joint
 * @param dampening     The dampening constant to use for the joints forces
 * @param canGoSlack    Boolean whether the joint can go slack or not
 * @param offset1       Offset to be applied to the location of the joint relative to b1's object space
 * @param offset2       Offset to be applied to the location of the joint relative to b2's object space
 */(
    body1: Body,
    private val body2: Body,
    jointLength: SceneUnit,
    jointConstant: Float,
    dampening: Float,
    canGoSlack: Boolean,
    offset1: Vec2,
    private val offset2: Vec2
) : Joint(body1, jointLength, jointConstant, dampening, canGoSlack, offset1) {
    private var object2AttachmentPoint: Vec2 = body2.position + Mat2(body2.orientation).mul(offset2)

    /**
     * Applies tension to the two bodies.
     */
    override fun applyTension() {
        val mat1 = Mat2(body.orientation)
        object1AttachmentPoint = body.position + mat1.mul(offset)
        val mat2 = Mat2(body2.orientation)
        object2AttachmentPoint = body2.position + mat2.mul(offset2)
        val tension = calculateTension()
        val distance = object2AttachmentPoint.minus(object1AttachmentPoint)
        distance.normalize()
        val impulse = distance.scalar(tension)
        body.applyLinearImpulse(impulse, object1AttachmentPoint.minus(body.position))
        body2.applyLinearImpulse(impulse.copyNegative(), object2AttachmentPoint.minus(body2.position))
    }

    /**
     * Calculates tension between the two attachment points of the joints bodies.
     *
     * @return double value of the tension force between the two bodies attachment points
     */
    override fun calculateTension(): SceneUnit {
        val distance = object1AttachmentPoint.minus(object2AttachmentPoint).length()
        if (distance < naturalLength && canGoSlack) {
            return SceneUnit.Zero
        }
        val extensionRatio = distance - naturalLength
        val tensionDueToHooksLaw = extensionRatio * springConstant
        val tensionDueToMotionDamping = rateOfChangeOfExtension() * dampeningConstant
        return tensionDueToHooksLaw + tensionDueToMotionDamping
    }

    /**
     * Determines the rate of change between two objects.
     *
     * @return double value of the rate of change
     */
    override fun rateOfChangeOfExtension(): SceneUnit {
        val distance = object2AttachmentPoint.minus(object1AttachmentPoint)
        distance.normalize()
        val relativeVelocity = body2.velocity.plus(
            object2AttachmentPoint.minus(body2.position).cross(body2.angularVelocity)
        ).minus(body.velocity).minus(
            object1AttachmentPoint.minus(body.position).cross(body.angularVelocity)
        )
        return relativeVelocity.dot(distance)
    }
}