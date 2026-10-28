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

import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.INDENT
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.appendKeyHandling
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.appendTimer
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.appendEvents
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.NUMBER_TEXT_FUNCTION
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.SCENES_FIELD
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.STATE_MANAGER_FIELD
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.STATE_MANAGER_IMPORT
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.blockLines
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.colorLiteral
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.eventLines
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.initialValueLiteral
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.periodField
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.remainingField
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.stringLiteral
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.templateLiteral
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.unitLiteral

/**
 * Turns block-built Actor types into ordinary Kotlin: one class per type, plus a registry of all of them.
 *
 * The output is meant to read like hand-written code, so "Convert to code" leaves something a person would keep.
 */
internal object ActorTypeCodeGenerator {

    const val GENERATED_PACKAGE = "generated"
    const val REGISTRY_NAME = "GeneratedActorTypes"

    /**
     * @param knownActorTypeNames Every Actor type of the project: collision Events with any other Actor type (one that
     *   was deleted, or not chosen yet) are left out, so that the generated code always compiles.
     * @param managerTypes Every Manager type of the project, whose Properties the Blocks may use.
     */
    fun generateActorType(
        definition: ActorTypeDefinition,
        packageName: String,
        knownActorTypeNames: Collection<String> = listOf(definition.name),
        managerTypes: List<ManagerTypeDefinition> = emptyList(),
    ): String {
        val name = definition.name
        val scope = BlockScope(name, BlockTypeKind.ACTOR, definition.properties, knownActorTypeNames.toList(), managerTypes)
        val keyEvents = definition.events.filter { it.type == EventType.KEY_PRESSED && it.key != null }
        val edgeEvents = definition.events.filter { it.type == EventType.EDGE_TOUCHED }
        val collisionEvents = definition.events.filter { it.type == EventType.COLLISION && it.otherActorType in knownActorTypeNames }
        val timerEvents = definition.events.filter { it.type == EventType.TIMER_FINISHED && it.timer in TIMER_NUMBERS }
        val generatedEvents = definition.events.filter { it.type == EventType.ADDED_TO_SCENE || it.type == EventType.EVERY_FRAME } + edgeEvents + keyEvents + collisionEvents + timerEvents
        val timers = (timerEvents.mapNotNull { it.timer } + generatedEvents.flatMap { event -> event.blocks.mapNotNull { it.timerNumber } }).filter { it in TIMER_NUMBERS }.distinct().sorted()
        val blocksByEvent = edgeEvents.flatMap { event -> event.blocks.map { it to event.type } } + collisionEvents.flatMap { event -> event.blocks.map { it to event.type } }
        val usesAbsoluteValue = blocksByEvent.any { (block, eventType) -> blockLines(block, eventType, scope).any { "absoluteValue" in it } }
        val generatedBlocks = generatedEvents.flatMap { it.blocks }
        val conditions = generatedBlocks.mapNotNull { (it as? BlockDefinition.If)?.condition }
        val usesKeyHeld = conditions.any { it is ConditionDefinition.KeyHeld }
        val usedManagers = scope.managersUsedBy(generatedBlocks)
        // Edge Events use the Scene settings' bounds, which GeneratedScenes knows.
        val usesScenes = BlockCodeGenerator.usesScenes(generatedBlocks) || edgeEvents.isNotEmpty()
        val usesPause = BlockCodeGenerator.usesPause(generatedBlocks)
        val usesKotlin = generatedBlocks.any { it is BlockDefinition.Kotlin }
        val isDynamic = usesKotlin || edgeEvents.isNotEmpty() || timers.isNotEmpty() ||
            definition.events.any { it.type == EventType.EVERY_FRAME && it.blocks.isNotEmpty() } || generatedBlocks.any { it.movesTheActor }
        val usesActorManager = usesKotlin || generatedBlocks.any { it.needsActorManager(knownActorTypeNames) }
        val usesViewportManager = usesKotlin || edgeEvents.isNotEmpty()
        val usesStateManager = usesPause || keyEvents.isNotEmpty()
        val lookups = listOfNotNull(
            "actorManager: ActorManager".takeIf { usesActorManager },
            "viewportManager: ViewportManager".takeIf { usesViewportManager },
            "keyboardInputManager: KeyboardInputManager".takeIf { usesKeyHeld },
            "$STATE_MANAGER_FIELD: StateManager".takeIf { usesStateManager },
            "$SCENES_FIELD: ${ScenesCodeGenerator.SCENES_NAME}".takeIf { usesScenes },
        ) + usedManagers.map { "${scope.managerField(it)}: $it" }
        val addedEvents = definition.events.filter { it.type == EventType.ADDED_TO_SCENE }
        val traits = listOfNotNull(
            "Visible",
            "Dynamic".takeIf { isDynamic },
            if (collisionEvents.isEmpty()) "Collidable" else "CollisionDetector",
            "Editable<$name>",
            "KeyboardInputAware".takeIf { keyEvents.isNotEmpty() },
        )
        val imports = buildSet {
            addAll(BASE_IMPORTS)
            add("com.pandulapeter.kubriko.collision.Collidable")
            add(
                when (definition.shape) {
                    ActorShape.CIRCLE -> "com.pandulapeter.kubriko.collision.mask.CircleCollisionMask"
                    ActorShape.RECTANGLE -> "com.pandulapeter.kubriko.collision.mask.BoxCollisionMask"
                },
            )
            if (collisionEvents.isNotEmpty()) add("com.pandulapeter.kubriko.collision.CollisionDetector")
            if (collisionEvents.isNotEmpty()) add("kotlin.reflect.KClass")
            if (keyEvents.isNotEmpty()) addAll(KEYBOARD_IMPORTS)
            if (edgeEvents.isNotEmpty()) add("com.pandulapeter.kubriko.helpers.extensions.constrainedWithin")
            if (usesAbsoluteValue) add("kotlin.math.absoluteValue")
            if (generatedEvents.any { event -> event.blocks.any { it is BlockDefinition.Log } }) add("com.pandulapeter.kubriko.logger.Logger")
            if (collisionEvents.any { event -> BlockDefinition.BounceOffOther in event.blocks }) add("com.pandulapeter.kubriko.types.SceneUnit")
            if (usesKeyHeld) addAll(KEYBOARD_MANAGER_IMPORTS)
            if (usesStateManager) add(STATE_MANAGER_IMPORT)
            if (isDynamic) add("com.pandulapeter.kubriko.actor.traits.Dynamic")
            if (usesActorManager) add("com.pandulapeter.kubriko.manager.ActorManager")
            if (usesViewportManager) add("com.pandulapeter.kubriko.manager.ViewportManager")
            if (lookups.isNotEmpty()) addAll(listOf("com.pandulapeter.kubriko.Kubriko", "com.pandulapeter.kubriko.helpers.extensions.get"))
            if (isDynamic || edgeEvents.isNotEmpty() || generatedBlocks.any { it is BlockDefinition.JumpTo || (it is BlockDefinition.AddActor && it.actorType in knownActorTypeNames) }) {
                add("com.pandulapeter.kubriko.types.SceneOffset")
            }
            if (conditions.any { it is ConditionDefinition.Chance }) add("kotlin.random.Random")
            if (generatedBlocks.any { it is BlockDefinition.MoveTowardsNearest && it.actorType in knownActorTypeNames }) {
                add("com.pandulapeter.kubriko.helpers.extensions.distanceTo")
                add("com.pandulapeter.kubriko.helpers.extensions.normalized")
            }
        }.sorted()
        return GeneratedFile.withHeader(" from .kubriko/actors/$name.json", buildString {
            appendLine("package $packageName.$GENERATED_PACKAGE")
            appendLine()
            imports.forEach { appendLine("import $it") }
            appendLine()
            appendLine("class $name private constructor(state: State) : ${traits.joinToString()} {")
            appendLine("    override val body = state.body")
            appendLine()
            appendLine("    @set:Exposed(name = \"color\")")
            appendLine("    var color: Color = state.color")
            if (isDynamic) {
                appendLine()
                appendLine("    /**")
                appendLine("     * In scene units per second: the Actor moves by this much every second.")
                appendLine("     */")
                appendLine("    var velocity = SceneOffset.Zero")
            }
            definition.properties.forEach { property ->
                appendLine()
                appendLine("    @set:Exposed(name = ${stringLiteral(property.name)})")
                appendLine("    var ${property.name}: ${PropertyKind.of(property).kotlinType} = state.${property.name}")
            }
            appendLine()
            appendLine("    /**")
            appendLine("     * The middle of the Actor in the Scene, wherever its pivot is, which is where its collision mask goes.")
            appendLine("     */")
            appendLine("    private val bodyCenter get() = body.axisAlignedBoundingBox.let { (it.min + it.max) / 2 }")
            appendLine(collisionMask(definition.shape))
            if (collisionEvents.isNotEmpty()) {
                appendLine("    override val collidableTypes: List<KClass<out Collidable>> = listOf(${collisionEvents.mapNotNull { it.otherActorType }.distinct().sorted().joinToString { "$it::class" }})")
            }
            lookups.forEach { appendLine("    private lateinit var $it") }
            timers.forEach { timer ->
                appendLine()
                appendLine("    /**")
                appendLine("     * Until Timer $timer finishes, or 0 while it doesn't run, and how often it finishes again, or 0 if it doesn't.")
                appendLine("     */")
                appendLine("    private var ${remainingField(timer)} = 0L")
                appendLine("    private var ${periodField(timer)} = 0L")
            }
            if (lookups.isNotEmpty() || addedEvents.any { it.blocks.isNotEmpty() }) {
                appendLine()
                appendLine("    override fun onAdded(kubriko: Kubriko) {")
                lookups.forEach { appendLine("        ${it.substringBefore(':')} = kubriko.get()") }
                appendEvents(addedEvents, depth = 2, scope = scope)
                appendLine("    }")
            }
            if (isDynamic) {
                appendLine()
                appendLine("    override fun update(deltaTimeInMilliseconds: Int) {")
                timers.forEach { timer -> appendTimer(timer, timerEvents.filter { it.timer == timer }, scope) }
                appendEvents(definition.events.filter { it.type == EventType.EVERY_FRAME }, depth = 2, scope = scope)
                appendLine("        body.position += velocity * (deltaTimeInMilliseconds / 1000f)")
                if (edgeEvents.isNotEmpty()) appendEdgeHandling(edgeEvents, scope)
                appendLine("        collisionMask.position = bodyCenter")
                appendLine("    }")
            }
            // Kubriko keeps telling Actors about key presses while the game is paused.
            if (keyEvents.isNotEmpty()) appendKeyHandling(keyEvents, scope, guard = "if (!$STATE_MANAGER_FIELD.isRunning.value) return")
            if (collisionEvents.isNotEmpty()) appendCollisionHandling(collisionEvents, scope)
            appendLine()
            appendLine(drawFunction(definition.shape))
            if (generatedEvents.any { event -> event.blocks.any { it is BlockDefinition.Log && ".$NUMBER_TEXT_FUNCTION()" in templateLiteral(it.message, scope) } }) {
                appendLine()
                appendLine("    private fun Float.$NUMBER_TEXT_FUNCTION() = if (this % 1f == 0f) toLong().toString() else toString()")
            }
            appendLine()
            appendLine("    override fun save() = State(")
            appendLine("        body = body,")
            appendLine("        color = color,")
            definition.properties.forEach { appendLine("        ${it.name} = ${it.name},") }
            appendLine("    )")
            appendLine()
            appendLine("    @kotlinx.serialization.Serializable")
            appendLine("    data class State(")
            appendLine("        @SerialName(\"body\") val body: SerializableBoxBody = BoxBody(),")
            appendLine("        @SerialName(\"color\") val color: SerializableColor = Color(${colorLiteral(definition.color)}),")
            definition.properties.forEach { property ->
                appendLine("        @SerialName(${stringLiteral(property.name)}) val ${property.name}: ${PropertyKind.of(property).kotlinType} = ${initialValueLiteral(property)},")
            }
            appendLine("    ) : Serializable.State<$name> {")
            appendLine()
            appendLine("        override fun restore() = $name(this)")
            appendLine()
            appendLine("        override fun serialize() = Json.encodeToString(this)")
            appendLine("    }")
            appendLine()
            appendLine("    companion object {")
            appendLine("        val metadata = EditableMetadata.create<$name, State>(typeId = \"$name\") { position ->")
            appendLine("            State(body = BoxBody(initialPosition = position, initialSize = SceneSize(${unitLiteral(definition.width)}, ${unitLiteral(definition.height)})))")
            appendLine("        }")
            appendLine("    }")
            appendLine("}")
        })
    }

