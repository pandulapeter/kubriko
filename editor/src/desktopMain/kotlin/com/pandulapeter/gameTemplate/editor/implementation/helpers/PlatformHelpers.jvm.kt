package com.pandulapeter.gameTemplate.editor.implementation.helpers

import java.io.File
import kotlin.system.exitProcess

internal actual fun exitApp(): Nothing = exitProcess(0)

internal actual suspend fun loadFile(path: String): String? = try {
    File(path).readBytes().decodeToString()
} catch (_: Exception) {
    null
}

internal actual suspend fun saveFile(path: String, content: String) = File(path).let { file ->
    file.parentFile?.mkdirs()
    if (!file.exists()) {
        file.createNewFile()
    }
    file.writeText(content)
}