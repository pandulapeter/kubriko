package com.pandulapeter.kubriko.physics.implementation.physics.joints

import com.pandulapeter.kubriko.physics.implementation.physics.dynamics.Body
import com.pandulapeter.kubriko.physics.implementation.physics.math.Mat2
import com.pandulapeter.kubriko.physics.implementation.physics.math.Vec2
import com.pandulapeter.kubriko.types.SceneUnit

/**
 * Abstract class for joints holding all the common properties of joints.
 *
 * @param body            A body the joint is attached to
 * @param naturalLength   The desired distance of the joint between two points/bodies
 * @param springConstant The strength of the joint
 * @param dampeningConstant     The dampening constant to use for the joints forces
 * @param canGoSlack    Boolean whether the joint can go slack or not
 * @param offset       Offset to be applied to the location of the joint relative to b1's object space.
 */
abstract class Joint protected constructor(
    protected val body: Body,
    protected val naturalLength: SceneUnit,
    protected val springConstant: Float,
    protected val dampeningConstant: Float,
    protected val canGoSlack: Boolean,
    protected val offset: Vec2
) {
    var object1AttachmentPoint: Vec2 = body.position + Mat2(body.orientation).mul(offset)

    /**
     * Abstract method to apply tension to the joint
     */
    abstract fun applyTension()

    /**
     * Abstract method to calculate tension between the joint
     *
     * @return value of the tension force between two points/bodies
     */
    abstract fun calculateTension(): SceneUnit

    /**
     * Determines the rate of change between two objects/points.
     * @return value of the rate of change
     */
    abstract fun rateOfChangeOfExtension(): SceneUnit
}