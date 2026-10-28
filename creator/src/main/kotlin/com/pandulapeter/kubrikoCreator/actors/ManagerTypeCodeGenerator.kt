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

import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.NUMBER_TEXT_FUNCTION
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.SCENES_FIELD
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.STATE_MANAGER_FIELD
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.STATE_MANAGER_IMPORT
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.appendEvents
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.appendTimer
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.colorLiteral
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.floatLiteral
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.initialValueLiteral
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.periodField
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.remainingField
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.templateLiteral

/**
 * Turns block-built Manager types into ordinary Kotlin: one `Manager` subclass per type, plus [WIRING_NAME], which
 * creates all of them for `Kubriko.newInstance`.
 *
 * Properties are Compose state, so the texts the Manager shows follow them; reading and writing them doesn't allocate.
 */
internal object ManagerTypeCodeGenerator {

    const val WIRING_NAME = "KubrikoWiring"
    private const val SCENE_EDITOR_GUARD = "if (isInSceneEditor) return"

    /**
     * @param knownActorTypeNames Every Actor type of the project, which the Blocks may add.
     * @param managerTypes Every Manager type of the project, whose Properties the Blocks may use.
     */
    fun generateManagerType(
        definition: ManagerTypeDefinition,
        packageName: String,
        knownActorTypeNames: Collection<String> = emptyList(),
        managerTypes: List<ManagerTypeDefinition> = listOf(definition),
    ): String {
        val name = definition.name
        val scope = BlockScope(name, BlockTypeKind.MANAGER, definition.properties, knownActorTypeNames.toList(), managerTypes)
        val events = definition.events.filter { it.type.isAvailableIn(BlockTypeKind.MANAGER) }
        val keyEvents = events.filter { it.type == EventType.KEY_PRESSED && it.key != null }
        val timerEvents = events.filter { it.type == EventType.TIMER_FINISHED && it.timer in TIMER_NUMBERS }
        val startEvents = events.filter { it.type == EventType.GAME_STARTED }
        val frameEvents = events.filter { it.type == EventType.EVERY_FRAME }
        val generatedEvents = startEvents + frameEvents + keyEvents + timerEvents
        val generatedBlocks = generatedEvents.flatMap { it.blocks }
        val timers = (timerEvents.mapNotNull { it.timer } + generatedBlocks.mapNotNull { it.timerNumber }).filter { it in TIMER_NUMBERS }.distinct().sorted()
        val conditions = generatedBlocks.mapNotNull { (it as? BlockDefinition.If)?.condition }
        val usesKeyHeld = conditions.any { it is ConditionDefinition.KeyHeld }
        val texts = definition.texts.map { templateLiteral(it.text, scope) }
        val usedManagers = scope.managersUsedBy(generatedBlocks, definition.texts.map { it.text })
        val usesScenes = BlockCodeGenerator.usesScenes(generatedBlocks)
        val runsEveryFrame = timers.isNotEmpty() || frameEvents.any { it.blocks.isNotEmpty() }
        val keys = keyEvents.mapNotNull { it.key }.distinct().sorted()
        val usesStateManager = runsEveryFrame || BlockCodeGenerator.usesPause(generatedBlocks)
        val imports = buildSet {
            add("com.pandulapeter.kubriko.Kubriko")
            add("com.pandulapeter.kubriko.manager.ActorManager")
            add("com.pandulapeter.kubriko.manager.Manager")
            add("com.pandulapeter.kubriko.manager.ViewportManager")
            if (definition.properties.any { it is PropertyDefinition.Number }) add("androidx.compose.runtime.mutableFloatStateOf")
            if (definition.properties.any { it !is PropertyDefinition.Number }) add("androidx.compose.runtime.mutableStateOf")
            if (definition.properties.isNotEmpty()) addAll(listOf("androidx.compose.runtime.getValue", "androidx.compose.runtime.setValue"))
            if (usesKeyHeld || keys.isNotEmpty()) addAll(listOf("androidx.compose.ui.input.key.Key", "com.pandulapeter.kubriko.keyboardInput.KeyboardInputManager"))
            if (usesStateManager) add(STATE_MANAGER_IMPORT)
            if (generatedBlocks.any { it is BlockDefinition.Log }) add("com.pandulapeter.kubriko.logger.Logger")
            if (conditions.any { it is ConditionDefinition.Chance }) add("kotlin.random.Random")
            if (generatedBlocks.any { it is BlockDefinition.AddActor && it.actorType in knownActorTypeNames }) addAll(listOf("com.pandulapeter.kubriko.helpers.extensions.sceneUnit", "com.pandulapeter.kubriko.types.SceneOffset"))
            if (texts.isNotEmpty()) addAll(TEXT_IMPORTS)
        }.sorted()
        return GeneratedFile.withHeader(" from .kubriko/managers/$name.json", buildString {
            appendLine("package $packageName.${ActorTypeCodeGenerator.GENERATED_PACKAGE}")
            appendLine()
            imports.forEach { appendLine("import $it") }
            appendLine()
            appendLine("/**")
            appendLine(" * @param isInSceneEditor True in the Scene editor, where its Events don't run, but Actors placed there can still use it.")
            appendLine(" */")
            appendLine("class $name(")
            appendLine("    private val isInSceneEditor: Boolean = false,")
            appendLine(") : Manager() {")
            definition.properties.forEach { property ->
                appendLine("    var ${property.name} by ${if (property is PropertyDefinition.Number) "mutableFloatStateOf" else "mutableStateOf"}(${initialValueLiteral(property)})")
            }
            if (definition.properties.isNotEmpty()) appendLine()
            appendLine("    private val actorManager by manager<ActorManager>()")
            appendLine("    private val viewportManager by manager<ViewportManager>()")
            if (usesKeyHeld || keys.isNotEmpty()) appendLine("    private val keyboardInputManager by manager<KeyboardInputManager>()")
            if (usesStateManager) appendLine("    private val $STATE_MANAGER_FIELD by manager<StateManager>()")
            if (usesScenes) appendLine("    private val $SCENES_FIELD by manager<${ScenesCodeGenerator.SCENES_NAME}>()")
            usedManagers.forEach { appendLine("    private val ${scope.managerField(it)} by manager<$it>()") }
            timers.forEach { timer ->
                appendLine()
                appendLine("    /**")
                appendLine("     * Until Timer $timer finishes, or 0 while it doesn't run, and how often it finishes again, or 0 if it doesn't.")
                appendLine("     */")
                appendLine("    private var ${remainingField(timer)} = 0L")
                appendLine("    private var ${periodField(timer)} = 0L")
            }
            if (keys.isNotEmpty()) {
                appendLine()
                appendLine("    /**")
                appendLine("     * Whether each key that a key pressed Event uses was down in the previous frame, so that the Event runs once per press.")
                appendLine("     */")
                keys.forEach { appendLine("    private var ${keyDownField(it)} = false") }
            }
            if (startEvents.any { it.blocks.isNotEmpty() }) {
                appendLine()
                appendLine("    override fun onInitialize(kubriko: Kubriko) {")
                appendLine("        $SCENE_EDITOR_GUARD")
                appendEvents(startEvents, depth = 2, scope = scope)
                appendLine("    }")
            }
            if (runsEveryFrame || keys.isNotEmpty()) {
                appendLine()
                appendLine("    override fun onUpdate(deltaTimeInMilliseconds: Int) {")
                appendLine("        $SCENE_EDITOR_GUARD")
                appendKeyPolling(keyEvents, keys, scope)
                if (runsEveryFrame) {
                    appendLine("        if (!$STATE_MANAGER_FIELD.isRunning.value) return")
                    timers.forEach { timer -> appendTimer(timer, timerEvents.filter { it.timer == timer }, scope) }
                    appendEvents(frameEvents, depth = 2, scope = scope)
                }
                appendLine("    }")
            }
            if (texts.isNotEmpty()) appendTexts(definition.texts, texts)
            if (texts.any { ".$NUMBER_TEXT_FUNCTION()" in it } || generatedBlocks.any { it is BlockDefinition.Log && ".$NUMBER_TEXT_FUNCTION()" in templateLiteral(it.message, scope) }) {
                appendLine()
                appendLine("    private fun Float.$NUMBER_TEXT_FUNCTION() = if (this % 1f == 0f) toLong().toString() else toString()")
            }
            appendLine("}")
        })
    }

