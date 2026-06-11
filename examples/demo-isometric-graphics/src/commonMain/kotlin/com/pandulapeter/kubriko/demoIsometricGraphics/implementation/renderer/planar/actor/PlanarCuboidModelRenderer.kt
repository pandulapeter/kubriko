/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.planar.actor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Group
import com.pandulapeter.kubriko.helpers.extensions.rad
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.helpers.extensions.shortestDeltaTo
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneUnit
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.data.Cuboid
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.data.CuboidAnimation
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.data.RenderableCuboidModel
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.data.actor.MiniMapMarker
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.data.animation.PreparedCuboidTracks
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.data.animation.applyTo
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.data.animation.buildTracks
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.planar.model.ProjectionPlane

open class PlanarCuboidModelRenderer(
    renderableCuboidModel: RenderableCuboidModel,
    private val projectionPlane: ProjectionPlane,
    val textureResolver: (String) -> ImageBitmap?,
    protected val miniMapMarker: MiniMapMarker? = null,
    protected val isPreferredMiniMapMarker: (String) -> Boolean = { true },
    isDrawn: Boolean = true,
    private val isStatic: Boolean = false,
) : Group, Dynamic {

    // Each instance owns independent Cuboid copies so animation mutations don't bleed across
    // instances that share the same CuboidModel (e.g. multiple NPCs using the same model file).
    val renderableCuboidModel = renderableCuboidModel.copy(
        cuboidModel = renderableCuboidModel.cuboidModel.copy(
            cuboids = renderableCuboidModel.cuboidModel.cuboids.entries
                .associateTo(LinkedHashMap()) { (id, cuboid) -> id to cuboid.copy() }
        )
    )

    override val actors = this.renderableCuboidModel.cuboidModel.cuboids.map { (id, cuboid) ->
        PlanarCuboidRenderer(
            modelId = renderableCuboidModel.id,
            cuboidId = id,
            cuboid = cuboid,
            projectionPlane = projectionPlane,
            textureResolver = textureResolver,
            initialModelPositionX = renderableCuboidModel.position.x,
            initialModelPositionY = renderableCuboidModel.position.y,
            initialModelPositionZ = renderableCuboidModel.position.z,
            initialModelRotationZ = renderableCuboidModel.rotationZ,
            miniMapMarker = miniMapMarker,
            isPreferredMiniMapMarker = isPreferredMiniMapMarker,
            isDrawn = isDrawn,
        )
    }

    private class ActiveAnimation(
        val name: String,
        val animation: CuboidAnimation,
        val tracks: Map<String, PreparedCuboidTracks>,
        // Cuboid copies taken at the moment playAnimation() was called; used as the interpolation base.
        val snapshots: Map<String, Cuboid>,
        val shouldRepeat: Boolean,
        var progressMs: Int = 0,
    )

    // Cuboid snapshots captured at the start of the crossfade (the "from" pose).
    private class Transition(
        val fromSnapshots: Map<String, Cuboid>,
        val durationMs: Int,
        var elapsedMs: Int = 0,
    )

    private val activeAnimations = mutableListOf<ActiveAnimation>()
    private var activeTransition: Transition? = null

    // Keyframe data is immutable at runtime, so tracks are built once per animation name and
    // reused across repeated playAnimation() calls (building is allocation-heavy).
    private val trackCache = mutableMapOf<String, Map<String, PreparedCuboidTracks>>()

    // Starts playing the animation with the given [name] (matched by CuboidAnimation.name).
    // If the animation is already playing it restarts from the beginning with a fresh snapshot.
    // Tracks are precomputed synchronously; both this function and advanceAnimation() must be
    // called from the same thread (e.g. the game update thread).
    // If more than one animation is active and they both key the same property, the one advanced
    // last in a frame wins — callers control priority through the order of advanceAnimation() calls.
    protected fun playAnimation(name: String, shouldRepeat: Boolean) {
        val animation = renderableCuboidModel.cuboidModel.cuboidAnimations.values
            .firstOrNull { it.name == name } ?: return
        val snapshots = renderableCuboidModel.cuboidModel.cuboids.mapValues { (_, cuboid) -> cuboid.copy() }
        val tracks = trackCache.getOrPut(name) { animation.buildTracks() }
        activeAnimations.removeAll { it.name == name }
        activeAnimations.add(ActiveAnimation(name, animation, tracks, snapshots, shouldRepeat))
    }

    // Starts playing [name] with a smooth crossfade from the current visual pose.
    // Captures a snapshot of all cuboids at call time as the "from" state, then blends linearly
    // toward the new animation output over [durationMs] real milliseconds (not animation-time).
    // If a crossfade is already in progress the snapshot captures the mid-blend pose, so rapid
    // switches between animations remain visually continuous.
    protected fun crossfadeToAnimation(name: String, shouldRepeat: Boolean, durationMs: Int) {
        val fromSnapshots = renderableCuboidModel.cuboidModel.cuboids.mapValues { (_, cuboid) -> cuboid.copy() }
        playAnimation(name, shouldRepeat)
        activeTransition = Transition(fromSnapshots, durationMs)
    }

    // Advances the named animation by [deltaTimeInMilliseconds] and applies the interpolated
    // values to the model's cuboids. Meant to be called from update(); subclasses decide when.
    // Animations that finish without repeating are automatically removed.
    // If a crossfade transition is active its blend is applied on top of the animation output.
    protected fun advanceAnimation(name: String, deltaTimeInMilliseconds: Int) {
        val active = activeAnimations.find { it.name == name } ?: return
        val rawProgress = active.progressMs + deltaTimeInMilliseconds
        val shouldRemove = !active.shouldRepeat && rawProgress >= active.animation.length
        active.progressMs = when {
            shouldRemove -> active.animation.length
            active.animation.length > 0 -> rawProgress % active.animation.length
            else -> 0
        }
        active.tracks.applyTo(
            cuboids = renderableCuboidModel.cuboidModel.cuboids,
            snapshots = active.snapshots,
            timestamp = active.progressMs,
        )
        activeTransition?.let { transition ->
            val alpha = (transition.elapsedMs.toFloat() / transition.durationMs).coerceIn(0f, 1f)
            blendCuboids(renderableCuboidModel.cuboidModel.cuboids, transition.fromSnapshots, alpha)
        }
        if (shouldRemove) {
            activeAnimations.remove(active)
        }
    }

    // Set once a static model's pose has reached its cuboid renderers; from then on the per-cuboid
    // synchronization is skipped entirely (scenery never moves, so ticking it is pure overhead).
    // Starting an animation re-enters the loop through the activeAnimations check below.
    private var hasAppliedStaticPose = false

    override fun update(deltaTimeInMilliseconds: Int) {
        activeTransition?.let { transition ->
            transition.elapsedMs = (transition.elapsedMs + deltaTimeInMilliseconds).coerceAtMost(transition.durationMs)
            if (transition.elapsedMs >= transition.durationMs) {
                activeTransition = null
            }
        }
        if (isStatic && hasAppliedStaticPose && activeAnimations.isEmpty() && activeTransition == null) return
        hasAppliedStaticPose = true
        for (i in actors.indices) {
            actors[i].update(
                modelPositionX = renderableCuboidModel.position.x,
                modelPositionY = renderableCuboidModel.position.y,
                modelPositionZ = renderableCuboidModel.position.z,
                modelRotationZ = renderableCuboidModel.rotationZ,
            )
        }
    }

    private fun blendCuboids(cuboids: Map<String, Cuboid>, fromSnapshots: Map<String, Cuboid>, alpha: Float) {
        cuboids.forEach { (id, cuboid) ->
            val from = fromSnapshots[id] ?: return@forEach
            cuboid.positionX = lerpSceneUnit(from.positionX, cuboid.positionX, alpha)
            cuboid.positionY = lerpSceneUnit(from.positionY, cuboid.positionY, alpha)
            cuboid.positionZ = lerpSceneUnit(from.positionZ, cuboid.positionZ, alpha)
            cuboid.sizeX = lerpSceneUnit(from.sizeX, cuboid.sizeX, alpha)
            cuboid.sizeY = lerpSceneUnit(from.sizeY, cuboid.sizeY, alpha)
            cuboid.sizeZ = lerpSceneUnit(from.sizeZ, cuboid.sizeZ, alpha)
            cuboid.rotationX = lerpAngle(from.rotationX, cuboid.rotationX, alpha)
            cuboid.rotationY = lerpAngle(from.rotationY, cuboid.rotationY, alpha)
            cuboid.rotationZ = lerpAngle(from.rotationZ, cuboid.rotationZ, alpha)
            cuboid.colorXPlus = lerpColor(from.colorXPlus, cuboid.colorXPlus, alpha)
            cuboid.colorXMinus = lerpColor(from.colorXMinus, cuboid.colorXMinus, alpha)
            cuboid.colorYPlus = lerpColor(from.colorYPlus, cuboid.colorYPlus, alpha)
            cuboid.colorYMinus = lerpColor(from.colorYMinus, cuboid.colorYMinus, alpha)
            cuboid.colorZPlus = lerpColor(from.colorZPlus, cuboid.colorZPlus, alpha)
            cuboid.colorZMinus = lerpColor(from.colorZMinus, cuboid.colorZMinus, alpha)
        }
    }

    private fun lerpFloat(from: Float, to: Float, alpha: Float) = from + alpha * (to - from)

    private fun lerpSceneUnit(from: SceneUnit, to: SceneUnit, alpha: Float): SceneUnit =
        lerpFloat(from.raw, to.raw, alpha).sceneUnit

    private fun lerpAngle(from: AngleRadians, to: AngleRadians, alpha: Float): AngleRadians =
        (from.raw + alpha * from.shortestDeltaTo(to).raw).rad

    private fun lerpColor(from: Color?, to: Color?, alpha: Float): Color? = when {
        from == null && to == null -> null
        from == null || to == null -> if (alpha < 0.5f) from else to
        else -> Color(
            red = lerpFloat(from.red, to.red, alpha).coerceIn(0f, 1f),
            green = lerpFloat(from.green, to.green, alpha).coerceIn(0f, 1f),
            blue = lerpFloat(from.blue, to.blue, alpha).coerceIn(0f, 1f),
            alpha = lerpFloat(from.alpha, to.alpha, alpha).coerceIn(0f, 1f),
        )
    }
}
