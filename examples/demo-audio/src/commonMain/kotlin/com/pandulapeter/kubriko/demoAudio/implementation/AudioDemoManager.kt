package com.pandulapeter.kubriko.demoAudio.implementation

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.audioPlayback.MusicManager
import com.pandulapeter.kubriko.manager.Manager
import kubriko.examples.demo_audio.generated.resources.Res
import kubriko.examples.demo_audio.generated.resources.ic_loop_on
import kubriko.examples.demo_audio.generated.resources.ic_pause
import kubriko.examples.demo_audio.generated.resources.ic_play
import kubriko.examples.demo_audio.generated.resources.ic_stop
import kubriko.examples.demo_audio.generated.resources.loop_on
import kubriko.examples.demo_audio.generated.resources.pause
import kubriko.examples.demo_audio.generated.resources.play
import kubriko.examples.demo_audio.generated.resources.stop
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalResourceApi::class)
internal class AudioDemoManager : Manager() {

    private val musicManager by manager<MusicManager>()
    private var isTrack1Playing = mutableStateOf(false)
    private var isTrack2Playing = mutableStateOf(false)
    private val track1Uri = Res.getUri(URI_MUSIC_1)
    private val track2Uri = Res.getUri(URI_MUSIC_2)

    override fun onInitialize(kubriko: Kubriko) {
        musicManager.preload(track1Uri, track2Uri)
    }

    override fun onUpdate(deltaTimeInMilliseconds: Float, gameTimeMilliseconds: Long) {
        isTrack1Playing.value = musicManager.isPlaying(track1Uri)
        isTrack2Playing.value = musicManager.isPlaying(track2Uri)
    }

    @Composable
    override fun Composable(insetPaddingModifier: Modifier) = Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MusicControls(
                title = "A Csajod - Átkok",
                musicUri = track1Uri,
                isPlaying = isTrack1Playing.value,
            )
            MusicControls(
                title = "A Csajod - Energiavámpír",
                musicUri = track2Uri,
                isPlaying = isTrack2Playing.value,
            )
        }
    }

    @Composable
    private fun MusicControls(
        title: String,
        musicUri: String,
        isPlaying: Boolean,
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
            ControlButton(
                icon = if (isPlaying) Res.drawable.ic_pause else Res.drawable.ic_play,
                contentDescription = if (isPlaying) Res.string.pause else Res.string.play,
                onClick = { if (isPlaying) musicManager.pause(musicUri) else musicManager.play(musicUri) },
            )
            ControlButton(
                icon = Res.drawable.ic_stop,
                contentDescription = Res.string.stop,
                isEnabled = isPlaying,
                onClick = { musicManager.stop(musicUri) },
            )
            ControlButton(
                icon = Res.drawable.ic_loop_on,
                contentDescription = Res.string.loop_on,
                isEnabled = false,
                onClick = {},
            )
        }
    }

    @Composable
    private fun ControlButton(
        icon: DrawableResource,
        contentDescription: StringResource,
        isEnabled: Boolean = true,
        onClick: () -> Unit,
    ) = Image(
        modifier = Modifier
            .clickable(enabled = isEnabled, onClick = onClick)
            .padding(8.dp)
            .alpha(if (isEnabled) 1f else 0.5f),
        colorFilter = ColorFilter.tint(LocalContentColor.current),
        painter = painterResource(icon),
        contentDescription = stringResource(contentDescription),
    )

    companion object {
        private const val URI_MUSIC_1 = "files/music/a_csajod-átkok.mp3"
        private const val URI_MUSIC_2 = "files/music/a_csajod-energiavámpír.mp3"
    }
}