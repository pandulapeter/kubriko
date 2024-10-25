package com.pandulapeter.gameTemplate

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.pandulapeter.gameTemplate.gameplay.GameplayCanvas
import com.pandulapeter.gameTemplate.gameplay.GameplayController
import com.pandulapeter.gameTemplate.ui.UserInterface
import game.app.generated.resources.Res
import game.app.generated.resources.logo

@Composable
fun App(
    modifier: Modifier = Modifier,
    exit: () -> Unit,
) {
    LaunchedEffect(Unit) {
        GameplayController.get().start()
    }
    GameplayCanvas(
        exit = exit,
    )
    UserInterface(
        modifier = modifier,
        logo = Res.drawable.logo, // TODO: Move to the `:ui` module
        platformName = getPlatform().name, // TODO: Move to the `:ui` module
    )
}