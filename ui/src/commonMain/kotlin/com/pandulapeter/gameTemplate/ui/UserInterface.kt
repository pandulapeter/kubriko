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
import com.pandulapeter.gameTemplate.engine.getEngine
import kotlinx.coroutines.flow.update
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun UserInterface(
    modifier: Modifier = Modifier,
    logo: DrawableResource,
    platformName: String,
) = MaterialTheme {
    Column(
        modifier = modifier.fillMaxSize().padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val isRunning = getEngine().isRunning.collectAsState()
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            DebugInfo(
                fps = getEngine().fps.collectAsState().value,
                drawnObjectCount = getEngine().drawnObjectCount.collectAsState().value,
            )
            Button(
                onClick = { getEngine().updateIsRunning(!isRunning.value) },
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
                    painter = painterResource(logo),
                    contentDescription = null,
                )
                Text(
                    text = "Compose: $platformName",
                )
            }
        }
    }
}

@Composable
private fun DebugInfo(
    fps: Float,
    drawnObjectCount: Int,
) = Text(
    text = "FPS: ${fps.toString().subSequence(0, fps.toString().indexOf('.'))}\n" +
            "Object count: $drawnObjectCount"
)