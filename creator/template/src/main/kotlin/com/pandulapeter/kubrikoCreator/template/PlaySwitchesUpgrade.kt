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

import com.pandulapeter.kubrikoCreator.actors.codegen.ManagerTypeCodeGenerator

/**
 * Template version 11: Kubriko Creator plays the game in a browser, on Android, and on iOS too, so their entry points also
 * turn the Log Blocks on and choose the first Scene, as the desktop's `Main.kt` does: from the address of the page Kubriko
 * Creator serves on this computer, from the launching intent's extras in a debuggable Android build, and from the launch
 * arguments in a debug iOS framework. Since playing only starts from the first Scene and without logs without it, a file
 * that doesn't look like the template's counts as done rather than left to do by hand.
 */
internal object PlaySwitchesUpgrade {
    private const val VERSION = 11
    private const val LOGGING_SWITCH = "${ManagerTypeCodeGenerator.WIRING_NAME}.${ManagerTypeCodeGenerator.LOGGING_FLAG}"
    private const val FIRST_SCENE_SWITCH = "${ManagerTypeCodeGenerator.WIRING_NAME}.${ManagerTypeCodeGenerator.FIRST_SCENE_FLAG}"
    private const val LOGGING = ManagerTypeCodeGenerator.LOGGING_PROPERTY
    private const val FIRST_SCENE = ManagerTypeCodeGenerator.FIRST_SCENE_PROPERTY
    private const val WEB_MAIN = "fun main() = ComposeViewport {"
    private const val ANDROID_ON_CREATE = "super.onCreate(savedInstanceState)"
    private const val IOS_VIEW_CONTROLLER = "fun MainViewController(): UIViewController = ComposeUIViewController {"

    fun isWebDone(text: String) = FIRST_SCENE_SWITCH in text || mainLineOf(text.lines(), WEB_MAIN) == null

    /**
     * [text] (the web `Main.kt`) reading the switches from the page's address, or [text] itself if it does already or its
     * `main` isn't the template's `ComposeViewport` expression at the end of the file.
     */
    fun web(text: String, wiringImport: String) = if (FIRST_SCENE_SWITCH in text) {
        text
    } else {
        withBodyWrapped(
            text = text,
            declaration = WEB_MAIN,
            opening = listOf(
                "fun main() {",
                "    $LOGGING_SWITCH = creatorParameter(\"$LOGGING\").toBoolean()",
                "    $FIRST_SCENE_SWITCH = creatorParameter(\"$FIRST_SCENE\")",
                "    ComposeViewport {",
            ),
            trailing = listOf(
                "",
                "/**",
                " * A parameter of the page's address that Kubriko Creator sets when it plays the game, read only on the page it serves on",
                " * this computer.",
                " */",
                "private fun creatorParameter(name: String): String? = js(\"location.hostname === '127.0.0.1' ? " +
                    "new URLSearchParams(location.search).get(name) : null\")",
            ),
            imports = listOf(wiringImport),
        ) ?: text
    }

    fun isAndroidDone(text: String) = FIRST_SCENE_SWITCH in text || onCreateLineOf(text.lines()) == null

    /**
     * [text] (the Android `MainActivity.kt`) reading the switches from the intent's extras in a debuggable build, or [text]
     * itself if it does already or doesn't call `super.onCreate` on a line of its own exactly once.
     */
    fun android(text: String, wiringImport: String): String {
        val lines = text.lines()
        if (FIRST_SCENE_SWITCH in text) return text
        val line = onCreateLineOf(lines) ?: return text
        val indent = lines[line].takeWhile { it == ' ' }
        val block = listOf(
            "${indent}if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {",
            "$indent    $LOGGING_SWITCH = intent.getBooleanExtra(\"$LOGGING\", false)",
            "$indent    $FIRST_SCENE_SWITCH = intent.getStringExtra(\"$FIRST_SCENE\")",
            "$indent}",
        )
        return withImports(lines.take(line + 1) + block + lines.drop(line + 1), listOf("import android.content.pm.ApplicationInfo", wiringImport))
    }

    fun isIosDone(text: String) = FIRST_SCENE_SWITCH in text || mainLineOf(text.lines(), IOS_VIEW_CONTROLLER) == null