    /**
     * The registry of every block-built Actor type, plus the Scene settings ([SceneSettingsCodeGenerator]).
     */
    fun generateRegistry(actorTypeNames: List<String>, packageName: String) = GeneratedFile.withHeader("", buildString {
        appendLine("package $packageName.$GENERATED_PACKAGE")
        appendLine()
        appendLine("import com.pandulapeter.kubriko.sceneEditor.EditableMetadata")
        appendLine()
        appendLine("/**")
        appendLine(" * The Scene metadata of every Actor type built in Kubriko Creator, and of the Scene settings.")
        appendLine(" */")
        appendLine("object $REGISTRY_NAME {")
        appendLine("    val metadata: List<EditableMetadata<*>> = listOf(")
        (actorTypeNames + SceneSettingsCodeGenerator.SETTINGS_NAME).sorted().forEach { appendLine("        $it.metadata,") }
        appendLine("    )")
        appendLine("}")
    })

    private fun StringBuilder.appendEdgeHandling(edgeEvents: List<EventDefinition>, scope: BlockScope) {
        appendLine("        val halfSize = SceneOffset(body.size.width / 2, body.size.height / 2)")
        appendLine("        val sceneSettings = $SCENES_FIELD.settings")
        appendLine("        val topLeft = (sceneSettings?.topLeft ?: viewportManager.topLeft.value) + halfSize")
        appendLine("        val bottomRight = (sceneSettings?.bottomRight ?: viewportManager.bottomRight.value) - halfSize")
        // Strict, so an Actor resting where the clamp below left it doesn't run the Event again every frame.
        appendLine("        val isTouchingLeftEdge = body.position.x < topLeft.x")
        appendLine("        val isTouchingRightEdge = body.position.x > bottomRight.x")
        appendLine("        val isTouchingTopEdge = body.position.y < topLeft.y")
        appendLine("        val isTouchingBottomEdge = body.position.y > bottomRight.y")
        appendLine("        if (isTouchingLeftEdge || isTouchingRightEdge || isTouchingTopEdge || isTouchingBottomEdge) {")
        appendLine("            body.position = body.position.constrainedWithin(topLeft, bottomRight)")
        appendEvents(edgeEvents, depth = 3, scope = scope)
        appendLine("        }")
    }

