package com.pandulapeter.gameTemplate.ui

internal actual fun getPlatform() = object : Platform {

    override val name: String = "Java ${System.getProperty("java.version")}"
}