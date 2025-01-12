package com.pandulapeter.kubriko.shared

import com.pandulapeter.kubriko.Kubriko

interface StateHolder {

    //    val kubriko: StateFlow<Kubriko?>
    val kubriko: Kubriko?

    fun dispose()
}