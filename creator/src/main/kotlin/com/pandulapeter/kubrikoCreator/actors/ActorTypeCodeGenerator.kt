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
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.NUMBER_TEXT_FUNCTION
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
     */
    fun generateActorType(
        definition: ActorTypeDefinition,
        packageName: String,
        knownActorTypeNames: Collection<String> = listOf(definition.name),
    ): String {
        val name = definition.name
        val keyEvents = definition.events.filter { it.type == EventType.KEY_PRESSED && it.key != null }
        val edgeEvents = definition.events.filter { it.type == EventType.EDGE_TOUCHED }
        val collisionEvents = definition.events.filter { it.type == EventType.COLLISION && it.otherActorType in knownActorTypeNames }
        val timerEvents = definition.events.filter { it.type == EventType.TIMER_FINISHED && it.timer in TIMER_NUMBERS }
        val generatedEvents = definition.events.filter { it.type == EventType.ADDED_TO_SCENE || it.type == EventType.EVERY_FRAME } + edgeEvents + keyEvents + collisionEvents + timerEvents
        val timers = (timerEvents.mapNotNull { it.timer } + generatedEvents.flatMap { event -> event.blocks.mapNotNull { it.timerNumber } }).filter { it in TIMER_NUMBERS }.distinct().sorted()
        val blocksByEvent = edgeEvents.flatMap { event -> event.blocks.map { it to event.type } } + collisionEvents.flatMap { event -> event.blocks.map { it to event.type } }
        val usesAbsoluteValue = blocksByEvent.any { (block, eventType) -> blockLines(block, eventType, name, definition.properties, knownActorTypeNames).any { "absoluteValue" in it } }
        val generatedBlocks = generatedEvents.flatMap { it.blocks }
        val conditions = generatedBlocks.mapNotNull { (it as? BlockDefinition.If)?.condition }
        val usesKeyHeld = conditions.any { it is ConditionDefinition.KeyHeld }
        val traits = listOfNotNull(
            "Visible",
            "Dynamic",
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
            appendLine()
            appendLine("    /**")
            appendLine("     * In scene units per second: the Actor moves by this much every second.")
            appendLine("     */")
            appendLine("    var velocity = SceneOffset.Zero")
            definition.properties.forEach { property ->
                appendLine()
                appendLine("    @set:Exposed(name = ${stringLiteral(property.name)})")
                appendLine("    var ${property.name}: ${PropertyKind.of(property).kotlinType} = state.${property.name}")
            }
            appendLine(collisionMask(definition.shape))
            if (collisionEvents.isNotEmpty()) {
                appendLine("    override val collidableTypes: List<KClass<out Collidable>> = listOf(${collisionEvents.mapNotNull { it.otherActorType }.distinct().sorted().joinToString { "$it::class" }})")
            }
            appendLine("    private lateinit var actorManager: ActorManager")
            appendLine("    private lateinit var viewportManager: ViewportManager")
            if (usesKeyHeld) appendLine("    private lateinit var keyboardInputManager: KeyboardInputManager")
            timers.forEach { timer ->
                appendLine()
                appendLine("    /**")
                appendLine("     * Until Timer $timer finishes, or 0 while it doesn't run, and how often it finishes again, or 0 if it doesn't.")
                appendLine("     */")
                appendLine("    private var ${remainingField(timer)} = 0L")
                appendLine("    private var ${periodField(timer)} = 0L")
            }
            appendLine()
            appendLine("    override fun onAdded(kubriko: Kubriko) {")
            appendLine("        actorManager = kubriko.get()")
            appendLine("        viewportManager = kubriko.get()")
            if (usesKeyHeld) appendLine("        keyboardInputManager = kubriko.get()")
            appendEvents(definition.events.filter { it.type == EventType.ADDED_TO_SCENE }, depth = 2, definition = definition, knownActorTypeNames = knownActorTypeNames)
            appendLine("    }")
            appendLine()
            appendLine("    override fun update(deltaTimeInMilliseconds: Int) {")
            timers.forEach { timer -> appendTimer(timer, timerEvents.filter { it.timer == timer }, definition, knownActorTypeNames) }
            appendEvents(definition.events.filter { it.type == EventType.EVERY_FRAME }, depth = 2, definition = definition, knownActorTypeNames = knownActorTypeNames)
            appendLine("        body.position += velocity * (deltaTimeInMilliseconds / 1000f)")
            if (edgeEvents.isNotEmpty()) appendEdgeHandling(edgeEvents, definition, knownActorTypeNames)
            appendLine("        collisionMask.position = body.position")
            appendLine("    }")
            if (keyEvents.isNotEmpty()) appendKeyHandling(keyEvents, definition, knownActorTypeNames)
            if (collisionEvents.isNotEmpty()) appendCollisionHandling(collisionEvents, definition, knownActorTypeNames)
            appendLine()
            appendLine(drawFunction(definition.shape))
            if (generatedEvents.any { event -> event.blocks.any { it is BlockDefinition.Log && ".$NUMBER_TEXT_FUNCTION()" in templateLiteral(it.message, definition.properties) } }) {
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

    fun generateRegistry(actorTypeNames: List<String>, packageName: String) = GeneratedFile.withHeader("", buildString {
        appendLine("package $packageName.$GENERATED_PACKAGE")
        appendLine()
        appendLine("import com.pandulapeter.kubriko.sceneEditor.EditableMetadata")
        appendLine()
        appendLine("/**")
        appendLine(" * The Scene metadata of every Actor type built in Kubriko Creator.")
        appendLine(" */")
        appendLine("object $REGISTRY_NAME {")
        if (actorTypeNames.isEmpty()) {
            appendLine("    val metadata: List<EditableMetadata<*>> = emptyList()")
        } else {
            appendLine("    val metadata: List<EditableMetadata<*>> = listOf(")
            actorTypeNames.sorted().forEach { appendLine("        $it.metadata,") }
            appendLine("    )")
        }
        appendLine("}")
    })

    private fun StringBuilder.appendEvents(events: List<EventDefinition>, depth: Int, definition: ActorTypeDefinition, knownActorTypeNames: Collection<String>) {
        val lines = events.flatMap { event -> eventLines(event.blocks, event.type, definition.name, definition.properties, knownActorTypeNames) }
        if (lines.isNotEmpty() && events.first().type != EventType.KEY_PRESSED && events.first().type != EventType.COLLISION && events.first().type != EventType.TIMER_FINISHED) {
            appendLine(INDENT.repeat(depth) + "// " + eventComment(events.first().type))
        }
        lines.forEach { appendLine(INDENT.repeat(depth) + it) }
    }

    private fun eventComment(type: EventType) = when (type) {
        EventType.ADDED_TO_SCENE -> "When added to the Scene"
        EventType.EVERY_FRAME -> "Every frame"
        EventType.KEY_PRESSED -> "When a key is pressed"
        EventType.EDGE_TOUCHED -> "When an edge of the screen is touched"
        EventType.COLLISION -> "When it collides with another Actor"
        EventType.TIMER_FINISHED -> "When a Timer finishes"
    }

    private fun StringBuilder.appendEdgeHandling(edgeEvents: List<EventDefinition>, definition: ActorTypeDefinition, knownActorTypeNames: Collection<String>) {
        appendLine("        val halfSize = SceneOffset(body.size.width / 2, body.size.height / 2)")
        appendLine("        val topLeft = viewportManager.topLeft.value + halfSize")
        appendLine("        val bottomRight = viewportManager.bottomRight.value - halfSize")
        // Strict, so an Actor resting where the clamp below left it doesn't run the Event again every frame.
        appendLine("        val isTouchingLeftEdge = body.position.x < topLeft.x")
        appendLine("        val isTouchingRightEdge = body.position.x > bottomRight.x")
        appendLine("        val isTouchingTopEdge = body.position.y < topLeft.y")
        appendLine("        val isTouchingBottomEdge = body.position.y > bottomRight.y")
        appendLine("        if (isTouchingLeftEdge || isTouchingRightEdge || isTouchingTopEdge || isTouchingBottomEdge) {")
        appendLine("            body.position = body.position.constrainedWithin(topLeft, bottomRight)")
        appendEvents(edgeEvents, depth = 3, definition = definition, knownActorTypeNames = knownActorTypeNames)
        appendLine("        }")
    }

    /**
     * Counts Timer [timer] down and runs the Blocks of its [events] when it finishes. A repeating Timer carries the time
     * it overshot into its next round, and the Blocks may start or stop it again.
     */
    private fun StringBuilder.appendTimer(timer: Int, events: List<EventDefinition>, definition: ActorTypeDefinition, knownActorTypeNames: Collection<String>) {
        val remaining = remainingField(timer)
        val period = periodField(timer)
        appendLine("        if ($remaining > 0) {")
        appendLine("            $remaining -= deltaTimeInMilliseconds")
        appendLine("            if ($remaining <= 0) {")
        appendLine("                // When Timer $timer finishes")
        appendLine("                $remaining = if ($period > 0) ($remaining + $period).coerceAtLeast(1) else 0")
        appendEvents(events, depth = 4, definition = definition, knownActorTypeNames = knownActorTypeNames)
        appendLine("            }")
        appendLine("        }")
    }


    private fun StringBuilder.appendKeyHandling(keyEvents: List<EventDefinition>, definition: ActorTypeDefinition, knownActorTypeNames: Collection<String>) {
        appendLine()
        appendLine("    override fun onKeyPressed(key: Key) {")
        appendLine("        when (key) {")
        keyEvents.groupBy { it.key!! }.toSortedMap().forEach { (key, events) ->
            appendLine("            Key.${key.composeKeyName} -> {")
            appendEvents(events, depth = 4, definition = definition, knownActorTypeNames = knownActorTypeNames)
            appendLine("            }")
        }
        appendLine("        }")
        appendLine("    }")
    }

    private fun StringBuilder.appendCollisionHandling(collisionEvents: List<EventDefinition>, definition: ActorTypeDefinition, knownActorTypeNames: Collection<String>) {
        appendLine()
        appendLine("    override fun onCollisionDetected(collidables: List<Collidable>) {")
        appendLine("        for (index in collidables.indices) {")
        appendLine("            when (val other = collidables[index]) {")
        collisionEvents.groupBy { it.otherActorType!! }.toSortedMap().forEach { (otherActorType, events) ->
            appendLine("                is $otherActorType -> {")
            appendEvents(events, depth = 5, definition = definition, knownActorTypeNames = knownActorTypeNames)
            appendLine("                }")
        }
        appendLine("            }")
        appendLine("        }")
        appendLine("    }")
    }

    private fun collisionMask(shape: ActorShape) = when (shape) {
        ActorShape.CIRCLE -> """
            |    override val collisionMask = CircleCollisionMask(
            |        initialPosition = body.position,
            |        initialRadius = body.size.width / 2,
            |    )
        """.trimMargin()
        ActorShape.RECTANGLE -> """
            |    override val collisionMask = BoxCollisionMask(
            |        initialPosition = body.position,
            |        initialSize = body.size,
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
        "com.pandulapeter.kubriko.Kubriko",
        "com.pandulapeter.kubriko.actor.body.BoxBody",
        "com.pandulapeter.kubriko.actor.traits.Dynamic",
        "com.pandulapeter.kubriko.actor.traits.Visible",
        "com.pandulapeter.kubriko.helpers.extensions.get",
        "com.pandulapeter.kubriko.helpers.extensions.sceneUnit",
        "com.pandulapeter.kubriko.manager.ActorManager",
        "com.pandulapeter.kubriko.manager.ViewportManager",
        "com.pandulapeter.kubriko.sceneEditor.Editable",
        "com.pandulapeter.kubriko.sceneEditor.EditableMetadata",
        "com.pandulapeter.kubriko.sceneEditor.Exposed",
        "com.pandulapeter.kubriko.serialization.Serializable",
        "com.pandulapeter.kubriko.serialization.typeSerializers.SerializableBoxBody",
        "com.pandulapeter.kubriko.serialization.typeSerializers.SerializableColor",
        "com.pandulapeter.kubriko.types.SceneOffset",
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
}
