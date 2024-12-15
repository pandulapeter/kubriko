package com.pandulapeter.kubriko.demoPerformance

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.actor.body.PointBody
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.debugMenu.DebugMenu
import com.pandulapeter.kubriko.demoPerformance.implementation.PerformanceDemoManager
import com.pandulapeter.kubriko.demoPerformance.implementation.PlatformSpecificContent
import com.pandulapeter.kubriko.demoPerformance.implementation.actors.BoxWithCircle
import com.pandulapeter.kubriko.demoPerformance.implementation.actors.Character
import com.pandulapeter.kubriko.demoPerformance.implementation.actors.MovingBox
import com.pandulapeter.kubriko.demoPerformance.implementation.sceneJson
import com.pandulapeter.kubriko.implementation.extensions.sceneUnit
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.sceneEditor.EditableMetadata
import com.pandulapeter.kubriko.shared.ExampleStateHolder
import com.pandulapeter.kubriko.types.SceneSize
import kotlinx.serialization.json.Json

@Composable
fun PerformanceDemo(
    modifier: Modifier = Modifier,
    stateHolder: PerformanceDemoStateHolder = createPerformanceDemoStateHolder(),
) {
    stateHolder as PerformanceDemoStateHolderImpl
    DebugMenu(
        contentModifier = modifier,
        kubriko = stateHolder.kubriko,
    ) {
        KubrikoViewport(
            kubriko = stateHolder.kubriko,
        ) {
            AnimatedVisibility(
                visible = stateHolder.performanceDemoManager.shouldShowLoadingIndicator.collectAsState().value,
                enter = fadeIn(animationSpec = tween(durationMillis = 0)),
                exit = fadeOut(animationSpec = tween(durationMillis = 1000)),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp).align(Alignment.BottomStart),
                        strokeWidth = 3.dp,
                    )
                }
            }
            PlatformSpecificContent()
        }
    }
}

sealed interface PerformanceDemoStateHolder : ExampleStateHolder

fun createPerformanceDemoStateHolder(): PerformanceDemoStateHolder = PerformanceDemoStateHolderImpl()

internal class PerformanceDemoStateHolderImpl : PerformanceDemoStateHolder {
    private val json = Json { ignoreUnknownKeys = true }
    val serializationManager = EditableMetadata.newSerializationManagerInstance(
        EditableMetadata(
            typeId = "character",
            deserializeState = { serializedState -> json.decodeFromString<Character.State>(serializedState) },
            instantiate = { Character.State(body = PointBody(initialPosition = it)) },
        ),
        EditableMetadata(
            typeId = "boxWithCircle",
            deserializeState = { serializedState -> json.decodeFromString<BoxWithCircle.State>(serializedState) },
            instantiate = { BoxWithCircle.State(body = RectangleBody(initialPosition = it, initialSize = SceneSize(100.sceneUnit, 100.sceneUnit))) },
        ),
        EditableMetadata(
            typeId = "movingBox",
            deserializeState = { serializedState -> json.decodeFromString<MovingBox.State>(serializedState) },
            instantiate = { MovingBox.State(body = RectangleBody(initialPosition = it, initialSize = SceneSize(100.sceneUnit, 100.sceneUnit))) }
        ),
    )
    val performanceDemoManager by lazy { PerformanceDemoManager(sceneJson = sceneJson) }
    val kubriko by lazy {
        Kubriko.newInstance(
            ViewportManager.newInstance(
                initialScaleFactor = 0.5f,
                viewportEdgeBuffer = 200.sceneUnit,
            ),
            performanceDemoManager,
            serializationManager,
        )
    }

    override fun dispose() = kubriko.dispose()
}