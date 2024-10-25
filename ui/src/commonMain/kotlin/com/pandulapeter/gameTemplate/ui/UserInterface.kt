package com.pandulapeter.gameTemplate.ui

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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.gameTemplate.gameplay.GameplayController
import com.pandulapeter.gameTemplate.gameplay.models.Metadata
import game.ui.generated.resources.Res
import game.ui.generated.resources.logo
import org.jetbrains.compose.resources.painterResource

@Composable
fun UserInterface(
    modifier: Modifier = Modifier,
) = MaterialTheme {
    Column(
        modifier = modifier.fillMaxSize().padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val isRunning = GameplayController.get().isRunning.collectAsState()
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            DebugInfo(
                metadata = GameplayController.get().metadata.collectAsState().value,
            )
            Button(
                onClick = { GameplayController.get().updateIsRunning(!isRunning.value) },
            ) {
                Text(
                    text = if (isRunning.value) "Pause" else "Resume"
                )
            }
        }
        AnimatedVisibility(
            visible = !isRunning.value,
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
                    text = "Compose: ${getPlatform().name}",
                )
            }
        }
    }
}

@Composable
private fun DebugInfo(metadata: Metadata) = Text(
    text = "FPS: ${metadata.fps.toString().subSequence(0, metadata.fps.toString().indexOf('.'))}\n" +
            "Total object count: ${metadata.totalGameObjectCount}\n" +
            "Visible object count: ${metadata.visibleGameObjectCount}"
)