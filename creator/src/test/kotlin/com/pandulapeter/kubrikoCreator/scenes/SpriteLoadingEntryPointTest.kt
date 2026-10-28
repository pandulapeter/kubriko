/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.scenes

import com.pandulapeter.kubriko.creator.CreatorEntryPoint
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.sprites.SpriteManager
import com.pandulapeter.kubriko.sprites.SpriteResource
import kotlinx.coroutines.test.runTest
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.InternalResourceApi
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame

@OptIn(InternalResourceApi::class)
class SpriteLoadingEntryPointTest {
    private val directory = createTempDirectory().toFile()
    private val ball = File(directory, "ball.png").apply { ImageIO.write(BufferedImage(6, 4, BufferedImage.TYPE_INT_ARGB), "png", this) }
    private val loader = ProjectSpriteLoader(mapOf("ball" to ball))

    @AfterTest
    fun tearDown() {
        directory.deleteRecursively()
    }

    /**
     * A resource as the project's generated accessor makes it, with the path inside the project's jar.
     */
    private fun projectResource(name: String) = DrawableResource("drawable:$name", emptySet())

    @Test
    fun theProjectsSpriteManagerIsReplacedAndTheOtherManagersAreKept() {
        val other = object : Manager() {}
        val projectSpriteManager = SpriteManager.newInstance()
        val entryPoint = SpriteLoadingEntryPoint(
            entryPoint = object : CreatorEntryPoint {
                override fun createSerializationManager() = throw NotImplementedError()
                override fun createCustomManagers() = listOf(other, projectSpriteManager)
            },
            loader = loader,
        )
        val managers = entryPoint.createCustomManagers()
        assertSame(other, managers[0])
        assertIs<SpriteManager>(managers[1])
        assertNotSame(projectSpriteManager, managers[1])
    }

    @Test
    fun spritesAreFoundByTheNameOfTheirResource() = runTest {
        assertEquals(ball, loader.fileOf(projectResource("ball")))
        assertNull(loader.fileOf(projectResource("paddle")))
        assertNull(loader.load(SpriteResource(projectResource("paddle"))))
        val image = loader.load(SpriteResource(projectResource("ball")))!!
        assertEquals(6 to 4, image.width to image.height)
        val sideways = loader.load(SpriteResource(projectResource("ball"), SpriteResource.Rotation.DEGREES_90))!!
        assertEquals(4 to 6, sideways.width to sideways.height)
    }
}
