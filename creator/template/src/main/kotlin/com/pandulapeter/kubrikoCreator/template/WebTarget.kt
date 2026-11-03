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

/**
 * The web target: the Kotlin/Wasm executable, and the build titling its page.
 */
internal object WebTarget : TargetEdits {
    private const val ANNOTATION = "@OptIn(ExperimentalWasmDsl::class)"
    private const val IMPORT = "import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl"
    private const val BLOCK_START = "wasmJs {"

    override val target = GameTarget.WEB

    override fun isOn(files: TargetFiles) = files.build?.lines()?.any { it.trim() == BLOCK_START } == true

    override fun turnedOn(files: TargetFiles): Map<String, String>? {
        val build = files.build ?: return null
        val withTarget = BuildFileLines.withTarget(build, listOf(ANNOTATION, BLOCK_START, "    browser()", "    binaries.executable()", "}")) ?: return null
        val block = files.buildBlockOf(target)
        val withBlock = if (block.lineSequence().first() in withTarget) withTarget else BuildFileLines.withAppended(withTarget, block)
        return files.missingFilesOf(target) + (TargetFiles.BUILD_FILE to BuildFileLines.withImport(withBlock, IMPORT))
    }

    override fun turnedOff(files: TargetFiles): Map<String, String>? {
        val build = files.build ?: return null
        val withoutTarget = BuildFileLines.withoutBlock(build, ANNOTATION) { lines, index -> lines[index].trim() == BLOCK_START } ?: return null
        val withoutBlock = BuildFileLines.withoutAppended(withoutTarget, files.buildBlockOf(target)) ?: return null
        return mapOf(TargetFiles.BUILD_FILE to BuildFileLines.withoutUnusedImport(withoutBlock, IMPORT, "ExperimentalWasmDsl"))
    }
}