    fun generateWiring(managerTypeNames: List<String>, packageName: String) = GeneratedFile.withHeader("", buildString {
        appendLine("package $packageName.${ActorTypeCodeGenerator.GENERATED_PACKAGE}")
        appendLine()
        appendLine("import com.pandulapeter.kubriko.manager.Manager")
        appendLine()
        appendLine("/**")
        appendLine(" * What Kubriko needs from the types built in Kubriko Creator.")
        appendLine(" */")
        appendLine("object $WIRING_NAME {")
        appendLine()
        appendLine("    /**")
        appendLine("     * Creates the Managers of every Manager type built in Kubriko Creator, and the one that loads Scenes, for Kubriko.newInstance.")
        appendLine("     *")
        appendLine("     * @param isInSceneEditor True in the Scene editor, where their Events don't run and no Scene is loaded.")
        appendLine("     * @param firstScene The name of the Scene loaded when the game starts, or null to load none.")
        appendLine("     */")
        appendLine("    fun createManagers(isInSceneEditor: Boolean = false, firstScene: String? = null): List<Manager> = listOf(")
        appendLine("        ${ScenesCodeGenerator.SCENES_NAME}(firstScene, isInSceneEditor),")
        managerTypeNames.sorted().forEach { appendLine("        $it(isInSceneEditor),") }
        appendLine("    )")
        appendLine("}")
    })

