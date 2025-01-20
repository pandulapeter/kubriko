package com.pandulapeter.kubriko.particles

import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.ComplexBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.extensions.cos
import com.pandulapeter.kubriko.extensions.get
import com.pandulapeter.kubriko.extensions.sin
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit
import kotlin.math.min

class Particle<T : Any>(
    private val payload: T,
    override val body: ComplexBody,
    override val drawingOrder: Float = 0f,
    private val speed: SceneUnit = SceneUnit.Zero,
    private val direction: AngleRadians = AngleRadians.Zero,
    private val lifespanInMilliseconds: Float,
    private val processBody: ComplexBody.(T, Float) -> Unit = { _, _ -> },
    private val drawParticle: DrawScope.(T, ComplexBody, Float) -> Unit,
) : Visible, Dynamic {
    private lateinit var actorManager: ActorManager
    private var remainingLifespan = lifespanInMilliseconds
    private var currentProgress = 0f

    override fun onAdded(kubriko: Kubriko) {
        actorManager = kubriko.get()
    }

    override fun update(deltaTimeInMilliseconds: Float) {
        // TODO: Move, apply transformations
        if (speed != SceneUnit.Zero) {
            body.position = SceneOffset(
                x = body.position.x + speed * direction.cos,
                y = body.position.y - speed * direction.sin,
            )
        }
        currentProgress = 1f - (remainingLifespan / lifespanInMilliseconds)
        if (currentProgress >= 1) {
            actorManager.remove(this)
        } else {
            body.processBody(payload, currentProgress)
            remainingLifespan -= deltaTimeInMilliseconds
        }
    }

    override fun DrawScope.draw() = drawParticle(payload,body, min(1f, currentProgress))
}