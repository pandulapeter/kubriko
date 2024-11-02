package com.pandulapeter.kubrikoPong.implementation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoPong.implementation.helpers.platformName
import kubriko.games.pong.generated.resources.Res
import kubriko.games.pong.generated.resources.logo
import org.jetbrains.compose.resources.painterResource

@Composable
fun UserInterface(
    modifier: Modifier = Modifier,
    isRunning: Boolean,
    updateIsRunning: (Boolean) -> Unit,
    debugInfo: @Composable () -> Unit,
) = MaterialTheme {
    Column(
        modifier = modifier.fillMaxSize().padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            debugInfo()
            Button(
                onClick = { updateIsRunning(!isRunning) },
            ) {
                Text(
                    text = if (isRunning) "Pause" else "Play"
                )
            }
        }
        AnimatedVisibility(
            visible = !isRunning,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(Res.drawable.logo),
                    contentDescription = null,
                )
                Text(
                    text = "Compose: $platformName",
                )
            }
        }
    }
}

