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

import kotlin.math.absoluteValue

/**
 * Turns block-built Actor types into ordinary Kotlin: one class per type, plus a registry of all of them.
 *
 * The output is meant to read like hand-written code, so "Convert to code" leaves something a person would keep.
 */
internal object ActorTypeCodeGenerator {

    const val GENERATED_PACKAGE = "generated"
    const val REGISTRY_NAME = "GeneratedActorTypes"
    private const val INDENT = "    "
    private const val NUMBER_TEXT_FUNCTION = "asText"

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
        val generatedEvents = definition.events.filter { it.type == EventType.ADDED_TO_SCENE || it.type == EventType.EVERY_FRAME } + edgeEvents + keyEvents + collisionEvents
        val blocksByEvent = edgeEvents.flatMap { event -> event.blocks.map { it to event.type } } + collisionEvents.flatMap { event -> event.blocks.map { it to event.type } }
        val usesAbsoluteValue = blocksByEvent.any { (block, eventType) -> blockLines(block, eventType, name, definition.properties).any { "absoluteValue" in it } }
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
            appendLine()
            appendLine("    override fun onAdded(kubriko: Kubriko) {")
            appendLine("        actorManager = kubriko.get()")
            appendLine("        viewportManager = kubriko.get()")
            appendEvents(definition.events.filter { it.type == EventType.ADDED_TO_SCENE }, depth = 2, definition = definition)
            appendLine("    }")
            appendLine()
            appendLine("    override fun update(deltaTimeInMilliseconds: Int) {")
            appendEvents(definition.events.filter { it.type == EventType.EVERY_FRAME }, depth = 2, definition = definition)
            appendLine("        body.position += velocity * (deltaTimeInMilliseconds / 1000f)")
            if (edgeEvents.isNotEmpty()) appendEdgeHandling(edgeEvents, definition)
            appendLine("        collisionMask.position = body.position")
            appendLine("    }")
            if (keyEvents.isNotEmpty()) appendKeyHandling(keyEvents, definition)
            if (collisionEvents.isNotEmpty()) appendCollisionHandling(collisionEvents, definition)
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

