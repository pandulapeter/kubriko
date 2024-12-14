package com.pandulapeter.kubriko.demoBuiltInShaders.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.demoBuiltInShaders.implementation.actors.ColorfulBox
import com.pandulapeter.kubriko.implementation.extensions.get
import com.pandulapeter.kubriko.implementation.extensions.sceneUnit
import com.pandulapeter.kubriko.implementation.extensions.times
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.shader.collection.ChromaticAberrationShader
import com.pandulapeter.kubriko.shader.collection.RippleShader
import com.pandulapeter.kubriko.shader.collection.SmoothPixelationShader
import com.pandulapeter.kubriko.shader.collection.VignetteShader
import com.pandulapeter.kubriko.types.SceneOffset

internal class BuiltInShadersDemoManager : Manager() {

    override fun onInitialize(kubriko: Kubriko) {
        kubriko.get<ActorManager>().add(
            (-5..5).flatMap { y ->
                (-5..5).map { x ->
                    ColorfulBox(
                        initialPosition = SceneOffset(
                            x = x * 100.sceneUnit,
                            y = y * 100.sceneUnit,
                        ),
                        hue = (0..360).random().toFloat(),
                    )
                }
            }
                    + SmoothPixelationShader()
                    + VignetteShader()
                    + RippleShader()
                    + ChromaticAberrationShader()
        )
    }
}