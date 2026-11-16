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
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
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
    fun onlyTheActorsOfTheSceneShownMoveTheCameraAndARemovedOneIsLetGoFor() {
        assertContains(hero, "class Hero private constructor(state: State) : Batched, Dynamic, Editable<Hero>, GeneratedScenes.CameraFollower {\n")
        assertContains(hero, "    @kotlin.concurrent.Volatile\n    final override var sceneChangesWhenAdded = 0\n        private set\n")
        assertContains(hero, "        sceneChangesWhenAdded = generatedScenes.sceneChanges\n")
        assertContains(hero, "generatedScenes.followWithCamera(this)\n")
        assertContains(hero, "    override fun onRemoved() {\n        sceneChangesWhenAdded = GeneratedScenes.NOT_IN_SCENE\n        generatedScenes.unfollowWithCamera(this)\n    }\n")
        assertContains(scenes, "        if (isInSceneEditor || cameraTarget === actor) return\n        withCameraLock {\n            if (!isOfSceneShown(actor)) return@withCameraLock\n            cameraTarget = actor\n")
        assertContains(scenes, "caller == null || caller.sceneChangesWhenAdded == sceneChanges\n")
    }

    @Test
    fun aNewSceneStopsTheFollowingAndTheShakeUnderTheLock() {
        assertContains(scenes, "    private fun startScene() = withCameraLock {\n        sceneChanges++\n        cameraTarget = null\n        stopShake()\n")
        assertContains(scenes, "            if (!isFirst) {\n                startScene()\n")
    }

    @Test
    fun anActorsCameraBlocksPassTheActorAndAManagersDont() {
        val manager = ManagerTypeCodeGenerator.generateManagerType(
            ManagerTypeDefinition(name = "Game", events = listOf(EventDefinition(EventType.GAME_STARTED, blocks = listOf(BlockDefinition.ZoomCamera())))),
            "com.example.game",
        )
        assertContains(manager, "generatedScenes.zoomCamera(1f)\n")
        val zooming = ActorTypeCodeGenerator.generateActorType(
            ActorTypeDefinition(name = "Lens", events = listOf(EventDefinition(EventType.ADDED_TO_SCENE, blocks = listOf(BlockDefinition.ZoomCamera())))),
            "com.example.game",
        )
        assertContains(zooming, "generatedScenes.zoomCamera(1f, this)\n")
    }

    @Test
    fun aShakeHoldsStillWhileTheGameIsntRunningWhileFollowingGoesOnAndNeitherCostsAnythingWhileIdle() {
        assertContains(scenes, "                if (isRunning) {\n                    shakeRemainingInMilliseconds -= deltaTimeInMilliseconds\n")
        assertContains(scenes, "                placeShakenCamera(cameraBaseX + shakeOffsetX, cameraBaseY + shakeOffsetY)\n")
        assertContains(scenes, "            if (target == null && shakenCameraX.isNaN() && shakeRemainingInMilliseconds <= 0f) return\n")
    }

    @Test
    fun aShakeShakesAroundWhereverSomethingElseMovedTheCamera() {
        assertContains(scenes, "                if (position.x.raw != shakenCameraX || position.y.raw != shakenCameraY) {\n                    cameraBaseX = position.x.raw\n")
    }

    @Test
    fun theZoomIsTheViewportsOwnAndLastsAcrossScenes() {
        assertContains(scenes, "        withCameraLock { if (isOfSceneShown(caller)) viewportManager.setScaleFactor(scaleFactor) }\n")
        assertFalse("BeforeZoom" in scenes)
    }

    @Test
    fun aMoveToAPositionThatIsntANumberIsLeftOut() {
        assertContains(scenes, "        if (isInSceneEditor || !x.isFinite() || !y.isFinite()) return\n")
    }

    @Test
    fun noStrengthOrNoTimeStopsTheShakeAndAnInfiniteOneIsLeftOut() {
        assertContains(scenes, "            if (strength <= 0f || durationInMilliseconds <= 0f) {\n                stopShake()\n")
        assertContains(scenes, "        if (isInSceneEditor || !strength.isFinite() || !durationInMilliseconds.isFinite()) return\n")
    }

    @Test
    fun aStoppedShakeLeavesNoOffsetForTheNextShakeOrMove() {
        assertContains(scenes, "    private fun stopShake() {\n        shakeRemainingInMilliseconds = 0f\n        shakeOffsetX = 0f\n        shakeOffsetY = 0f\n    }\n")
        assertContains(scenes, "                return@withCameraLock\n            }\n            if (shakeRemainingInMilliseconds <= 0f) stopShake()\n")
    }

    @Test
    fun whilePausedTheCameraFollowsOnlyAnActorThatMovesAndANewFollowAlwaysPlacesIt() {
        assertContains(scenes, "                if (!isRunning && x == cameraBaseX && y == cameraBaseY && shakenCameraX.isNaN()) return\n")
        assertContains(scenes, "            cameraTarget = actor\n            cameraBaseX = Float.NaN\n")
    }

    @Test
    fun aSceneFittingTheViewShowsItsWholeBoundsWhateverTheZoom() {
        assertContains(scenes, "width = width.sceneUnit))\n        viewportManager.setScaleFactor(1f)\n")
    }
}
