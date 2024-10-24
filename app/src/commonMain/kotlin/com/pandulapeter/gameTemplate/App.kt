package com.pandulapeter.gameTemplate

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pandulapeter.gameTemplate.gameplay.GameplayCanvas
import com.pandulapeter.gameTemplate.ui.UserInterface
import game.app.generated.resources.Res
import game.app.generated.resources.logo

@Composable
fun App(
    modifier: Modifier = Modifier,
) {
    GameplayCanvas()
    UserInterface(
        modifier = modifier,
        logo = Res.drawable.logo,
        platformGreeting = Greeting().greet(),
    )
}