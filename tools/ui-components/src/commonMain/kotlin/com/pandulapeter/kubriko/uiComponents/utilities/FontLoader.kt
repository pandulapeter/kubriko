package com.pandulapeter.kubriko.uiComponents.utilities

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily

// TODO: While something like this would be needed for web, this implementation doesn't seem to work.
@Composable
fun isFontFamilyLoaded(fontFamily: FontFamily) = !fontFamily.toString().contains("emptyFont")