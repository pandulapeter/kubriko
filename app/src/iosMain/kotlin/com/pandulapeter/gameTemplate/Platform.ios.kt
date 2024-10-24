package com.pandulapeter.gameTemplate

import platform.UIKit.UIDevice

actual fun getPlatform() = object : Platform {

    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}