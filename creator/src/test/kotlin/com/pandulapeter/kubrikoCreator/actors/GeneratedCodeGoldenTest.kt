/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Pins the code generated from [EveryBlockTypes] to the files in `src/test/resources/generated`. A deliberate change to
 * the generated code rewrites them with `KUBRIKO_CREATOR_UPDATE_GOLDEN=true`, for review as a diff.
 */
class GeneratedCodeGoldenTest {
    private val root = createTempDirectory().toFile()
    private val goldenDirectory = File("src/test/resources/generated")

    @AfterTest
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun theEveryBlockProjectGeneratesTheCodeItDidBefore() {
        assertTrue(goldenDirectory.isDirectory, "${goldenDirectory.absolutePath} is missing.")
        File(root, ".kubriko/project.json").apply { parentFile.mkdirs() }.writeText("""{ "packageName": "com.example.integration" }""")
        File(root, "src/commonMain/composeResources/drawable/every-block.png").apply { parentFile.mkdirs() }.createNewFile()
        val store = assertNotNull(BlockTypeStore.open(root))
        EveryBlockTypes.all.forEach(store::save)
        store.regenerateAll()
        val generated = File(root, store.generatedPath).walkTopDown().filter { it.isFile }.associate { "${it.name}.txt" to it.readText() }
        if (System.getenv("KUBRIKO_CREATOR_UPDATE_GOLDEN") == "true") {
            goldenDirectory.listFiles().orEmpty().forEach { it.delete() }
            generated.forEach { (name, text) -> File(goldenDirectory, name).writeText(text) }
            fail("Golden files written; review the diff.")
        }
        val golden = goldenDirectory.listFiles().orEmpty().associate { it.name to it.readText().replace("\r\n", "\n") }
        assertEquals(golden.keys.sorted(), generated.keys.sorted())
        generated.forEach { (name, text) -> assertEquals(golden.getValue(name), text, "$name changed.") }
    }
}
