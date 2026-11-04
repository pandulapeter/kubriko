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
 * Template version 12: Compose for Web has no font of its own, so a text (a Manager type's) drew nothing in the browser.
 * The web source set gets an open font (Public Sans, under the SIL Open Font License, which its metadata carries), and the
 * web `Main.kt` shows the game only once that font is loaded as the one every text falls back to. A `Main.kt` whose
 * `ComposeViewport` isn't the template's, or a build that doesn't name its resources' package, is left to the user.
 */
internal object WebFontUpgrade {
    private const val VERSION = 12
    const val FONT_FILE = "public_sans_regular.ttf"
    const val FONT_RESOURCE = "fonts/$FONT_FILE"
    private const val WRAPPER = "WithFallbackFont"
    private const val VIEWPORT = "ComposeViewport {"
    private const val FONT_FAMILY_RESOLVER = "LocalFontFamilyResolver"
    private const val RAW_STRING = "\"\"\""
    private val clashingNames = listOf("Font", "Res")
    private val resourcesPackagePattern = Regex("""packageOfResClass\s*=\s*"([^"]+)"""")
    private val fontAccessor = FONT_FILE.substringBeforeLast('.')

    fun isDone(text: String) = WRAPPER in text || FONT_FAMILY_RESOLVER in text

    /**
     * Whether a project whose web `Main.kt` is [text] needs the font: one that loads a font of its own doesn't.
     */
    fun needsFont(text: String) = WRAPPER in text || FONT_FAMILY_RESOLVER !in text

    /**
     * [text] (the web `Main.kt`) showing the game only once the fallback font is loaded, or null if its `ComposeViewport`
     * block isn't there once, ending in a line of its own and holding no raw string, if it imports another `Font` or
     * `Res`, or if [buildFile] doesn't name the package of the resources' `Res` class.
     */
    fun web(text: String, buildFile: String?): String? {
        val resourcesPackage = buildFile?.let { resourcesPackagePattern.find(it)?.groupValues?.get(1) } ?: return null
        val lines = text.lines()
        val imports = imports(resourcesPackage)
        if (lines.any { line -> line.startsWith("import ") && line !in imports && clashingNames.any { line.endsWith(".$it") || line.endsWith(" as $it") } }) return null
        val start = lines.indices.singleOrNull { lines[it].trim() == VIEWPORT } ?: return null
        val indent = lines[start].takeWhile { it == ' ' }
        val end = (start + 1 until lines.size).firstOrNull { lines[it] == "$indent}" } ?: return null
        if (lines.subList(start + 1, end).any { RAW_STRING in it }) return null
        val body = lines.subList(start + 1, end).map { if (it.isEmpty()) it else "    $it" }
        val last = lines.indexOfLast { it.isNotBlank() }
        val wrapped = lines.subList(0, start + 1) + "$indent    $WRAPPER {" + body + "$indent    }" + lines.subList(end, last + 1) +
            fallbackFont + lines.subList(last + 1, lines.size)
        return imports.fold(wrapped) { result, import ->
            if (import in result) result else ProjectUpgrader.withImport(result, import).lines()
        }.joinToString("\n")
    }

    private fun imports(resourcesPackage: String) = listOf(
        "import androidx.compose.runtime.Composable",
        "import androidx.compose.runtime.LaunchedEffect",
        "import androidx.compose.runtime.getValue",
        "import androidx.compose.runtime.mutableStateOf",
        "import androidx.compose.runtime.remember",
        "import androidx.compose.runtime.setValue",
        "import androidx.compose.ui.platform.LocalFontFamilyResolver",
        "import androidx.compose.ui.text.font.FontFamily",
        "import androidx.compose.ui.text.platform.Font",
        "import $resourcesPackage.Res",
        "import $resourcesPackage.$fontAccessor",
        "import kotlinx.coroutines.CancellationException",
        "import org.jetbrains.compose.resources.getFontResourceBytes",
        "import org.jetbrains.compose.resources.getSystemResourceEnvironment",
    )

    private val fallbackFont = listOf(
        "",
        "/**",
        " * Shows [content] once the font every text falls back to is loaded, since Compose for Web has no font of its own: a text",
        " * drawn before then stays blank.",
        " */",
        "@Composable",
        "private fun $WRAPPER(content: @Composable () -> Unit) {",
        "    val fontFamilyResolver = LocalFontFamilyResolver.current",
        "    var isLoaded by remember { mutableStateOf(false) }",
        "    LaunchedEffect(Unit) {",
        "        try {",
        "            val bytes = getFontResourceBytes(getSystemResourceEnvironment(), Res.font.$fontAccessor)",
        "            fontFamilyResolver.preload(FontFamily(Font(\"$fontAccessor\", bytes)))",
        "        } catch (exception: CancellationException) {",
        "            throw exception",
        "        } catch (exception: Exception) {",
        "            println(\"The font texts fall back to couldn't be loaded, so they may stay blank: \$exception\")",
        "        }",
        "        isLoaded = true",
        "    }",
        "    if (isLoaded) content()",
        "}",
    )

    /**
     * This version's step of the web `Main.kt`.
     */
    object WebStep : TemplateStep {
        override val version = VERSION

        override fun isDone(text: String, context: TemplateContext) = isDone(text)

        override fun change(text: String, context: TemplateContext) = web(text, context.read(TargetFiles.BUILD_FILE))
    }

    /**
     * The font this version adds to a project built for the web, unless its `Main.kt` loads a font of its own.
     */
    val fontFile = CopiedFile(path = { it.webFontPath }, resource = FONT_RESOURCE) { context -> context.read(context.webMainPath)?.let(::needsFont) == true }
}
