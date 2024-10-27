package com.pandulapeter.gameTemplate.editor.implementation.helpers

internal expect fun exitApp(): Nothing

internal expect suspend fun loadFile(path: String): String?

internal expect suspend fun saveFile(path: String, content: String)