    private fun StringBuilder.appendEvents(events: List<EventDefinition>, depth: Int, definition: ActorTypeDefinition) {
        val lines = events.flatMap { event -> event.blocks.flatMap { block -> blockLines(block, event.type, definition.name, definition.properties) } }
        if (lines.isNotEmpty() && events.first().type != EventType.KEY_PRESSED && events.first().type != EventType.COLLISION) {
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
    }

    private fun StringBuilder.appendEdgeHandling(edgeEvents: List<EventDefinition>, definition: ActorTypeDefinition) {
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
        appendEvents(edgeEvents, depth = 3, definition = definition)
        appendLine("        }")
    }

    private fun StringBuilder.appendKeyHandling(keyEvents: List<EventDefinition>, definition: ActorTypeDefinition) {
        appendLine()
        appendLine("    override fun onKeyPressed(key: Key) {")
        appendLine("        when (key) {")
        keyEvents.groupBy { it.key!! }.toSortedMap().forEach { (key, events) ->
            appendLine("            Key.${key.composeKeyName} -> {")
            appendEvents(events, depth = 4, definition = definition)
            appendLine("            }")
        }
        appendLine("        }")
        appendLine("    }")
    }

    private fun StringBuilder.appendCollisionHandling(collisionEvents: List<EventDefinition>, definition: ActorTypeDefinition) {
        appendLine()
        appendLine("    override fun onCollisionDetected(collidables: List<Collidable>) {")
        appendLine("        for (index in collidables.indices) {")
        appendLine("            when (val other = collidables[index]) {")
        collisionEvents.groupBy { it.otherActorType!! }.toSortedMap().forEach { (otherActorType, events) ->
            appendLine("                is $otherActorType -> {")
            appendEvents(events, depth = 5, definition = definition)
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

    /**
     * The statements of [block] in an Event of [eventType]. Blocks that need what only one Event knows become a comment
     * anywhere else, so that a definition edited by hand still compiles.
     */
    internal fun blockLines(
        block: BlockDefinition,
        eventType: EventType,
        actorTypeName: String,
        properties: List<PropertyDefinition> = emptyList(),
    ): List<String> = when (block) {
        is BlockDefinition.SetVelocity -> listOf("velocity = ${offsetLiteral(block.x, block.y)}")
        is BlockDefinition.AddToVelocity -> listOf("velocity += ${offsetLiteral(block.x, block.y)}")
        BlockDefinition.Stop -> listOf("velocity = SceneOffset.Zero")
        is BlockDefinition.JumpTo -> listOf("body.position = ${offsetLiteral(block.x, block.y)}")
        BlockDefinition.BounceOffEdge -> if (eventType == EventType.EDGE_TOUCHED) {
            listOf(
                "if (isTouchingLeftEdge) velocity = SceneOffset(velocity.x.raw.absoluteValue.sceneUnit, velocity.y)",
                "if (isTouchingRightEdge) velocity = SceneOffset(-velocity.x.raw.absoluteValue.sceneUnit, velocity.y)",
                "if (isTouchingTopEdge) velocity = SceneOffset(velocity.x, velocity.y.raw.absoluteValue.sceneUnit)",
                "if (isTouchingBottomEdge) velocity = SceneOffset(velocity.x, -velocity.y.raw.absoluteValue.sceneUnit)",
            )
        } else {
            listOf("// Bounce off the edge only works in the Edge touched Event.")
        }
        BlockDefinition.RemoveSelf -> listOf("actorManager.remove(this)")
        BlockDefinition.BounceOffOther -> if (eventType == EventType.COLLISION) {
            listOf(
                "run {",
                "    val offset = body.position - other.body.position",
                "    if (offset.x.raw.absoluteValue * (body.size.height + other.body.size.height).raw >= offset.y.raw.absoluteValue * (body.size.width + other.body.size.width).raw) {",
                "        velocity = SceneOffset(if (offset.x > SceneUnit.Zero) velocity.x.raw.absoluteValue.sceneUnit else -velocity.x.raw.absoluteValue.sceneUnit, velocity.y)",
                "    } else {",
                "        velocity = SceneOffset(velocity.x, if (offset.y > SceneUnit.Zero) velocity.y.raw.absoluteValue.sceneUnit else -velocity.y.raw.absoluteValue.sceneUnit)",
                "    }",
                "}",
            )
        } else {
            listOf("// Bounce off it only works in a collision Event.")
        }
        BlockDefinition.RemoveOther -> if (eventType == EventType.COLLISION) {
            listOf("actorManager.remove(other)")
        } else {
            listOf("// Remove the other Actor only works in a collision Event.")
        }
        is BlockDefinition.SetNumber -> propertyLines(block, properties) { "${block.property} = ${floatLiteral(block.value)}" }
        is BlockDefinition.ChangeNumber -> propertyLines(block, properties) { "${block.property} += ${floatLiteral(block.by)}" }
        is BlockDefinition.SetText -> propertyLines(block, properties) { "${block.property} = ${stringLiteral(block.value)}" }
        is BlockDefinition.SetToggle -> propertyLines(block, properties) { "${block.property} = ${block.value}" }
        is BlockDefinition.Log -> listOf("Logger.log(message = ${templateLiteral(block.message, properties)}, source = ${stringLiteral(actorTypeName)})")
        // Each Kotlin Block gets a scope of its own, like GameMaker's code actions, so two of them can declare the same name.
        is BlockDefinition.Kotlin -> block.code.lines().map { it.trimEnd() }.dropWhile { it.isEmpty() }.dropLastWhile { it.isEmpty() }
            .takeIf { it.isNotEmpty() }
            ?.let { lines -> listOf("run {") + lines.map { if (it.isEmpty()) it else INDENT + it } + "}" }
            .orEmpty()
    }

    /**
     * The statement of a Block that changes a Property, or a comment if the Actor type has no Property of that name
     * and kind (one that was renamed, deleted, or not chosen yet), so that the generated code always compiles.
     */
    private fun propertyLines(block: BlockDefinition, properties: List<PropertyDefinition>, statement: () -> String): List<String> {
        val (name, kind) = checkNotNull(block.propertyReference)
        return if (properties.any { it.name == name && PropertyKind.of(it) == kind }) listOf(statement()) else listOf("// Choose a Property for this Block.")
    }

    /**
     * [text] as a string literal in which `{name}` is the value of the Property called name (a whole number without
     * its ".0", through [NUMBER_TEXT_FUNCTION]). Braces around anything else stay as they are.
     */
    internal fun templateLiteral(text: String, properties: List<PropertyDefinition>): String {
        val propertiesByName = properties.associateBy { it.name }
        var end = 0
        return buildString {
            append('"')
            propertyPlaceholder.findAll(text).filter { it.groupValues[1] in propertiesByName }.forEach { match ->
                val name = match.groupValues[1]
                appendEscaped(text.substring(end, match.range.first))
                append("\${").append(name)
                if (propertiesByName.getValue(name) is PropertyDefinition.Number) append(".$NUMBER_TEXT_FUNCTION()")
                append('}')
                end = match.range.last + 1
            }
            appendEscaped(text.substring(end))
            append('"')
        }
    }

    internal fun stringLiteral(text: String) = buildString {
        append('"')
        appendEscaped(text)
        append('"')
    }

    private fun StringBuilder.appendEscaped(text: String) {
        text.forEach { character ->
            when (character) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '$' -> append("\\$")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(character)
            }
        }
    }

    private fun initialValueLiteral(property: PropertyDefinition) = when (property) {
        is PropertyDefinition.Number -> floatLiteral(property.initialValue)
        is PropertyDefinition.Text -> stringLiteral(property.initialValue)
        is PropertyDefinition.Toggle -> property.initialValue.toString()
    }

    private val propertyPlaceholder = Regex("\\{([a-z][A-Za-z0-9]*)\\}")

    private fun offsetLiteral(x: Float, y: Float) = "SceneOffset(${unitLiteral(x)}, ${unitLiteral(y)})"

    internal fun unitLiteral(value: Float) = "${floatLiteral(value)}.sceneUnit"

    internal fun floatLiteral(value: Float) = when {
        !value.isFinite() -> "0f"
        value == value.toLong().toFloat() && value.absoluteValue < 1e9f -> "${value.toLong()}f"
        else -> "${value}f"
    }

    internal fun colorLiteral(color: Long) = "0x" + (color and 0xFFFFFFFFL).toString(16).uppercase().padStart(8, '0')

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

    private val KEYBOARD_IMPORTS = listOf(
        "androidx.compose.ui.input.key.Key",
        "com.pandulapeter.kubriko.keyboardInput.KeyboardInputAware",
    )
}
