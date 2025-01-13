package com.pandulapeter.kubriko.shared

import com.pandulapeter.kubriko.Kubriko
import kotlinx.coroutines.flow.Flow

interface StateHolder {

    val kubriko: Flow<Kubriko?>

    fun dispose()
}