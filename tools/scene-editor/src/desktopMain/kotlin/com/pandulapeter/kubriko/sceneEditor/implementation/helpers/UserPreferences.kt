package com.pandulapeter.kubriko.sceneEditor.implementation.helpers

import com.pandulapeter.kubriko.persistence.PersistenceManager
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.settings.AngleEditorMode
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.settings.ColorEditorMode

internal class UserPreferences(persistenceManager: PersistenceManager) {

    val colorEditorMode = persistenceManager.generic(
        key = "colorEditorMode",
        defaultValue = ColorEditorMode.HSV,
        serializer = { it.serializedName },
        deserializer = { it.toColorEditorMode },
    )
    val angleEditorMode = persistenceManager.generic(
        key = "angleEditorMode",
        defaultValue = AngleEditorMode.DEGREES,
        serializer = { it.serializedName },
        deserializer = { it.toAngleEditorMode },
    )
    val isDebugMenuEnabled = persistenceManager.boolean("isDebugMenuEnabled")

    private val ColorEditorMode.serializedName
        get() = when (this) {
            ColorEditorMode.HSV -> "hsv"
            ColorEditorMode.RGB -> "rgb"
        }

    private val String.toColorEditorMode get() = ColorEditorMode.entries.firstOrNull { it.serializedName == this } ?: ColorEditorMode.HSV

    private val AngleEditorMode.serializedName
        get() = when (this) {
            AngleEditorMode.DEGREES -> "degrees"
            AngleEditorMode.RADIANS -> "radians"
        }

    private val String.toAngleEditorMode get() = AngleEditorMode.entries.firstOrNull { it.serializedName == this } ?: AngleEditorMode.DEGREES
}
