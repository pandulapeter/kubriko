/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.serialization.typeSerializers

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.actor.body.PointBody
import com.pandulapeter.kubriko.helpers.extensions.deg
import com.pandulapeter.kubriko.helpers.extensions.rad
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import com.pandulapeter.kubriko.types.SceneUnit
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TypeSerializerRoundTripTest {

    @Test
    fun floatBasedValuesRoundTripBitForBit() = FLOAT_CASES.forEach { case -> case.assertRoundTrips(Json, case.finiteValues) }

    @Test
    fun anglesComeBackNormalized() {
        (FLOATS + listOf(-90f, 450f)).forEach { value ->
            val angle = value.deg
            val decoded = Json.decodeFromString(AngleDegreesSerializer, Json.encodeToString(AngleDegreesSerializer, angle))
            assertBitsEqual(angle.normalized, decoded.raw, "$value degrees")
        }
        (FLOATS + listOf((-PI / 2).toFloat(), (5 * PI / 2).toFloat())).forEach { value ->
            val angle = value.rad
            val decoded = Json.decodeFromString(AngleRadiansSerializer, Json.encodeToString(AngleRadiansSerializer, angle))
            assertBitsEqual(angle.normalized, decoded.raw, "$value radians")
        }
    }

    @Test
    fun colorsRoundTripLosslessly() = listOf(Color.Black, Color.Transparent, Color(0x80FF0000), Color(0x12345678)).forEach { color ->
        assertEquals(color.value, Json.decodeFromString(ColorSerializer, Json.encodeToString(ColorSerializer, color)).value)
    }

    @Test
    fun nonFiniteValuesRoundTripOnlyWhenJsonAllowsThem() {
        FLOAT_CASES.forEach { case ->
            case.assertRoundTrips(SPECIAL_FLOATS_JSON, case.nonFiniteValues)
            case.assertNonFiniteValuesAreRejectedByDefault()
        }
        listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY).forEach { value ->
            val degrees = SPECIAL_FLOATS_JSON.decodeFromString(AngleDegreesSerializer, SPECIAL_FLOATS_JSON.encodeToString(AngleDegreesSerializer, value.deg))
            assertTrue(degrees.raw.isNaN(), "$value degrees came back as ${degrees.raw}")
            val radians = SPECIAL_FLOATS_JSON.decodeFromString(AngleRadiansSerializer, SPECIAL_FLOATS_JSON.encodeToString(AngleRadiansSerializer, value.rad))
            assertTrue(radians.raw.isNaN(), "$value radians came back as ${radians.raw}")
            assertFailsWith<SerializationException> { Json.encodeToString(AngleDegreesSerializer, value.deg) }
            assertFailsWith<SerializationException> { Json.encodeToString(AngleRadiansSerializer, value.rad) }
        }
    }

    @Test
    fun unknownKeysAreRejectedUnlessIgnored() = STRUCTURED_CASES.forEach { case -> case.assertUnknownKeysAreRejectedUnlessIgnored() }

    @Test
    fun missingElementsDecodeAsTheLoopDefaults() = STRUCTURED_CASES.forEach { case -> case.assertMissingElementsDecodeAsDefaults() }

    /**
     * One float-based serializer: [components] flattens a value for bitwise comparison.
     */
    private class Case<T>(
        val name: String,
        val serializer: KSerializer<T>,
        val finiteValues: List<T>,
        val nonFiniteValues: List<T>,
        val components: (T) -> List<Float>,
        val valueForMissingElements: T? = null,
        val missingElementDefaults: List<Pair<Int, T>> = emptyList(),
    ) {
        fun assertRoundTrips(json: Json, values: List<T>) = values.forEach { value ->
            assertSameComponents(value, json.decodeFromString(serializer, json.encodeToString(serializer, value)))
        }

        fun assertNonFiniteValuesAreRejectedByDefault() = nonFiniteValues.forEach { value ->
            assertFailsWith<SerializationException>(name) { Json.encodeToString(serializer, value) }
        }

        fun assertUnknownKeysAreRejectedUnlessIgnored() = finiteValues.forEach { value ->
            val encoded = Json.encodeToJsonElement(serializer, value).jsonObject
            val withUnknownKey = JsonObject(encoded + ("unknownKey" to JsonPrimitive(1)))
            assertFailsWith<SerializationException>(name) { Json.decodeFromJsonElement(serializer, withUnknownKey) }
            assertSameComponents(value, IGNORE_UNKNOWN_KEYS_JSON.decodeFromJsonElement(serializer, withUnknownKey))
        }

        fun assertMissingElementsDecodeAsDefaults() {
            val encoded = Json.encodeToJsonElement(serializer, checkNotNull(valueForMissingElements)).jsonObject
            missingElementDefaults.forEach { (elementIndex, expected) ->
                val elementName = serializer.descriptor.getElementName(elementIndex)
                assertSameComponents(expected, Json.decodeFromJsonElement(serializer, JsonObject(encoded - elementName)), "$name without $elementName")
            }
        }

        fun assertSameComponents(expected: T, actual: T, message: String = name) {
            val expectedComponents = components(expected)
            val actualComponents = components(actual)
            assertEquals(expectedComponents.size, actualComponents.size, message)
            expectedComponents.indices.forEach { index ->
                assertBitsEqual(expectedComponents[index], actualComponents[index], "$message, component $index: $expectedComponents vs $actualComponents")
            }
        }
    }

    private companion object {
        val FLOATS = listOf(0f, -0f, 1f, -3.5f, 1e7f, 1e-7f, 0.1f + 0.2f)
        val NON_FINITE_FLOATS = listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)
        val SPECIAL_FLOATS_JSON = Json { allowSpecialFloatingPointValues = true }
        val IGNORE_UNKNOWN_KEYS_JSON = Json { ignoreUnknownKeys = true }

        fun assertBitsEqual(expected: Float, actual: Float, message: String) = if (expected.isNaN()) {
            assertTrue(actual.isNaN(), message)
        } else {
            assertEquals(expected.toRawBits(), actual.toRawBits(), message)
        }

        fun offset(x: Float, y: Float) = SceneOffset(x.sceneUnit, y.sceneUnit)

        fun size(width: Float, height: Float) = SceneSize(width.sceneUnit, height.sceneUnit)

        fun boxBody(
            position: SceneOffset = offset(1f, -3.5f),
            size: SceneSize = size(10f, 20f),
            pivot: SceneOffset = offset(2f, 3f),
            scale: Scale = Scale(1.5f, -0.5f),
            rotation: AngleRadians = 1f.rad,
        ) = BoxBody(
            initialPosition = position,
            initialSize = size,
            initialPivot = pivot,
            initialScale = scale,
            initialRotation = rotation,
        )

        fun <T> pairs(create: (Float, Float) -> T) = FLOATS.map { create(it, it) } + create(1f, -3.5f)

        fun <T> nonFinitePairs(create: (Float, Float) -> T) = NON_FINITE_FLOATS.map { create(it, 1f) } + NON_FINITE_FLOATS.map { create(1f, it) }

        val SCENE_UNIT_CASE = Case(
            name = "SceneUnit",
            serializer = SceneUnitSerializer,
            finiteValues = FLOATS.map { it.sceneUnit },
            nonFiniteValues = NON_FINITE_FLOATS.map { it.sceneUnit },
            components = { listOf(it.raw) },
        )
        val OFFSET_CASE = Case(
            name = "Offset",
            serializer = OffsetSerializer,
            finiteValues = pairs(::Offset),
            nonFiniteValues = nonFinitePairs(::Offset),
            components = { listOf(it.x, it.y) },
            valueForMissingElements = Offset(2f, 3f),
            missingElementDefaults = listOf(0 to Offset(0f, 3f), 1 to Offset(2f, 0f)),
        )
        val SCENE_OFFSET_CASE = Case(
            name = "SceneOffset",
            serializer = SceneOffsetSerializer,
            finiteValues = pairs(::offset),
            nonFiniteValues = nonFinitePairs(::offset),
            components = { listOf(it.x.raw, it.y.raw) },
            valueForMissingElements = offset(2f, 3f),
            missingElementDefaults = listOf(0 to offset(0f, 3f), 1 to offset(2f, 0f)),
        )
        val SIZE_CASE = Case(
            name = "Size",
            serializer = SizeSerializer,
            finiteValues = pairs(::Size),
            nonFiniteValues = nonFinitePairs(::Size),
            components = { listOf(it.width, it.height) },
            valueForMissingElements = Size(2f, 3f),
            missingElementDefaults = listOf(0 to Size(0f, 3f), 1 to Size(2f, 0f)),
        )
        val SCENE_SIZE_CASE = Case(
            name = "SceneSize",
            serializer = SceneSizeSerializer,
            finiteValues = pairs(::size),
            nonFiniteValues = nonFinitePairs(::size),
            components = { listOf(it.width.raw, it.height.raw) },
            valueForMissingElements = size(2f, 3f),
            missingElementDefaults = listOf(0 to size(0f, 3f), 1 to size(2f, 0f)),
        )
        val SCALE_CASE = Case(
            name = "Scale",
            serializer = ScaleSerializer,
            finiteValues = pairs(::Scale),
            nonFiniteValues = nonFinitePairs(::Scale),
            components = { listOf(it.horizontal, it.vertical) },
            valueForMissingElements = Scale(2f, 3f),
            missingElementDefaults = listOf(0 to Scale(0f, 3f), 1 to Scale(2f, 0f)),
        )
        val POINT_BODY_CASE = Case<PointBody>(
            name = "PointBody",
            serializer = PointBodySerializer,
            finiteValues = pairs { x, y -> PointBody(offset(x, y)) },
            nonFiniteValues = nonFinitePairs { x, y -> PointBody(offset(x, y)) },
            components = { listOf(it.position.x.raw, it.position.y.raw) },
            valueForMissingElements = PointBody(offset(2f, 3f)),
            missingElementDefaults = listOf(0 to PointBody(SceneOffset.Zero)),
        )
        val BOX_BODY_CASE = Case(
            name = "BoxBody",
            serializer = BoxBodySerializer,
            finiteValues = FLOATS.map { value ->
                boxBody(position = offset(value, value), size = size(10f + value, 20f), rotation = value.rad)
            } + boxBody(),
            nonFiniteValues = NON_FINITE_FLOATS.map { value ->
                boxBody(position = offset(value, 1f), scale = Scale(1f, value))
            },
            components = { body ->
                listOf(
                    body.position.x.raw,
                    body.position.y.raw,
                    body.size.width.raw,
                    body.size.height.raw,
                    body.pivot.x.raw,
                    body.pivot.y.raw,
                    body.scale.horizontal,
                    body.scale.vertical,
                    body.rotation.normalized,
                )
            },
            valueForMissingElements = boxBody(),
            missingElementDefaults = listOf(
                0 to boxBody(position = SceneOffset.Zero),
                1 to boxBody(size = SceneSize.Zero),
                3 to boxBody(scale = Scale.Unit),
                4 to boxBody(rotation = AngleRadians.Zero),
            ),
        )
        val STRUCTURED_CASES = listOf(OFFSET_CASE, SCENE_OFFSET_CASE, SIZE_CASE, SCENE_SIZE_CASE, SCALE_CASE, POINT_BODY_CASE, BOX_BODY_CASE)
        val FLOAT_CASES = listOf(SCENE_UNIT_CASE) + STRUCTURED_CASES
    }
}
