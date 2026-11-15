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

import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The sound a Block plays: a file by its path from `composeResources`, or the one a sound Property holds, as [NumberValue] is
 * for numbers.
 *
 * A fixed sound is written as its bare path, as those fields were before they could be anything else, and a Property's as
 * an object naming its kind in `type` ([SoundValueSerializer]).
 */
@Serializable(with = SoundValueSerializer::class)
public sealed interface SoundValue {

    /**
     * The same file every time, by its path from `composeResources` (empty until one is chosen).
     */
    public data class Fixed(val path: String) : SoundValue

    @Serializable
    @SerialName("property")
    @Description("The file a sound Property holds when the Block runs.")
    public data class Property(
        @Description("The name of one of the sound Properties of the type itself, or of the Manager type in manager.")
        @SerialName("property") val property: String = "",
        @Description(MANAGER_DESCRIPTION)
        @SerialName("manager") val manager: String? = null,
    ) : SoundValue

    public companion object {
        internal const val SUMMARY = "A file's path, or a property sound value: {\"type\": \"property\", \"property\": name, \"manager\": its Manager type or left out}."
    }
}

/**
 * The sound Property [this] value reads, if it reads one.
 */
public val SoundValue.propertyReference: PropertyReference?
    get() = (this as? SoundValue.Property)?.let { PropertyReference(it.property, PropertyKind.SOUND, it.manager) }

/**
 * [this] value reading the Property [newName] instead, if it reads [oldName] of [manager] (the type's own one, if null).
 */
internal fun SoundValue.withPropertyRenamed(oldName: String, newName: String, manager: String?) =
    if (this is SoundValue.Property && this.manager == manager && property == oldName) copy(property = newName) else this

/**
 * The path of the file [this] value always plays, or null if it reads a Property (or none is chosen yet).
 */
public val SoundValue.fixedPath: String?
    get() = (this as? SoundValue.Fixed)?.path?.ifEmpty { null }

/**
 * [this] value reading its Property from the Manager type [newName] instead, if it reads one of [oldName].
 */
internal fun SoundValue.withManagerRenamed(oldName: String, newName: String) = if (this is SoundValue.Property && manager == oldName) copy(manager = newName) else this
