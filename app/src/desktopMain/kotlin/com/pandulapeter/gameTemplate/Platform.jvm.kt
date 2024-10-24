package com.pandulapeter.gameTemplate

actual fun getPlatform() = object : Platform {

    override val name: String = "Java ${System.getProperty("java.version")}"
}