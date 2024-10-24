package com.pandulapeter.gameTemplate

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform