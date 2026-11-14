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
import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import com.pandulapeter.kubrikoCreator.actors.DefinitionPaths
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.GeneratedNames
import com.pandulapeter.kubrikoCreator.actors.KeyChoice
import com.pandulapeter.kubrikoCreator.actors.KotlinUi
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.ScreenTextDefinition
import com.pandulapeter.kubrikoCreator.actors.TimerNaming
import com.pandulapeter.kubrikoCreator.actors.codegen.ActorClassCode.appendNumberTextFunction
import com.pandulapeter.kubrikoCreator.actors.codegen.ActorClassCode.appendTimerCountdown
import com.pandulapeter.kubrikoCreator.actors.codegen.ActorClassCode.appendTimerFields
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.AUDIO_FIELD
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.SCENES_FIELD
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.STATE_MANAGER_FIELD
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.STATE_MANAGER_IMPORT
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.colorLiteral
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.eventCode
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.floatLiteral
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.initialValueLiteral
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.templateLiteral
import com.pandulapeter.kubrikoCreator.actors.codegen.EventFunctions.appendFunction
import com.pandulapeter.kubrikoCreator.actors.timerName

/**
 * Turns block-built Manager types into ordinary Kotlin: one `Manager` subclass per type, plus [WIRING_NAME], which
 * creates all of them for `Kubriko.newInstance`.
 *
 * Properties are Compose state, so the texts the Manager shows follow them; reading and writing them doesn't allocate.
 */
public object ManagerTypeCodeGenerator {

    public const val WIRING_NAME: String = GeneratedNames.WIRING

    /**
     * The field of [WIRING_NAME] that turns the Log Blocks on, and the system property the play session sets for it.
     */
    public const val LOGGING_FLAG: String = "isLoggingEnabled"
    public const val LOGGING_PROPERTY: String = "kubriko.creator.logging"
    public const val FIRST_SCENE_FLAG: String = "firstSceneOverride"

    /**
     * The field of [WIRING_NAME] that every random number of the Blocks is drawn from.
     */
    internal const val RANDOM_FIELD: String = "random"

    /**
     * The field of [WIRING_NAME] listing the game's running Kubriko instances, which `GameEntryPoint` reports to Kubriko
     * Creator.
     */
    public const val RUNNING_INSTANCES: String = "runningInstances"
    public const val FIRST_SCENE_PROPERTY: String = "kubriko.creator.scene"
    private const val SCENE_EDITOR_GUARD = "if (isInSceneEditor) return"
    private const val ACTOR_MANAGER_FIELD = "actorManager"
    private const val VIEWPORT_MANAGER_FIELD = "viewportManager"

