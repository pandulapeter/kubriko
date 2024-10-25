package com.pandulapeter.gameTemplate.editor.implementation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun EditorUserInterface(
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier,
) {
    GameObjectCounter(
        gameObjectCount = EditorController.totalGameObjectCount.collectAsState().value,
    )
}

@Composable
private fun GameObjectCounter(
    gameObjectCount: Int
) = Text(
    modifier = Modifier.padding(16.dp),
    text = "GameObjects: $gameObjectCount"
)