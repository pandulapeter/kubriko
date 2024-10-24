package com.pandulapeter.gameTemplate

import android.os.Build

actual fun getPlatform() = object : Platform {

    override val name = "Android ${Build.VERSION.SDK_INT}"
}