    /**
     * [text] (the iOS `MainViewController.kt`) reading the switches from the launch arguments in a debug framework, or
     * [text] itself if it does already or its function isn't the template's `ComposeUIViewController` expression at the
     * end of the file.
     */
    fun ios(text: String, wiringImport: String) = if (FIRST_SCENE_SWITCH in text) {
        text
    } else {
        withBodyWrapped(
            text = text,
            declaration = IOS_VIEW_CONTROLLER,
            opening = listOf(
                "@OptIn(ExperimentalNativeApi::class)",
                "fun MainViewController(): UIViewController {",
                "    if (Platform.isDebugBinary) {",
                "        val launchArguments = NSUserDefaults.standardUserDefaults",
                "        $LOGGING_SWITCH = launchArguments.boolForKey(\"$LOGGING\")",
                "        $FIRST_SCENE_SWITCH = launchArguments.stringForKey(\"$FIRST_SCENE\")",
                "    }",
                "    return ComposeUIViewController {",
            ),
            trailing = emptyList(),
            imports = listOf(
                "import platform.Foundation.NSUserDefaults",
                wiringImport,
                "import kotlin.experimental.ExperimentalNativeApi",
                "import kotlin.native.Platform",
            ),
        ) ?: text
    }

    /**
     * [text] with the expression body that [declaration] opens, which ends the file, made the last statement of a block
     * body that starts with [opening], followed by [trailing], or null if [text] isn't shaped so.
     */
    private fun withBodyWrapped(text: String, declaration: String, opening: List<String>, trailing: List<String>, imports: List<String>): String? {
        val lines = text.lines()
        val start = mainLineOf(lines, declaration) ?: return null
        val end = lines.indexOfLast { it.isNotBlank() }
        val body = lines.subList(start + 1, end + 1).map { if (it.isEmpty()) it else "    $it" }
        return withImports(lines.subList(0, start) + opening + body + "}" + trailing + lines.subList(end + 1, lines.size), imports)
    }

    /**
     * [lines] with each of [imports] they lack, in the order ktlint keeps them (`java`, `javax`, and `kotlin` last).
     */
    private fun withImports(lines: List<String>, imports: List<String>) = imports.fold(lines) { result, import ->
        if (import in result) {
            result
        } else {
            val next = result.indexOfFirst { it.startsWith("import ") && importOrderOf(it) > importOrderOf(import) }
            if (next >= 0) result.take(next) + import + result.drop(next) else ProjectUpgrader.withImport(result, import).lines()
        }
    }.joinToString("\n")

    private fun importOrderOf(import: String) = import.removePrefix("import ").let { name ->
        (if (lastImportPrefixes.any { name.startsWith(it) }) "1" else "0") + name
    }

    private val lastImportPrefixes = listOf("java.", "javax.", "kotlin.")

    /**
     * The line of [declaration], if it is there once and the file ends with the `}` closing it.
     */
    private fun mainLineOf(lines: List<String>, declaration: String) = lines.indexOf(declaration).takeIf {
        it >= 0 && lines.count { line -> line == declaration } == 1 && lines.lastOrNull { line -> line.isNotBlank() } == "}"
    }

    private fun onCreateLineOf(lines: List<String>) = lines.indices.filter { lines[it].trim() == ANDROID_ON_CREATE }.singleOrNull()

    /**
     * This version's step of the web `Main.kt`.
     */
    object WebStep : TemplateStep {
        override val version = VERSION

        override fun isDone(text: String, context: TemplateContext) = isWebDone(text)

        override fun change(text: String, context: TemplateContext) = web(text, context.wiringImport)
    }

    /**
     * This version's step of the Android `MainActivity.kt`.
     */
    object AndroidStep : TemplateStep {
        override val version = VERSION

        override fun isDone(text: String, context: TemplateContext) = isAndroidDone(text)

        override fun change(text: String, context: TemplateContext) = android(text, context.wiringImport)
    }

    /**
     * This version's step of the iOS `MainViewController.kt`.
     */
    object IosStep : TemplateStep {
        override val version = VERSION

        override fun isDone(text: String, context: TemplateContext) = isIosDone(text)

        override fun change(text: String, context: TemplateContext) = ios(text, context.wiringImport)
    }
}
