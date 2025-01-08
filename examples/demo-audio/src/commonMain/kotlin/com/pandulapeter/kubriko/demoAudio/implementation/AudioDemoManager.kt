package com.pandulapeter.kubriko.demoAudio.implementation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.manager.Manager
import kubriko.examples.demo_audio.generated.resources.Res
import kubriko.examples.demo_audio.generated.resources.ic_pause
import kubriko.examples.demo_audio.generated.resources.ic_play
import kubriko.examples.demo_audio.generated.resources.ic_stop
import kubriko.examples.demo_audio.generated.resources.pause
import kubriko.examples.demo_audio.generated.resources.play
import kubriko.examples.demo_audio.generated.resources.stop
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

internal class AudioDemoManager : Manager() {

    @Composable
    override fun Composable(insetPaddingModifier: Modifier) = Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SongControls(
                title = "Music 01",
            )
            SongControls(
                title = "Music 02",
            )
        }
    }

    @Composable
    private fun SongControls(
        title: String,
    ) = Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_play),
                contentDescription = stringResource(Res.string.play),
            )
            Image(
                painter = painterResource(Res.drawable.ic_pause),
                contentDescription = stringResource(Res.string.pause),
            )
            Image(
                painter = painterResource(Res.drawable.ic_stop),
                contentDescription = stringResource(Res.string.stop),
            )
        }
    }
}