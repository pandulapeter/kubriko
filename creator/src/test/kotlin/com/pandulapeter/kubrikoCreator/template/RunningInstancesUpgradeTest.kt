/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.template

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RunningInstancesUpgradeTest {

    @Test
    fun theGameAddsItsInstanceWhileItIsShown() {
        val before = "    DisposableEffect(kubriko) {\n        onDispose { kubriko.dispose() }\n    }"
        assertFalse(RunningInstancesUpgrade.isGameDone(before))
        val after = RunningInstancesUpgrade.game(before)
        assertEquals(
            "    DisposableEffect(kubriko) {\n        KubrikoWiring.runningInstances += kubriko\n        onDispose {\n" +
                "            KubrikoWiring.runningInstances -= kubriko\n            kubriko.dispose()\n        }\n    }",
            after,
        )
        assertTrue(RunningInstancesUpgrade.isGameDone(after))
    }

    @Test
    fun theEntryPointReportsTheInstances() {
        val before = "class GameEntryPoint : CreatorEntryPoint {\n    override fun createCustomManagers() = KubrikoWiring.createManagers(isInSceneEditor = true)\n}"
        assertFalse(RunningInstancesUpgrade.isEntryPointDone(before))
        val after = RunningInstancesUpgrade.entryPoint(before)
        assertTrue("\n\n    override fun getRunningInstances() = KubrikoWiring.runningInstances\n}" in after)
        assertTrue(RunningInstancesUpgrade.isEntryPointDone(after))
    }

    @Test
    fun filesUnlikeTheTemplatesAreLeftAsTheyAreAndCountAsDone() {
        val game = "val kubriko = remember { Kubriko.newInstance() }"
        assertEquals(game, RunningInstancesUpgrade.game(game))
        assertTrue(RunningInstancesUpgrade.isGameDone(game))
        assertTrue(RunningInstancesUpgrade.isEntryPointDone("class GameEntryPoint : CreatorEntryPoint"))
    }
}
