package com.pandulapeter.gameTemplate.ui

internal interface Platform {
    val name: String
}

internal expect fun getPlatform(): Platform