/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors.codegen

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

/**
 * What `GeneratedScenes` does for the camera Blocks, and what an Actor the camera follows tells it.
 */
class CameraCodeTest {
    private val scenes = ScenesCodeGenerator.generate("com.example.game")
    private val hero = ActorTypeCodeGenerator.generateActorType(
        ActorTypeDefinition(name = "Hero", events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.FollowWithCamera)))),
        "com.example.game",
    )

    @Test
    fun theCameraFollowsOnlyAnActorOfTheSceneShownAndStopsOnceItIsRemoved() {
        assertContains(hero, "        sceneChangesWhenAdded = generatedScenes.sceneChanges\n")
        assertContains(hero, "generatedScenes.followWithCamera(this, sceneChangesWhenAdded)\n")
        assertContains(hero, "    override fun onRemoved() {\n        generatedScenes.unfollowWithCamera(this)\n    }\n")
        assertContains(scenes, "        withCameraLock { if (sceneChangesWhenAdded == sceneChanges) cameraTarget = actor }\n")
        assertContains(scenes, "        if (cameraTarget === actor) cameraTarget = null\n")
    }

    @Test
    fun theCameraStaysStillWhileTheGameIsPausedAndCostsNothingWhileIdle() {
        assertContains(scenes, "    override fun onUpdate(deltaTimeInMilliseconds: Int) {\n        if (isInSceneEditor || !stateManager.isRunning.value) return\n")
        assertContains(scenes, "            if (target == null && shakenCameraX.isNaN() && shakeRemainingInMilliseconds <= 0f) return\n")
    }

    @Test
    fun aShakeShakesAroundWhereverSomethingElseMovedTheCamera() {
        assertContains(scenes, "                if (position.x.raw != shakenCameraX || position.y.raw != shakenCameraY) {\n                    cameraBaseX = position.x.raw\n")
    }

    @Test
    fun theZoomIsTheViewportsOwnAndLastsAcrossScenes() {
        assertContains(scenes, "        if (!isInSceneEditor && scaleFactor > 0f) viewportManager.setScaleFactor(scaleFactor)\n")
        assertFalse("BeforeZoom" in scenes)
    }

    @Test
    fun aMoveToAPositionThatIsntANumberIsLeftOut() {
        assertContains(scenes, "        if (isInSceneEditor || !x.isFinite() || !y.isFinite()) return\n")
    }

    @Test
    fun noStrengthOrNoTimeStopsTheShake() {
        assertContains(scenes, "            if (strength <= 0f || durationInMilliseconds <= 0f) {\n                shakeRemainingInMilliseconds = 0f\n")
    }
}
