/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors.codegen

import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.FontFiles
import com.pandulapeter.kubrikoCreator.actors.KotlinUi
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.ScreenTextDefinition
import com.pandulapeter.kubrikoCreator.actors.TextFont
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.STATE_MANAGER_FIELD
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.colorLiteral
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.floatLiteral
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.templateLiteral

/**
 * What a generated Manager draws on top of the game: its texts, then its Kotlin UI, drawn on top of them, in its
 * `Composable`.
 *
 * @param members The members the Manager declares for it: the texts' styles and the `Composable` itself.
 * @param needs What the Manager declares for it besides [members].
 * @param imports The fully qualified names [members] use.
 * @param managers The other Manager types whose fields [members] use.
 */
internal class ManagerUiCode private constructor(
    val members: String,
    val needs: Set<BlockNeed>,
    val imports: Set<String>,
    val managers: Set<String>,
) {

    companion object {

        /**
         * The UI of [definition], whose Blocks see [scope], in a game whose code is in [packageName], or null if it shows
         * nothing.
         */
        fun of(definition: ManagerTypeDefinition, scope: BlockScope, packageName: String): ManagerUiCode? {
            val kotlinUi = KotlinUi.of(definition.kotlinUi)
            if (definition.texts.isEmpty() && kotlinUi == null) return null
            val texts = definition.texts.map { templateLiteral(it.text, scope) }
            val conditions = definition.texts.map { text -> text.shownWhen?.let { ConditionCode.of(it, scope) } }
            val conditionCode = conditions.fold(BlockCode.NONE) { code, condition -> if (condition == null) code else code + condition }
            val fontAccessors = definition.texts.map { text -> text.fontFile?.let(FontFiles::accessorOf) }
            val needs = conditionCode.needs + buildSet {
                if (kotlinUi?.names(ManagerTypeCodeGenerator.ACTOR_MANAGER_FIELD) == true) add(BlockNeed.ACTOR_MANAGER)
                if (kotlinUi?.names(ManagerTypeCodeGenerator.VIEWPORT_MANAGER_FIELD) == true) add(BlockNeed.VIEWPORT_MANAGER)
                if (kotlinUi?.names(STATE_MANAGER_FIELD) == true) add(BlockNeed.STATE_MANAGER)
                if (texts.any { it.usesNumberText }) add(BlockNeed.NUMBER_TEXT)
            }
            val imports = buildSet {
                addAll(COMPOSABLE_IMPORTS)
                if (texts.isNotEmpty()) addAll(TEXT_IMPORTS)
                if (fontAccessors.any { it != null }) addAll(listOf("$packageName.resources.Res", "$packageName.resources.allFontResources", "androidx.compose.runtime.remember"))
                addAll(conditionCode.imports)
                kotlinUi?.imports?.let(::addAll)
                kotlinUi?.let { ui -> BlockCodeGenerator.randomImportFor(ui.statements.joinToString("\n"), ui.imports)?.let(::add) }
            }
            return ManagerUiCode(
                members = buildString { appendComposable(definition.texts, texts.map { it.code }, conditions.map { it?.lines?.single() }, fontAccessors, kotlinUi) },
                needs = needs,
                imports = imports,
                managers = texts.flatMapTo(HashSet()) { it.managers } + conditionCode.managers + scope.managersNamedIn(kotlinUi),
            )
        }

        /**
         * Texts sharing a position are stacked in a column there, in their order, rather than drawn over each other; one
         * that isn't shown takes no room in it.
         *
         * @param conditions The expression each text is shown while, or null for one shown always.
         * @param fontAccessors The accessor of the font file each text is drawn in, or null for one drawn in its family.
         */
        private fun StringBuilder.appendComposable(
            definitions: List<ScreenTextDefinition>,
            texts: List<String>,
            conditions: List<String?>,
            fontAccessors: List<String?>,
            kotlinUi: KotlinUi?,
        ) {
            definitions.forEachIndexed { index, definition ->
                appendLine()
                appendLine("    private val ${textStyleField(index)} = ${textStyleOf(definition)}")
            }
            appendLine()
            appendLine("    @Composable")
            appendLine("    override fun Composable(windowInsets: WindowInsets) {")
            fontAccessors.forEachIndexed { index, accessor -> if (accessor != null) appendFontFileStyle(index, accessor) }
            if (texts.isNotEmpty()) {
                appendLine("        Box(")
                appendLine("            modifier = Modifier.fillMaxSize().windowInsetsPadding(windowInsets).padding(16.dp),")
                appendLine("        ) {")
                definitions.indices.groupBy { definitions[it].position }.forEach { (position, indices) ->
                    if (indices.size == 1) {
                        val index = indices.single()
                        appendText(texts[index], styleOf(index, fontAccessors), conditions[index], indent = "            ", alignment = position.alignment)
                    } else {
                        // By its full name, since the project may have a type called Column in the generated package.
                        appendLine("            androidx.compose.foundation.layout.Column(")
                        appendLine("                modifier = Modifier.align(Alignment.${position.alignment}),")
                        appendLine("                horizontalAlignment = Alignment.${position.horizontalAlignment},")
                        appendLine("            ) {")
                        indices.forEach { index -> appendText(texts[index], styleOf(index, fontAccessors), conditions[index], indent = "                ", alignment = null) }
                        appendLine("            }")
                    }
                }
                appendLine("        }")
            }
            kotlinUi?.statements?.forEach { appendLine(if (it.isEmpty()) it else "        $it") }
            appendLine("    }")
        }

        /**
         * The style of the text at [index] drawn in the font file of [accessor], made in the `Composable`, since loading
         * a font resource is `@Composable`: the text's own style (in its family) while the game has no such file, so a
         * deleted one breaks no build, and a copy drawn in the font otherwise, made again only when the font changes.
         * Where fonts load in the background (the web), Compose hands out its default face until the file has loaded, so
         * the text shows in that for its first moments. Its locals start with an underscore, which no Property's name can.
         */
        private fun StringBuilder.appendFontFileStyle(index: Int, accessor: String) {
            val font = "_font${index + 1}"
            appendLine("        val $font = Res.allFontResources[${BlockCodeGenerator.stringLiteral(accessor)}]?.let { org.jetbrains.compose.resources.Font(it) }")
            appendLine("        val ${fontFileStyle(index)} = remember($font) { if ($font == null) ${textStyleField(index)} else ${textStyleField(index)}.copy(fontFamily = $FONT_PACKAGE.FontFamily($font)) }")
        }

        private fun styleOf(index: Int, fontAccessors: List<String?>) = if (fontAccessors[index] != null) fontFileStyle(index) else textStyleField(index)

        private fun fontFileStyle(index: Int) = "_textStyle${index + 1}"

        private fun StringBuilder.appendText(text: String, style: String, condition: String?, indent: String, alignment: String?) {
            if (condition != null) appendLine("${indent}if ($condition) {")
            val textIndent = if (condition == null) indent else "$indent    "
            appendLine("${textIndent}BasicText(")
            if (alignment != null) appendLine("$textIndent    modifier = Modifier.align(Alignment.$alignment),")
            appendLine("$textIndent    text = $text,")
            appendLine("$textIndent    style = $style,")
            appendLine("$textIndent)")
            if (condition != null) appendLine("$indent}")
        }

        private fun textStyleOf(text: ScreenTextDefinition) = buildList {
            add("color = Color(${colorLiteral(text.color)})")
            add("fontSize = ${floatLiteral(text.size.coerceAtLeast(1f))}.sp")
            if (text.font != TextFont.DEFAULT) add("fontFamily = $FONT_PACKAGE.FontFamily.${text.font.fontFamily}")
            if (text.isBold) add("fontWeight = $FONT_PACKAGE.FontWeight.Bold")
            if (text.isItalic) add("fontStyle = $FONT_PACKAGE.FontStyle.Italic")
        }.joinToString(prefix = "TextStyle(", postfix = ")")

        /**
         * The field that keeps the style of the text at [index], so that it isn't built again on every recomposition.
         */
        private fun textStyleField(index: Int) = "textStyle${index + 1}"

        /**
         * Where the font classes are, named in full rather than imported, since an Actor type or a Manager type may be
         * called like one of them, and reserving those names would need a migration renaming such types.
         */
        private const val FONT_PACKAGE = "androidx.compose.ui.text.font"

        private val COMPOSABLE_IMPORTS = listOf(
            "androidx.compose.foundation.layout.WindowInsets",
            "androidx.compose.runtime.Composable",
        )

        private val TEXT_IMPORTS = listOf(
            "androidx.compose.foundation.layout.Box",
            "androidx.compose.foundation.layout.fillMaxSize",
            "androidx.compose.foundation.layout.padding",
            "androidx.compose.foundation.layout.windowInsetsPadding",
            "androidx.compose.foundation.text.BasicText",
            "androidx.compose.ui.Alignment",
            "androidx.compose.ui.Modifier",
            "androidx.compose.ui.graphics.Color",
            "androidx.compose.ui.text.TextStyle",
            "androidx.compose.ui.unit.dp",
            "androidx.compose.ui.unit.sp",
        )
    }
}