    /**
     * @param knownActorTypeNames Every Actor type of the project, which the Blocks may add.
     * @param managerTypes Every Manager type of the project, whose Properties the Blocks may use.
     */
    internal fun generateManagerType(
        definition: ManagerTypeDefinition,
        packageName: String,
        knownActorTypeNames: List<String> = emptyList(),
        managerTypes: List<ManagerTypeDefinition> = listOf(definition),
    ): String {
        val name = definition.name
        val scope = BlockScope(name, BlockTypeKind.MANAGER, definition.properties, knownActorTypeNames, managerTypes)
        val events = definition.events.filter { it.type.isAvailableIn(BlockTypeKind.MANAGER) }
        val keyEvents = events.filter { it.type == EventType.KEY_PRESSED && it.key?.isKnown == true }
        val timerEvents = events.filter { it.type == EventType.TIMER_FINISHED && it.timer?.let(TimerNaming::isValid) == true }
        val startEvents = events.filter { it.type == EventType.GAME_STARTED }
        val frameEvents = events.filter { it.type == EventType.EVERY_FRAME }
        val generatedEvents = startEvents + frameEvents + keyEvents + timerEvents
        val timers = (timerEvents.mapNotNull { it.timer } + generatedEvents.flatMap { event -> event.blocks.mapNotNull { it.timerName } }).filter(TimerNaming::isValid).distinct().sorted()
        val blockCode = generatedEvents.fold(BlockCode.NONE) { code, event -> code + eventCode(event.blocks, event.type, scope) }
        val texts = definition.texts.map { templateLiteral(it.text, scope) }
        val kotlinUi = KotlinUi.of(definition.kotlinUi)
        val usedManagers = (blockCode.managers + texts.flatMap { it.managers } + scope.managersNamedIn(kotlinUi)).distinct().sorted()
        val runsEveryFrame = timers.isNotEmpty() || frameEvents.any { it.blocks.isNotEmpty() }
        // Key Events add the Manager to the Scene as an Actor, and onUpdate returns early while the game is paused.
        val needs = blockCode.needs + buildSet {
            if (keyEvents.isNotEmpty() || kotlinUi?.names(ACTOR_MANAGER_FIELD) == true) add(BlockNeed.ACTOR_MANAGER)
            if (kotlinUi?.names(VIEWPORT_MANAGER_FIELD) == true) add(BlockNeed.VIEWPORT_MANAGER)
            if (runsEveryFrame || kotlinUi?.names(STATE_MANAGER_FIELD) == true) add(BlockNeed.STATE_MANAGER)
            if (texts.any { it.usesNumberText }) add(BlockNeed.NUMBER_TEXT)
        }
        val functions = generatedEvents.filter { it.blocks.isNotEmpty() }.mapNotNull { EventFunctions.of(it, scope) }.distinct()
        fun callOf(events: List<EventDefinition>) = events.firstNotNullOfOrNull { event -> EventFunctions.of(event, scope)?.takeIf { it in functions }?.call }
        val functionsCode = StringBuilder()
        val functionImports = mutableSetOf<String>()
        functions.forEach { function ->
            functionsCode.appendFunction("private", function, generatedEvents.filter { EventFunctions.of(it, scope) == function }, scope, functionImports)
        }
        val imports = buildSet {
            add("com.pandulapeter.kubriko.manager.Manager")
            addAll(functionImports)
            if (keyEvents.isNotEmpty() || startEvents.any { it.blocks.isNotEmpty() }) add("com.pandulapeter.kubriko.Kubriko")
            if (keyEvents.isNotEmpty()) addAll(listOf("androidx.compose.ui.input.key.Key", "com.pandulapeter.kubriko.keyboardInput.KeyboardInputAware"))
            if (definition.properties.any { it is PropertyDefinition.Number }) add("androidx.compose.runtime.mutableFloatStateOf")
            if (definition.properties.any { it !is PropertyDefinition.Number }) add("androidx.compose.runtime.mutableStateOf")
            if (definition.properties.isNotEmpty()) addAll(listOf("androidx.compose.runtime.getValue", "androidx.compose.runtime.setValue"))
            needs.forEach { addAll(importsOf(it)) }
            addAll(blockCode.imports)
            if (texts.isNotEmpty()) addAll(TEXT_IMPORTS)
            if (texts.isNotEmpty() || kotlinUi != null) addAll(COMPOSABLE_IMPORTS)
            kotlinUi?.imports?.let(::addAll)
            kotlinUi?.let { ui -> BlockCodeGenerator.randomImportFor(ui.statements.joinToString("\n"), ui.imports)?.let(::add) }
        }.sorted()
        return GeneratedFile.withHeader(" from ${DefinitionPaths.definitionPath(definition)}", buildString {
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
            BlockNeed.entries.filter { it in needs }.forEach { need -> fieldOf(need)?.let { appendLine("    private val $it") } }
            usedManagers.forEach { appendLine("    private val ${scope.managerField(it)} by manager<$it>()") }
            appendTimerFields(timers, "private")
            if (keyEvents.isNotEmpty() || startEvents.any { it.blocks.isNotEmpty() }) {
                appendLine()
                appendLine("    override fun onInitialize(kubriko: Kubriko) {")
                appendLine("        $SCENE_EDITOR_GUARD")
                // Kubriko tells Actors about key presses, so the Manager is one too (GeneratedScenes keeps it in every Scene).
                if (keyEvents.isNotEmpty()) appendLine("        actorManager.add(this)")
                callOf(startEvents)?.let { appendLine("        $it") }
                appendLine("    }")
            }
            if (runsEveryFrame) {
                appendLine()
                appendLine("    override fun onUpdate(deltaTimeInMilliseconds: Int) {")
                appendLine("        if (isInSceneEditor || !$STATE_MANAGER_FIELD.isRunning.value) return")
                timers.forEach { timer ->
                    appendTimerCountdown(timer) { callOf(timerEvents.filter { it.trigger.timer == timer })?.let { appendLine("                $it") } }
                }
                callOf(frameEvents)?.let { appendLine("        $it") }
                appendLine("    }")
            }
            // Unlike an Actor's, these run while the game is paused, so that a key can resume it.
            if (keyEvents.isNotEmpty()) appendKeyDispatch(keyEvents.groupBy { it.trigger.key!! }.toSortedMap().mapValues { (_, events) -> callOf(events) })
            append(functionsCode)
            if (texts.isNotEmpty() || kotlinUi != null) appendComposable(definition.texts, texts.map { it.code }, kotlinUi)
            if (BlockNeed.NUMBER_TEXT in needs) appendNumberTextFunction("private")
            appendLine("}")
        })
    }