    private fun StringBuilder.appendCollisionHandling(collisionEvents: List<EventDefinition>, scope: BlockScope) {
        appendLine()
        appendLine("    override fun onCollisionDetected(collidables: List<Collidable>) {")
        appendLine("        for (index in collidables.indices) {")
        appendLine("            when (val other = collidables[index]) {")
        collisionEvents.groupBy { it.otherActorType!! }.toSortedMap().forEach { (otherActorType, events) ->
            appendLine("                is $otherActorType -> {")
            appendEvents(events, depth = 5, scope = scope)
            appendLine("                }")
        }
        appendLine("            }")
        appendLine("        }")
        appendLine("    }")
    }

    private fun collisionMask(shape: ActorShape) = when (shape) {
        ActorShape.CIRCLE -> """
            |    override val collisionMask = CircleCollisionMask(
            |        initialPosition = bodyCenter,
            |        initialRadius = body.size.width * body.scale.horizontal / 2,
            |    )
        """.trimMargin()
        ActorShape.RECTANGLE -> """
            |    override val collisionMask = BoxCollisionMask(
            |        initialPosition = bodyCenter,
            |        initialSize = body.size * body.scale,
            |        initialRotation = body.rotation,
            |    )
        """.trimMargin()
    }

    private fun drawFunction(shape: ActorShape) = when (shape) {
        ActorShape.CIRCLE -> """
            |    override fun DrawScope.draw() = drawCircle(
            |        color = color,
            |        radius = body.size.width.raw / 2,
            |        center = body.size.center.raw,
            |    )
        """.trimMargin()
        ActorShape.RECTANGLE -> """
            |    override fun DrawScope.draw() = drawRect(
            |        color = color,
            |        size = body.size.raw,
            |    )
        """.trimMargin()
    }

