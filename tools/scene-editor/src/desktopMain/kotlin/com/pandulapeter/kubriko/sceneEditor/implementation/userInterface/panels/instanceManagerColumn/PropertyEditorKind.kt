/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn

import androidx.compose.ui.graphics.Color
import com.pandulapeter.kubriko.types.AngleDegrees
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit
import kotlin.reflect.KType
import kotlin.reflect.full.createType

private val booleanType = Boolean::class.createType()
private val colorType = Color::class.createType()
private val angleDegreesType = AngleDegrees::class.createType()
private val angleRadiansType = AngleRadians::class.createType()
private val sceneOffsetType = SceneOffset::class.createType()
private val scaleType = Scale::class.createType()
private val floatType = Float::class.createType()
private val intType = Int::class.createType()
private val sceneUnitType = SceneUnit::class.createType()

internal enum class PropertyEditorKind {
    BOOLEAN,
    COLOR,
    ANGLE_DEGREES,
    ANGLE_RADIANS,
    SCENE_OFFSET,
    SCALE,
    FLOAT,
    INT,
    STRING,
    SCENE_UNIT,
}

/**
 * Picks the editor for a property type. Only [String] is matched regardless of nullability; every other type must
 * match exactly (non-null).
 */
internal fun KType.toPropertyEditorKind(): PropertyEditorKind? = if (classifier == String::class) {
    PropertyEditorKind.STRING
} else when (this) {
    booleanType -> PropertyEditorKind.BOOLEAN
    colorType -> PropertyEditorKind.COLOR
    angleDegreesType -> PropertyEditorKind.ANGLE_DEGREES
    angleRadiansType -> PropertyEditorKind.ANGLE_RADIANS
    sceneOffsetType -> PropertyEditorKind.SCENE_OFFSET
    scaleType -> PropertyEditorKind.SCALE
    floatType -> PropertyEditorKind.FLOAT
    intType -> PropertyEditorKind.INT
    sceneUnitType -> PropertyEditorKind.SCENE_UNIT
    else -> null
}
