package com.pandulapeter.kubriko.sceneEditor.implementation.helpers

import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.settings.AngleEditorMode
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.settings.ColorEditorMode
import java.util.prefs.Preferences
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

internal class UserPreferences {

    private val preferences by lazy { Preferences.userRoot().node("kubrikoSceneEditor") }
    var colorEditorMode by GenericPreferenceDelegate(
        key = "colorEditorMode",
        serializer = { it.serializedName },
        deserializer = { it.toColorEditorMode },
    )
    var angleEditorMode by GenericPreferenceDelegate(
        key = "angleEditorMode",
        serializer = { it.serializedName },
        deserializer = { it.toAngleEditorMode },
    )
    var isDebugMenuEnabled by BooleanPreferenceDelegate(
        key = "isDebugMenuEnabled",
    )

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

    private class BooleanPreferenceDelegate(
        private val key: String,
    ) : ReadWriteProperty<UserPreferences, Boolean> {

        override fun getValue(thisRef: UserPreferences, property: KProperty<*>) = thisRef.preferences.getBoolean(key, false)

        override fun setValue(thisRef: UserPreferences, property: KProperty<*>, value: Boolean) = thisRef.preferences.putBoolean(key, value)
    }

    private class GenericPreferenceDelegate<T>(
        private val key: String,
        private val serializer: (T) -> String,
        private val deserializer: (String) -> T,
    ) : ReadWriteProperty<UserPreferences, T> {

        override fun getValue(thisRef: UserPreferences, property: KProperty<*>) = deserializer(thisRef.preferences.get(key, "").orEmpty())

        override fun setValue(thisRef: UserPreferences, property: KProperty<*>, value: T) = thisRef.preferences.put(key, serializer(value))
    }
}