    /**
     * @param pluginManagers Which of the plugins' Managers the game needs, which are created here too, so that `Game.kt`
     *   doesn't name a plugin the game doesn't use.
     */
    internal fun generateWiring(
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
        appendLine("import com.pandulapeter.kubriko.Kubriko")
        appendLine("import com.pandulapeter.kubriko.actor.Actor")
        if (pluginManagers.usesAudio) appendLine("import com.pandulapeter.kubriko.audioPlayback.MusicManager")
        if (pluginManagers.usesAudio) appendLine("import com.pandulapeter.kubriko.audioPlayback.SoundManager")
        if (pluginManagers.usesCollisions) appendLine("import com.pandulapeter.kubriko.collision.CollisionManager")
        if (pluginManagers.usesKeyboard) appendLine("import com.pandulapeter.kubriko.keyboardInput.KeyboardInputManager")
        appendLine("import com.pandulapeter.kubriko.manager.Manager")
        if (pluginManagers.usesSprites) appendLine("import com.pandulapeter.kubriko.sprites.SpriteManager")
        appendLine("import kotlin.concurrent.Volatile")
        appendLine("import kotlin.random.Random")
        appendLine()
        appendLine("/**")
        appendLine(" * What Kubriko needs from the types built in Kubriko Creator.")
        appendLine(" */")
        appendLine("object $WIRING_NAME {")
        appendLine()
        appendLine("    /**")
        appendLine("     * Whether the Log Blocks log. Kubriko Creator turns it on while it plays the game (the entry points read it as")
        appendLine("     * `$LOGGING_PROPERTY`); a released game leaves it off, since every log allocates.")
        appendLine("     */")
        appendLine("    var $LOGGING_FLAG = false")
        appendLine()
        appendLine("    /**")
        appendLine("     * The Scene the game starts with instead of the one `Game.kt` names, when Kubriko Creator plays it from a Scene (the")
        appendLine("     * entry points read it as `$FIRST_SCENE_PROPERTY`); null in a released game.")
        appendLine("     */")
        appendLine("    var $FIRST_SCENE_FLAG: String? = null")
        appendLine()
        appendLine("    /**")
        appendLine("     * Where every random number of the Blocks comes from (random values and the chance condition), which a test can")
        appendLine("     * replace with a seeded one to play the game the same way every time.")
        appendLine("     */")
        appendLine("    var $RANDOM_FIELD: Random = Random.Default")
        appendLine()
        appendLine("    /**")
        appendLine("     * The game's running Kubriko instances: `Game.kt` adds its own while it is shown, and `GameEntryPoint` reports them, so")
        appendLine("     * that Kubriko Creator can wait until the game has drawn its Actors.")
        appendLine("     */")
        appendLine("    @Volatile")
        appendLine("    var $RUNNING_INSTANCES: List<Kubriko> = emptyList()")
        appendLine()
        appendLine("    /**")
        appendLine("     * Creates the Managers of every Manager type built in Kubriko Creator, the one that loads Scenes (and the ones that play")
        appendLine("     * sounds and music and load the sprites, if the game has any), and those of the plugins the game uses, for")
        appendLine("     * Kubriko.newInstance.")
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
            (if (pluginManagers.usesSprites) ", SpriteManager.newInstance(), ${SpritesCodeGenerator.SPRITES_NAME}()" else "") + ")"
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

    private fun StringBuilder.appendKeyDispatch(callsByKey: Map<KeyChoice, String?>) {
        appendLine()
        appendLine("    override fun onKeyPressed(key: Key) {")
        appendLine("        when (key) {")
        callsByKey.forEach { (key, call) -> if (call != null) appendLine("            Key.${key.composeKeyName} -> $call") }
        appendLine("        }")
        appendLine("    }")
    }

    /**
     * The Manager's `Composable`: its texts, then its Kotlin UI, which is drawn on top of them. Texts sharing a position
     * are stacked in a column there, in their order, rather than drawn over each other.
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
            definitions.indices.groupBy { definitions[it].position }.forEach { (position, indices) ->
                if (indices.size == 1) {
                    appendText(texts[indices.single()], indices.single(), indent = "            ", alignment = position.alignment)
                } else {
                    // By its full name, since the project may have a type called Column in the generated package.
                    appendLine("            androidx.compose.foundation.layout.Column(")
                    appendLine("                modifier = Modifier.align(Alignment.${position.alignment}),")
                    appendLine("                horizontalAlignment = Alignment.${position.horizontalAlignment},")
                    appendLine("            ) {")
                    indices.forEach { index -> appendText(texts[index], index, indent = "                ", alignment = null) }
                    appendLine("            }")
                }
            }
            appendLine("        }")
        }
        kotlinUi?.statements?.forEach { appendLine(if (it.isEmpty()) it else "        $it") }
        appendLine("    }")
    }

    private fun StringBuilder.appendText(text: String, index: Int, indent: String, alignment: String?) {
        appendLine("${indent}BasicText(")
        if (alignment != null) appendLine("$indent    modifier = Modifier.align(Alignment.$alignment),")
        appendLine("$indent    text = $text,")
        appendLine("$indent    style = ${textStyleField(index)},")
        appendLine("$indent)")
    }

    private fun fieldOf(need: BlockNeed) = when (need) {
        BlockNeed.ACTOR_MANAGER -> "$ACTOR_MANAGER_FIELD by manager<ActorManager>()"
        BlockNeed.VIEWPORT_MANAGER -> "$VIEWPORT_MANAGER_FIELD by manager<ViewportManager>()"
        BlockNeed.KEYBOARD_INPUT_MANAGER -> "keyboardInputManager by manager<KeyboardInputManager>()"
        BlockNeed.STATE_MANAGER -> "$STATE_MANAGER_FIELD by manager<StateManager>()"
        BlockNeed.SCENES -> "$SCENES_FIELD by manager<${ScenesCodeGenerator.SCENES_NAME}>()"
        BlockNeed.AUDIO -> "$AUDIO_FIELD by manager<${AudioCodeGenerator.AUDIO_NAME}>()"
        BlockNeed.MOVEMENT, BlockNeed.NUMBER_TEXT, BlockNeed.BOUNCE_OFF, BlockNeed.DIRECTION, BlockNeed.STEERING, BlockNeed.ANIMATION, BlockNeed.FRAME,
        BlockNeed.SCALE_TWEEN, BlockNeed.OPACITY_TWEEN, BlockNeed.COLOR_TWEEN, BlockNeed.POSITION_TWEEN,
        -> null
    }

    private fun importsOf(need: BlockNeed) = when (need) {
        BlockNeed.ACTOR_MANAGER -> listOf("com.pandulapeter.kubriko.manager.ActorManager")
        BlockNeed.VIEWPORT_MANAGER -> listOf("com.pandulapeter.kubriko.manager.ViewportManager")
        BlockNeed.KEYBOARD_INPUT_MANAGER -> listOf("androidx.compose.ui.input.key.Key", "com.pandulapeter.kubriko.keyboardInput.KeyboardInputManager")
        BlockNeed.STATE_MANAGER -> listOf(STATE_MANAGER_IMPORT)
        BlockNeed.MOVEMENT, BlockNeed.SCENES, BlockNeed.AUDIO, BlockNeed.NUMBER_TEXT, BlockNeed.BOUNCE_OFF, BlockNeed.DIRECTION, BlockNeed.STEERING, BlockNeed.ANIMATION, BlockNeed.FRAME,
        BlockNeed.SCALE_TWEEN, BlockNeed.OPACITY_TWEEN, BlockNeed.COLOR_TWEEN, BlockNeed.POSITION_TWEEN,
        -> emptyList()
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
