/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.persistence

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Returns a [MutableStateFlow] for an enum value, stored by the [name][Enum.name] of its entry.
 *
 * Changes to the flow's value will be automatically saved to local storage. A stored name that no longer matches any
 * entry (one that was renamed or removed since) reads as [defaultValue], so renaming an entry loses the choices saved
 * under its old name.
 *
 * @param key The unique key for this value.
 * @param defaultValue The value to return if no value is found in storage, or if the stored one names no entry.
 */
inline fun <reified T : Enum<T>> PersistenceManager.enum(
    key: String,
    defaultValue: T,
): MutableStateFlow<T> = generic(
    key = key,
    defaultValue = defaultValue,
    serializer = { it.name },
    deserializer = { name -> enumEntryNamed(enumValues<T>(), name, defaultValue) },
)

@PublishedApi
internal fun <T : Enum<T>> enumEntryNamed(entries: Array<T>, name: String, defaultValue: T): T = entries.firstOrNull { it.name == name } ?: defaultValue
