package com.pandulapeter.kubriko.demoPhysics.implementation

import kubriko.examples.demo_physics.generated.resources.Res
import kubriko.examples.demo_physics.generated.resources.joints
import kubriko.examples.demo_physics.generated.resources.rigid_body_collisions
import org.jetbrains.compose.resources.StringResource

internal enum class PhysicsDemoType(
    val nameStringResource: StringResource,
) {
    RIGID_BODY_COLLISIONS(nameStringResource = Res.string.rigid_body_collisions),
    JOINTS(nameStringResource = Res.string.joints),
}