    /**
     * Runs the key pressed Events of each of [keys] once when it goes down. Kubriko only tells Actors about key presses, so
     * a Manager polls the keys instead, before the pause check, so that a key can resume the game.
     */
    private fun StringBuilder.appendKeyPolling(keyEvents: List<EventDefinition>, keys: List<KeyChoice>, scope: BlockScope) = keys.forEach { key ->
        val field = keyDownField(key)
        appendLine("        if (keyboardInputManager.isKeyPressed(Key.${key.composeKeyName}) != $field) {")
        appendLine("            $field = !$field")
        appendLine("            if ($field) {")
        appendEvents(keyEvents.filter { it.key == key }, depth = 4, scope = scope)
        appendLine("            }")
        appendLine("        }")
    }

    private fun keyDownField(key: KeyChoice) = "isKey${key.composeKeyName}Down"

    private fun StringBuilder.appendTexts(definitions: List<ScreenTextDefinition>, texts: List<String>) {
        appendLine()
        appendLine("    @Composable")
        appendLine("    override fun Composable(windowInsets: WindowInsets) {")
        appendLine("        Box(")
        appendLine("            modifier = Modifier.fillMaxSize().windowInsetsPadding(windowInsets).padding(16.dp),")
        appendLine("        ) {")
        definitions.forEachIndexed { index, definition ->
            appendLine("            BasicText(")
            appendLine("                modifier = Modifier.align(Alignment.${definition.position.alignment}),")
            appendLine("                text = ${texts[index]},")
            appendLine("                style = TextStyle(color = Color(${colorLiteral(definition.color)}), fontSize = ${floatLiteral(definition.size.coerceAtLeast(1f))}.sp),")
            appendLine("            )")
        }
        appendLine("        }")
        appendLine("    }")
    }

    private val TEXT_IMPORTS = listOf(
        "androidx.compose.foundation.layout.Box",
        "androidx.compose.foundation.layout.WindowInsets",
        "androidx.compose.foundation.layout.fillMaxSize",
        "androidx.compose.foundation.layout.padding",
        "androidx.compose.foundation.layout.windowInsetsPadding",
        "androidx.compose.foundation.text.BasicText",
        "androidx.compose.runtime.Composable",
        "androidx.compose.ui.Alignment",
        "androidx.compose.ui.Modifier",
        "androidx.compose.ui.graphics.Color",
        "androidx.compose.ui.text.TextStyle",
        "androidx.compose.ui.unit.dp",
        "androidx.compose.ui.unit.sp",
    )
}
