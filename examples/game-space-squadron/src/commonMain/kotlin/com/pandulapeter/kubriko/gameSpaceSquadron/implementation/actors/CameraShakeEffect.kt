/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameSpaceSquadron.implementation.actors

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.extensions.get
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.CameraShakeManager
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.ViewportManager

internal class CameraShakeEffect : Dynamic {
    private lateinit var actorManager: ActorManager
    private lateinit var cameraShakeManager: CameraShakeManager
    private lateinit var viewportManager: ViewportManager
    private var lifespan = 1f

    override fun onAdded(kubriko: Kubriko) {
        actorManager = kubriko.get()
        cameraShakeManager = kubriko.get()
        viewportManager = kubriko.get()
    }

    override fun update(deltaTimeInMilliseconds: Int) {
        if (deltaTimeInMilliseconds > 0) {
            lifespan -= 0.005f * deltaTimeInMilliseconds
            viewportManager.setScaleFactor(1f + (-10..10).random() * viewportManager.size.value.height * 0.000001f)
            cameraShakeManager.setRotation(((-20..20).random()) / 10f)
            if (lifespan <= 0) {
                cameraShakeManager.setRotation(0f)
                viewportManager.setScaleFactor(1f)
                actorManager.remove(this)
            }
        }
    }
}
