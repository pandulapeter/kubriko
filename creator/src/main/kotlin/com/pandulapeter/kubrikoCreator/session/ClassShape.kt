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

import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.File
import java.io.IOException
import java.security.MessageDigest

/**
 * What a reload can't change about a class the running game uses: Compose Hot Reload redefines methods in place, but an
 * object that exists keeps its fields (a field added to it is null or zero, which a non-null Kotlin property never is), an
 * Actor's Traits are read once when it is added, and a class whose static initializer runs again loses its static state.
 *
 * @param instanceFields Each instance field as its name and descriptor.
 * @param stateHash The content of a class that holds static state a reload would reset (mutable static fields or enum
 *   constants), null for one that holds none.
 */
internal data class ClassShape(
    val superName: String?,
    val interfaces: List<String>,
    val instanceFields: Set<String>,
    val stateHash: String?,
) {
    companion object {
        private const val MAGIC = 0xCAFEBABE.toInt()
        private const val ACC_STATIC = 0x0008
        private const val ACC_FINAL = 0x0010
        private const val ACC_ENUM = 0x4000

        /**
         * Compose keeps the lambdas of a file's composables in its `ComposableSingletons` class, whose static fields Hot
         * Reload sets again by design.
         */
        private const val COMPOSABLE_SINGLETONS_PREFIX = "ComposableSingletons$"

        /**
         * The shape of every class in [folders], by its path from its folder without the extension (`com/example/Ball`).
         */
        fun read(folders: Collection<File>): Map<String, ClassShape> = buildMap {
            folders.forEach { folder ->
                folder.walkTopDown().filter { it.isFile && it.extension == "class" }.forEach { file ->
                    val name = file.relativeTo(folder).invariantSeparatorsPath.removeSuffix(".class")
                    if (name !in this) put(name, read(file.readBytes(), isStateExempt = name.substringAfterLast('/').startsWith(COMPOSABLE_SINGLETONS_PREFIX)))
                }
            }
        }

        /**
         * The names (as [read] gives them) of the classes in both [running] and [built] whose shapes differ, which a
         * reload can't bring the running game up to.
         */
        fun changesNeedingRestart(running: Map<String, ClassShape>, built: Map<String, ClassShape>) =
            built.filter { (name, shape) -> running[name].let { it != null && it != shape } }.keys.sorted()

        /**
         * Reads the shape of the class file [bytes].
         *
         * @param isStateExempt Whether its mutable static fields are left out of [stateHash].
         */
        internal fun read(bytes: ByteArray, isStateExempt: Boolean = false): ClassShape {
            val input = DataInputStream(ByteArrayInputStream(bytes))
            if (input.readInt() != MAGIC) throw IOException("Not a class file.")
            input.skipBytes(4)
            val constantCount = input.readUnsignedShort()
            val strings = arrayOfNulls<String>(constantCount)
            val classNameIndices = IntArray(constantCount)
            var index = 1
            while (index < constantCount) {
                when (val tag = input.readUnsignedByte()) {
                    1 -> strings[index] = input.readUTF()
                    7 -> classNameIndices[index] = input.readUnsignedShort()
                    8, 16, 19, 20 -> input.skipBytes(2)
                    15 -> input.skipBytes(3)
                    3, 4, 9, 10, 11, 12, 17, 18 -> input.skipBytes(4)
                    5, 6 -> {
                        input.skipBytes(8)
                        index++
                    }
                    else -> throw IOException("Unknown constant pool tag $tag.")
                }
                index++
            }
            fun className(constantIndex: Int) = if (constantIndex == 0) null else strings[classNameIndices[constantIndex]]
            val isEnum = input.readUnsignedShort() and ACC_ENUM != 0
            input.skipBytes(2)
            val superName = className(input.readUnsignedShort())
            val interfaces = List(input.readUnsignedShort()) { className(input.readUnsignedShort()).orEmpty() }
            val instanceFields = mutableSetOf<String>()
            var hasMutableStaticFields = false
            repeat(input.readUnsignedShort()) {
                val access = input.readUnsignedShort()
                val field = "${strings[input.readUnsignedShort()]}:${strings[input.readUnsignedShort()]}"
                when {
                    access and ACC_STATIC == 0 -> instanceFields += field
                    access and ACC_FINAL == 0 -> hasMutableStaticFields = true
                }
                repeat(input.readUnsignedShort()) {
                    input.skipBytes(2)
                    input.skipBytes(input.readInt())
                }
            }
            val hasState = isEnum || (hasMutableStaticFields && !isStateExempt)
            return ClassShape(
                superName = superName,
                interfaces = interfaces,
                instanceFields = instanceFields,
                stateHash = if (hasState) hashOf(bytes) else null,
            )
        }

        private fun hashOf(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    }
}
