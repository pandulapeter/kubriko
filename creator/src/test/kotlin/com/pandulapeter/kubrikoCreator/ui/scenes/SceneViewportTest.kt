/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.scenes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.ExperimentalTestApi
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class SceneViewportTest {

    private class Host : ScenePictureHost {
        var picture: ScenePicture? = null

        override fun provide(picture: ScenePicture) {
            this.picture = picture
        }

        override fun withdraw(picture: ScenePicture) {
            if (this.picture === picture) this.picture = null
        }
    }

    /**
     * Fills the scene's top left quarter of the viewport with [color], the camera being at the scene's origin.
     */
    private class Backdrop(private val color: Color) : Visible {
        override val body = BoxBody(initialPosition = SceneOffset(-5_000f.sceneUnit, -5_000f.sceneUnit), initialSize = SceneSize(10_000f.sceneUnit, 10_000f.sceneUnit))

        override fun DrawScope.draw() = drawRect(color, size = body.size.raw)
    }

    private fun Image.colorAt(x: Int, y: Int) = Bitmap.makeFromImage(this).getColor(x, y)

    @Test
    fun aPictureShowsTheSceneAsDrawnWhileTheViewportIsShown() = uiTest {
        val host = Host()
        val actorManager = ActorManager.newInstance(initialActors = listOf(Backdrop(Color.Red)))
        val kubriko = Kubriko.newInstance(actorManager)
        var isShown by mutableStateOf(true)
        showing {
            if (isShown) SceneViewport(remember { kubriko }, host, background = Color.Blue)
        }
        waitUntil(timeoutMillis = 5_000) { actorManager.allActors.value.isNotEmpty() }
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        val png = assertNotNull(runBlocking { assertNotNull(host.picture).png() })
        saveSnapshot("scene-viewport")
        val image = Image.makeFromEncoded(png)
        assertEquals(Color.Red.toArgb(), image.colorAt(image.width / 4, image.height / 4))
        assertEquals(Color.Blue.toArgb(), image.colorAt(image.width * 3 / 4, image.height * 3 / 4))
        isShown = false
        waitForIdle()
        assertNull(host.picture)
        kubriko.dispose()
    }

    @Test
    fun aWidePictureIsScaledDownToTheGamesScreenshotWidth() {
        val wide = ImageBitmap(2_560, 400)
        val image = Image.makeFromEncoded(assertNotNull(ScenePicture.pngOf(wide)))
        assertEquals(ScenePicture.MAXIMUM_WIDTH, image.width)
        assertEquals(200, image.height)
        assertEquals(640, Image.makeFromEncoded(assertNotNull(ScenePicture.pngOf(ImageBitmap(640, 480)))).width)
    }
}
