/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.session

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

private open class ShapeBase

private class ShapeWithFields : ShapeBase(), Runnable {
    val name = "a"
    var count = 0

    override fun run() = Unit

    companion object {
        const val CONSTANT = 1
        val shared = listOf(1)
    }
}

private object ShapeWithState {
    var counter = 0
}

private enum class ShapeEnum { A }

class ClassShapeTest {
    private fun bytesOf(type: Class<*>) = type.getResourceAsStream("/${type.name.replace('.', '/')}.class")!!.use { it.readBytes() }
    private val packagePath = "com/pandulapeter/kubrikoCreator/session"

    @Test
    fun theShapeIsTheSuperclassTheInterfacesAndTheInstanceFields() {
        assertEquals(
            ClassShape(
                superName = "$packagePath/ShapeBase",
                interfaces = listOf("java/lang/Runnable"),
                instanceFields = setOf("name:Ljava/lang/String;", "count:I"),
                stateHash = null,
            ),
            ClassShape.read(bytesOf(ShapeWithFields::class.java)),
        )
    }

    @Test
    fun classesWithMutableStaticStateOrEnumConstantsAreKnownByTheirContent() {
        val state = ClassShape.read(bytesOf(ShapeWithState::class.java))
        assertNotNull(state.stateHash)
        assertNotEquals(state.stateHash, ClassShape.read(bytesOf(ShapeEnum::class.java)).stateHash)
        assertNull(ClassShape.read(bytesOf(ShapeWithState::class.java), isStateExempt = true).stateHash)
    }

    @Test
    fun theClassesOfFoldersAreReadByTheirPathsAndComposableSingletonsAreExemptFromState() {
        val folder = createTempDirectory().toFile()
        try {
            File(folder, "game/Ball.class").apply { parentFile.mkdirs() }.writeBytes(bytesOf(ShapeWithFields::class.java))
            File(folder, "game/ComposableSingletons\$MainKt.class").writeBytes(bytesOf(ShapeWithState::class.java))
            File(folder, "game/Game.class").writeBytes(bytesOf(ShapeWithState::class.java))
            File(folder, "game/notes.txt").writeText("not a class")
            val shapes = ClassShape.read(listOf(folder))
            assertEquals(setOf("game/Ball", "game/ComposableSingletons\$MainKt", "game/Game"), shapes.keys)
            assertNull(shapes.getValue("game/ComposableSingletons\$MainKt").stateHash)
            assertNotNull(shapes.getValue("game/Game").stateHash)
        } finally {
            folder.deleteRecursively()
        }
    }

    @Test
    fun onlyClassesTheGameRunsWhoseShapesChangedNeedARestart() {
        val fields = ClassShape.read(bytesOf(ShapeWithFields::class.java))
        val other = fields.copy(instanceFields = fields.instanceFields + "label:Ljava/lang/String;")
        assertEquals(
            listOf("game/B", "game/C"),
            ClassShape.changesNeedingRestart(
                running = mapOf("game/A" to fields, "game/B" to fields, "game/C" to fields, "game/E" to fields),
                built = mapOf("game/A" to fields, "game/C" to other, "game/B" to fields.copy(interfaces = emptyList()), "game/D" to other),
            ),
        )
    }
}
