/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoPhysics.implementation.ui

import kubriko.examples.demo_physics.generated.resources.Res
import kubriko.examples.demo_physics.generated.resources.chain
import kubriko.examples.demo_physics.generated.resources.explosion
import kubriko.examples.demo_physics.generated.resources.ic_chain
import kubriko.examples.demo_physics.generated.resources.ic_explosion
import kubriko.examples.demo_physics.generated.resources.ic_shape
import kubriko.examples.demo_physics.generated.resources.shape
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

internal enum class ActionType(
    val icon: DrawableResource,
    val contentDescription: StringResource,
) {
    SHAPE(Res.drawable.ic_shape, Res.string.shape),
    CHAIN(Res.drawable.ic_chain, Res.string.chain),
    EXPLOSION(Res.drawable.ic_explosion, Res.string.explosion),
}
