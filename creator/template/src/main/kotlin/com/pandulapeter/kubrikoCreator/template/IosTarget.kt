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
 * The iOS target: the Kotlin framework the Xcode project in `iosApp/` builds the app around, and the build task that hands
 * Xcode the game's identity.
 */
internal object IosTarget : TargetEdits {
    const val XCODE_CONFIGURATION = "iosApp/Configuration/Config.xcconfig"
    private const val XCODE_USER_DATA = "xcuserdata/"
    private const val BUNDLE_IDENTIFIER = "PRODUCT_BUNDLE_IDENTIFIER"
    private val block = listOf(
        "listOf(",
        "    iosArm64(),",
        "    iosSimulatorArm64(),",
        ").forEach { iosTarget ->",
        "    iosTarget.binaries.framework {",
        "        baseName = \"ComposeApp\"",
        "        isStatic = true",
        "    }",
        "}",
    )

    override val target = GameTarget.IOS

    override val keptPaths = setOf(TargetFiles.GITIGNORE_FILE)

    override fun isOn(files: TargetFiles) = files.build?.contains("iosSimulatorArm64(") == true

    override fun turnedOn(files: TargetFiles): Map<String, String>? {
        val build = files.build ?: return null
        val withTarget = BuildFileLines.withTarget(build, block) ?: return null
        val buildBlock = files.buildBlockOf(target)
        val withBlock = if (buildBlock.lineSequence().first() in withTarget) withTarget else BuildFileLines.withAppended(withTarget, buildBlock)
        val gitignore = files.read(TargetFiles.GITIGNORE_FILE).orEmpty()
        val configuration = files.read(XCODE_CONFIGURATION)?.let { XCODE_CONFIGURATION to withBundleIdentifier(it, files.applicationId) }
        return files.missingFilesOf(target) +
            listOfNotNull(configuration) +
            (TargetFiles.BUILD_FILE to withBlock) +
            (TargetFiles.GITIGNORE_FILE to BuildFileLines.withLine(gitignore, XCODE_USER_DATA))
    }

    /**
     * [text] (an `.xcconfig` file) building [applicationId], or [text] itself if it doesn't set the bundle ID on a line of
     * its own.
     */
    fun withBundleIdentifier(text: String, applicationId: String) = text.lines().joinToString("\n") { line ->
        if (line.substringBefore('=').trim() == BUNDLE_IDENTIFIER && '=' in line) "$BUNDLE_IDENTIFIER=$applicationId" else line
    }

    override fun turnedOff(files: TargetFiles): Map<String, String>? {
        val build = files.build ?: return null
        val withoutTarget = BuildFileLines.withoutBlock(build, countsParentheses = true) { lines, index ->
            lines[index].trim() == block.first() && lines.getOrNull(index + 1)?.trim()?.startsWith("ios") == true
        } ?: return null
        if ("iosSimulatorArm64(" in withoutTarget) return null
        return mapOf(TargetFiles.BUILD_FILE to (BuildFileLines.withoutAppended(withoutTarget, files.buildBlockOf(target)) ?: return null))
    }
}
