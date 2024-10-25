package com.pandulapeter.gameTemplate.ui

import android.os.Build

internal actual fun getPlatform() = object : Platform {

    override val name = "Android ${Build.VERSION.SDK_INT}"
}