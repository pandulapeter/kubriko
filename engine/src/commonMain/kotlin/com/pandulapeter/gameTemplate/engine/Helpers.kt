package com.pandulapeter.gameTemplate.engine

fun consume(action: () -> Any?) = action().let { true }