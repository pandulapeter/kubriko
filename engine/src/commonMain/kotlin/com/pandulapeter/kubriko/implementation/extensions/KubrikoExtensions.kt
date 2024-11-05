package com.pandulapeter.kubriko.implementation.extensions

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.manager.Manager

inline fun <reified T : Manager> Kubriko.get() = get(T::class)

inline fun <reified T : Manager> Kubriko.require() = require(T::class)