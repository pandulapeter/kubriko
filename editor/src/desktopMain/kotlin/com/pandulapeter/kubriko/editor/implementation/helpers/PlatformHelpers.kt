package com.pandulapeter.kubriko.editor.implementation.helpers

import java.io.File
import kotlin.system.exitProcess

internal fun exitApp(): Nothing = exitProcess(0)

internal suspend fun loadFile(path: String): String? = try {
    File(path).readBytes().decodeToString()
} catch (_: Exception) {
    null
}

internal suspend fun saveFile(path: String, content: String) = File(path).let { file ->
    file.parentFile?.mkdirs()
    if (!file.exists()) {
        file.createNewFile()
    }
    file.writeText(content)
}