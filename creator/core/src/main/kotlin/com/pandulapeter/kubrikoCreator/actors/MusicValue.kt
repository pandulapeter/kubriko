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
 * The music a Block plays: a file by its path from `composeResources`, or the one a music Property holds, as [NumberValue] is
 * for numbers.
 *
 * A fixed music is written as its bare path, as those fields were before they could be anything else, and a Property's as
 * an object naming its kind in `type` ([MusicValueSerializer]).
 */
@Serializable(with = MusicValueSerializer::class)
public sealed interface MusicValue {

    /**
     * The same file every time, by its path from `composeResources` (empty until one is chosen).
     */
    public data class Fixed(val path: String) : MusicValue

    @Serializable
    @SerialName("property")
    @Description("The file a music Property holds when the Block runs.")
    public data class Property(
        @Description("The name of one of the music Properties of the type itself, or of the Manager type in manager.")
        @SerialName("property") val property: String = "",
        @Description(MANAGER_DESCRIPTION)
        @SerialName("manager") val manager: String? = null,
    ) : MusicValue

    public companion object {
        internal const val SUMMARY = "A file's path, or a property music value: {\"type\": \"property\", \"property\": name, \"manager\": its Manager type or left out}."
    }
}

/**
 * The music Property [this] value reads, if it reads one.
 */
public val MusicValue.propertyReference: PropertyReference?
    get() = (this as? MusicValue.Property)?.let { PropertyReference(it.property, PropertyKind.MUSIC, it.manager) }

/**
 * [this] value reading the Property [newName] instead, if it reads [oldName] of [manager] (the type's own one, if null).
 */
internal fun MusicValue.withPropertyRenamed(oldName: String, newName: String, manager: String?) =
    if (this is MusicValue.Property && this.manager == manager && property == oldName) copy(property = newName) else this

/**
 * The path of the file [this] value always plays, or null if it reads a Property (or none is chosen yet).
 */
public val MusicValue.fixedPath: String?
    get() = (this as? MusicValue.Fixed)?.path?.ifEmpty { null }

/**
 * [this] value reading its Property from the Manager type [newName] instead, if it reads one of [oldName].
 */
internal fun MusicValue.withManagerRenamed(oldName: String, newName: String) = if (this is MusicValue.Property && manager == oldName) copy(manager = newName) else this
