package com.pandulapeter.kubriko.implementation.extensions

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds

internal fun List<Modifier>.fold() = fold(Modifier.fillMaxSize().clipToBounds()) { compoundModifier, managerModifier -> compoundModifier then managerModifier }