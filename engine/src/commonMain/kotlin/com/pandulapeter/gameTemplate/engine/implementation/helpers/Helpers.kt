package com.pandulapeter.gameTemplate.engine.implementation.helpers

internal fun consume(action: () -> Any?) = action().let { true }