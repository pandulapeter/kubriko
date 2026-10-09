/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sprites

import androidx.compose.ui.graphics.ImageBitmap
import com.pandulapeter.kubriko.testFixtures.awaitCondition
import com.pandulapeter.kubriko.testFixtures.newManualKubriko
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.InternalResourceApi
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpriteManagerRetryTest {

    @Test
    fun failedLoadIsRetriedUntilItSucceeds() {
        val calls = AtomicInteger()
        val spriteManager = SpriteManagerImpl(
            isLoggingEnabled = false,
            instanceNameForLogging = null,
            initialRetryDelayInMilliseconds = 10,
            imageLoader = { if (calls.incrementAndGet() <= 2) null else FakeImageBitmap() },
        )
        val kubriko = newManualKubriko(spriteManager)
        try {
            val resource = spriteResource("retried")
            awaitCondition { spriteManager.get(resource) != null }
            runBlocking {
                withTimeout(2_000) { spriteManager.getSpriteLoadingProgress(listOf(resource)).first { it == 1f } }
            }
            assertTrue(calls.get() >= 3)
        } finally {
            kubriko.dispose()
        }
    }

    @Test
    fun loadFinishingAfterUnloadIsDiscarded() {
        val calls = AtomicInteger()
        val firstLoad = CompletableDeferred<ImageBitmap>()
        val spriteManager = SpriteManagerImpl(
            isLoggingEnabled = false,
            instanceNameForLogging = null,
            imageLoader = { if (calls.incrementAndGet() == 1) firstLoad.await() else CompletableDeferred<ImageBitmap>().await() },
        )
        val kubriko = newManualKubriko(spriteManager)
        try {
            val resource = spriteResource("unloaded")
            assertNull(spriteManager.get(resource))
            awaitCondition { calls.get() == 1 }
            spriteManager.unload(resource)
            firstLoad.complete(FakeImageBitmap())
            // Absence can only be checked over a window, and it has to outlast the 100 ms warm-up a kept load would end with.
            Thread.sleep(300)

            assertNull(spriteManager.get(resource))
            awaitCondition { calls.get() == 2 }
        } finally {
            kubriko.dispose()
        }
    }

    @OptIn(InternalResourceApi::class)
    private fun spriteResource(path: String) = SpriteResource(DrawableResource(path, emptySet()))
}
