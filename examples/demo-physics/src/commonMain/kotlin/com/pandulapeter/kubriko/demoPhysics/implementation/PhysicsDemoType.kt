package com.pandulapeter.kubriko.demoPhysics.implementation

import kubriko.examples.demo_physics.generated.resources.Res
import kubriko.examples.demo_physics.generated.resources.chains
import kubriko.examples.demo_physics.generated.resources.rigid_body_collisions
import org.jetbrains.compose.resources.StringResource

internal enum class PhysicsDemoType(
    val nameStringResource: StringResource,
) {
    RIGID_BODY_COLLISIONS(nameStringResource = Res.string.rigid_body_collisions),
    CHAINS(nameStringResource = Res.string.chains),
}