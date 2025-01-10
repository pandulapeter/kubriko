package com.pandulapeter.kubriko.uiComponents

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
) = CircularProgressIndicator(
    modifier = modifier.size(24.dp),
    strokeWidth = 3.dp,
)
