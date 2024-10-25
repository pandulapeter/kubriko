package com.pandulapeter.gameTemplate.ui

import platform.UIKit.UIDevice

internal actual fun getPlatform() = object : Platform {

    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}