    private val BASE_IMPORTS = listOf(
        "androidx.compose.ui.graphics.Color",
        "androidx.compose.ui.graphics.drawscope.DrawScope",
        "com.pandulapeter.kubriko.actor.body.BoxBody",
        "com.pandulapeter.kubriko.actor.traits.Visible",
        "com.pandulapeter.kubriko.helpers.extensions.sceneUnit",
        "com.pandulapeter.kubriko.sceneEditor.Editable",
        "com.pandulapeter.kubriko.sceneEditor.EditableMetadata",
        "com.pandulapeter.kubriko.sceneEditor.Exposed",
        "com.pandulapeter.kubriko.serialization.Serializable",
        "com.pandulapeter.kubriko.serialization.typeSerializers.SerializableBoxBody",
        "com.pandulapeter.kubriko.serialization.typeSerializers.SerializableColor",
        "com.pandulapeter.kubriko.types.SceneSize",
        "kotlinx.serialization.SerialName",
        "kotlinx.serialization.json.Json",
    )

    private val KEYBOARD_MANAGER_IMPORTS = listOf(
        "androidx.compose.ui.input.key.Key",
        "com.pandulapeter.kubriko.keyboardInput.KeyboardInputManager",
    )

    private val KEYBOARD_IMPORTS = listOf(
        "androidx.compose.ui.input.key.Key",
        "com.pandulapeter.kubriko.keyboardInput.KeyboardInputAware",
    )

    /**
     * Whether [this] Block changes where the Actor is or how it moves, which only an Actor that updates every frame
     * does. A Kotlin Block counts too, since it may.
     */
    private val BlockDefinition.movesTheActor
        get() = when (this) {
            is BlockDefinition.SetVelocity, is BlockDefinition.AddToVelocity, BlockDefinition.Stop, is BlockDefinition.JumpTo,
            is BlockDefinition.MoveTowardsNearest, BlockDefinition.BounceOffEdge, BlockDefinition.BounceOffOther, is BlockDefinition.Kotlin -> true
            is BlockDefinition.If -> condition is ConditionDefinition.Velocity
            else -> false
        }

    private fun BlockDefinition.needsActorManager(knownActorTypeNames: Collection<String>) = when (this) {
        BlockDefinition.RemoveSelf, BlockDefinition.RemoveOther, is BlockDefinition.Kotlin -> true
        is BlockDefinition.AddActor -> actorType in knownActorTypeNames
        is BlockDefinition.MoveTowardsNearest -> actorType in knownActorTypeNames
        else -> false
    }
}
