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

import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.AUDIO_FIELD
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.NUMBER_TEXT_FUNCTION
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.SCENES_FIELD
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.STATE_MANAGER_FIELD
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.STATE_MANAGER_IMPORT
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.appendEvents
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.appendKeyHandling
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

    /**
     * The field of [WIRING_NAME] that turns the Log Blocks on, and the system property the play session sets for it.
     */
    const val LOGGING_FLAG = "isLoggingEnabled"
    const val LOGGING_PROPERTY = "kubriko.creator.logging"
    const val FIRST_SCENE_FLAG = "firstSceneOverride"
    const val FIRST_SCENE_PROPERTY = "kubriko.creator.scene"
    private const val SCENE_EDITOR_GUARD = "if (isInSceneEditor) return"
    private const val ACTOR_MANAGER_FIELD = "actorManager"
    private const val VIEWPORT_MANAGER_FIELD = "viewportManager"

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
        val kotlinUi = KotlinUi.of(definition.kotlinUi)
        val usedManagers = (scope.managersUsedBy(generatedBlocks, definition.texts.map { it.text }) + scope.managersNamedIn(kotlinUi)).distinct().sorted()
        val usesScenes = BlockCodeGenerator.usesScenes(generatedBlocks)
        val runsEveryFrame = timers.isNotEmpty() || frameEvents.any { it.blocks.isNotEmpty() }
        val usesKotlin = generatedBlocks.any { it is BlockDefinition.Kotlin }
        val usesActorManager = keyEvents.isNotEmpty() || usesKotlin || generatedBlocks.any { it is BlockDefinition.AddActor && it.actorType in knownActorTypeNames } ||
            kotlinUi?.names(ACTOR_MANAGER_FIELD) == true
        val usesViewportManager = usesKotlin || kotlinUi?.names(VIEWPORT_MANAGER_FIELD) == true
        val usesStateManager = runsEveryFrame || BlockCodeGenerator.usesPause(generatedBlocks) || kotlinUi?.names(STATE_MANAGER_FIELD) == true
        val imports = buildSet {
            add("com.pandulapeter.kubriko.manager.Manager")
            if (keyEvents.isNotEmpty() || startEvents.any { it.blocks.isNotEmpty() }) add("com.pandulapeter.kubriko.Kubriko")
            if (usesActorManager) add("com.pandulapeter.kubriko.manager.ActorManager")
            if (usesViewportManager) add("com.pandulapeter.kubriko.manager.ViewportManager")
            if (keyEvents.isNotEmpty()) addAll(listOf("androidx.compose.ui.input.key.Key", "com.pandulapeter.kubriko.keyboardInput.KeyboardInputAware"))
            if (definition.properties.any { it is PropertyDefinition.Number }) add("androidx.compose.runtime.mutableFloatStateOf")
            if (definition.properties.any { it !is PropertyDefinition.Number }) add("androidx.compose.runtime.mutableStateOf")
            if (definition.properties.isNotEmpty()) addAll(listOf("androidx.compose.runtime.getValue", "androidx.compose.runtime.setValue"))
            if (usesKeyHeld) addAll(listOf("androidx.compose.ui.input.key.Key", "com.pandulapeter.kubriko.keyboardInput.KeyboardInputManager"))
            if (usesStateManager) add(STATE_MANAGER_IMPORT)
            if (generatedBlocks.any { it is BlockDefinition.Log }) add("com.pandulapeter.kubriko.logger.Logger")
            if (conditions.any { it is ConditionDefinition.Chance }) add("kotlin.random.Random")
            if (generatedBlocks.any { it is BlockDefinition.AddActor && it.actorType in knownActorTypeNames }) addAll(listOf("com.pandulapeter.kubriko.helpers.extensions.sceneUnit", "com.pandulapeter.kubriko.types.SceneOffset"))
            if (texts.isNotEmpty()) addAll(TEXT_IMPORTS)
            if (texts.isNotEmpty() || kotlinUi != null) addAll(COMPOSABLE_IMPORTS)
            kotlinUi?.imports?.let(::addAll)
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
            appendLine(") : Manager()${if (keyEvents.isNotEmpty()) ", KeyboardInputAware" else ""} {")
            definition.properties.forEach { property ->
                appendLine("    var ${property.name} by ${if (property is PropertyDefinition.Number) "mutableFloatStateOf" else "mutableStateOf"}(${initialValueLiteral(property)})")
            }
            if (definition.properties.isNotEmpty()) appendLine()
            if (usesActorManager) appendLine("    private val $ACTOR_MANAGER_FIELD by manager<ActorManager>()")
            if (usesViewportManager) appendLine("    private val $VIEWPORT_MANAGER_FIELD by manager<ViewportManager>()")
            if (usesKeyHeld) appendLine("    private val keyboardInputManager by manager<KeyboardInputManager>()")
            if (usesStateManager) appendLine("    private val $STATE_MANAGER_FIELD by manager<StateManager>()")
            if (usesScenes) appendLine("    private val $SCENES_FIELD by manager<${ScenesCodeGenerator.SCENES_NAME}>()")
            if (BlockCodeGenerator.usesAudio(generatedBlocks)) appendLine("    private val $AUDIO_FIELD by manager<${AudioCodeGenerator.AUDIO_NAME}>()")
            usedManagers.forEach { appendLine("    private val ${scope.managerField(it)} by manager<$it>()") }
            timers.forEach { timer ->
                appendLine()
                appendLine("    /**")
                appendLine("     * Until Timer $timer finishes, or 0 while it doesn't run, and how often it finishes again, or 0 if it doesn't.")
                appendLine("     */")
                appendLine("    private var ${remainingField(timer)} = 0L")
                appendLine("    private var ${periodField(timer)} = 0L")
            }
            if (keyEvents.isNotEmpty() || startEvents.any { it.blocks.isNotEmpty() }) {
                appendLine()
                appendLine("    override fun onInitialize(kubriko: Kubriko) {")
                appendLine("        $SCENE_EDITOR_GUARD")
                // Kubriko tells Actors about key presses, so the Manager is one too (GeneratedScenes keeps it in every Scene).
                if (keyEvents.isNotEmpty()) appendLine("        actorManager.add(this)")
                appendEvents(startEvents, depth = 2, scope = scope)
                appendLine("    }")
            }
            if (runsEveryFrame) {
                appendLine()
                appendLine("    override fun onUpdate(deltaTimeInMilliseconds: Int) {")
                appendLine("        if (isInSceneEditor || !$STATE_MANAGER_FIELD.isRunning.value) return")
                timers.forEach { timer -> appendTimer(timer, timerEvents.filter { it.timer == timer }, scope) }
                appendEvents(frameEvents, depth = 2, scope = scope)
                appendLine("    }")
            }
            // Unlike an Actor's, these run while the game is paused, so that a key can resume it.
            if (keyEvents.isNotEmpty()) appendKeyHandling(keyEvents, scope)
            if (texts.isNotEmpty() || kotlinUi != null) appendComposable(definition.texts, texts, kotlinUi)
            if (texts.any { ".$NUMBER_TEXT_FUNCTION()" in it } || generatedBlocks.any { it is BlockDefinition.Log && ".$NUMBER_TEXT_FUNCTION()" in templateLiteral(it.message, scope) }) {
                appendLine()
                appendLine("    private fun Float.$NUMBER_TEXT_FUNCTION() = if (this % 1f == 0f) toLong().toString() else toString()")
            }
            appendLine("}")
        })
    }

    /**
     * @param pluginManagers Which of the plugins' Managers the game needs, which are created here too, so that `Game.kt`
     *   doesn't name a plugin the game doesn't use.
     */
    fun generateWiring(
        managerTypeNames: List<String>,
        packageName: String,
        pluginManagers: PluginManagers = PluginManagers(),
    ) = GeneratedFile.withHeader("", buildString {
        val pluginManagerNames = listOfNotNull(
            "CollisionManager".takeIf { pluginManagers.usesCollisions },
            "KeyboardInputManager".takeIf { pluginManagers.usesKeyboard },
            "MusicManager".takeIf { pluginManagers.usesAudio },
            "SoundManager".takeIf { pluginManagers.usesAudio },
        )
        appendLine("package $packageName.${ActorTypeCodeGenerator.GENERATED_PACKAGE}")
        appendLine()
        appendLine("import com.pandulapeter.kubriko.actor.Actor")
        if (pluginManagers.usesAudio) appendLine("import com.pandulapeter.kubriko.audioPlayback.MusicManager")
        if (pluginManagers.usesAudio) appendLine("import com.pandulapeter.kubriko.audioPlayback.SoundManager")
        if (pluginManagers.usesCollisions) appendLine("import com.pandulapeter.kubriko.collision.CollisionManager")
        if (pluginManagers.usesKeyboard) appendLine("import com.pandulapeter.kubriko.keyboardInput.KeyboardInputManager")
        appendLine("import com.pandulapeter.kubriko.manager.Manager")
        if (pluginManagers.usesSprites) appendLine("import com.pandulapeter.kubriko.sprites.SpriteManager")
        appendLine()
        appendLine("/**")
        appendLine(" * What Kubriko needs from the types built in Kubriko Creator.")
        appendLine(" */")
        appendLine("object $WIRING_NAME {")
        appendLine()
        appendLine("    /**")
        appendLine("     * Whether the Log Blocks log. Kubriko Creator turns it on while it plays the game (`Main.kt` reads it from the")
        appendLine("     * `$LOGGING_PROPERTY` system property); a released game leaves it off, since every log allocates.")
        appendLine("     */")
        appendLine("    var $LOGGING_FLAG = false")
        appendLine()
        appendLine("    /**")
        appendLine("     * The Scene the game starts with instead of the one `Game.kt` names, when Kubriko Creator plays it from a Scene (`Main.kt`")
        appendLine("     * reads it from the `$FIRST_SCENE_PROPERTY` system property); null in a released game.")
        appendLine("     */")
        appendLine("    var $FIRST_SCENE_FLAG: String? = null")
        appendLine()
        appendLine("    /**")
        appendLine("     * Creates the Managers of every Manager type built in Kubriko Creator, the one that loads Scenes (and the one that plays")
        appendLine("     * sounds and music, if the game does), and those of the plugins the game uses, for Kubriko.newInstance.")
        appendLine("     *")
        appendLine("     * @param isInSceneEditor True in the Scene editor, where their Events don't run, no Scene is loaded, and the plugins'")
        appendLine("     *   Managers are left to the editor (except the one that draws sprites, which it doesn't have).")
        appendLine("     * @param firstScene The name of the Scene loaded when the game starts, or null to load none.")
        appendLine("     */")
        appendLine("    fun createManagers(isInSceneEditor: Boolean = false, firstScene: String? = null): List<Manager> {")
        appendLine("        val managers = listOf<Manager>(")
        managerTypeNames.sorted().forEach { appendLine("            $it(isInSceneEditor),") }
        appendLine("        )")
        val scenes = "listOf(${ScenesCodeGenerator.SCENES_NAME}($FIRST_SCENE_FLAG ?: firstScene, isInSceneEditor, managers.filterIsInstance<Actor>())" +
            (if (pluginManagers.usesAudio) ", ${AudioCodeGenerator.AUDIO_NAME}(isInSceneEditor)" else "") +
            (if (pluginManagers.usesSprites) ", SpriteManager.newInstance()" else "") + ")"
        if (pluginManagerNames.isEmpty()) {
            appendLine("        return $scenes + managers")
        } else {
            appendLine("        val pluginManagers = if (isInSceneEditor) {")
            appendLine("            emptyList()")
            appendLine("        } else {")
            appendLine("            listOf<Manager>(")
            pluginManagerNames.forEach { appendLine("                $it.newInstance(),") }
            appendLine("            )")
            appendLine("        }")
            appendLine("        return pluginManagers + $scenes + managers")
        }
        appendLine("    }")
        appendLine("}")
    })

    /**
     * The Manager's `Composable`: its texts, then its Kotlin UI, which is drawn on top of them.
     */
    private fun StringBuilder.appendComposable(definitions: List<ScreenTextDefinition>, texts: List<String>, kotlinUi: KotlinUi?) {
        definitions.forEachIndexed { index, definition ->
            appendLine()
            appendLine("    private val ${textStyleField(index)} = TextStyle(color = Color(${colorLiteral(definition.color)}), fontSize = ${floatLiteral(definition.size.coerceAtLeast(1f))}.sp)")
        }
        appendLine()
        appendLine("    @Composable")
        appendLine("    override fun Composable(windowInsets: WindowInsets) {")
        if (texts.isNotEmpty()) {
            appendLine("        Box(")
            appendLine("            modifier = Modifier.fillMaxSize().windowInsetsPadding(windowInsets).padding(16.dp),")
            appendLine("        ) {")
            definitions.forEachIndexed { index, definition ->
                appendLine("            BasicText(")
                appendLine("                modifier = Modifier.align(Alignment.${definition.position.alignment}),")
                appendLine("                text = ${texts[index]},")
                appendLine("                style = ${textStyleField(index)},")
                appendLine("            )")
            }
            appendLine("        }")
        }
        kotlinUi?.statements?.forEach { appendLine(if (it.isEmpty()) it else "        $it") }
        appendLine("    }")
    }

    /**
     * The field that keeps the style of the text at [index], so that it isn't built again on every recomposition.
     */
    private fun textStyleField(index: Int) = "textStyle${index + 1}"

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
