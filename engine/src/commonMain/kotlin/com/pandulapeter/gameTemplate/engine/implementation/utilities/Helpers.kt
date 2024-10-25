package com.pandulapeter.gameTemplate.engine.implementation.utilities

fun consume(action: () -> Any?) = action().let { true }