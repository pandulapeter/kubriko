package com.pandulapeter.kubriko.demoPhysics

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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.actor.body.CircleBody
import com.pandulapeter.kubriko.actor.body.PolygonBody
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.debugMenu.DebugMenu
import com.pandulapeter.kubriko.demoPhysics.implementation.ActionType
import com.pandulapeter.kubriko.demoPhysics.implementation.PhysicsDemoManager
import com.pandulapeter.kubriko.demoPhysics.implementation.PlatformSpecificContent
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.StaticBox
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.StaticCircle
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.StaticPolygon
import com.pandulapeter.kubriko.demoPhysics.implementation.sceneJson
import com.pandulapeter.kubriko.implementation.extensions.cos
import com.pandulapeter.kubriko.implementation.extensions.sceneUnit
import com.pandulapeter.kubriko.implementation.extensions.sin
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.physics.PhysicsManager
import com.pandulapeter.kubriko.pointerInput.PointerInputManager
import com.pandulapeter.kubriko.sceneEditor.EditableMetadata
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import kotlinx.serialization.json.Json
import kubriko.examples.demo_physics.generated.resources.Res
import kubriko.examples.demo_physics.generated.resources.chain
import kubriko.examples.demo_physics.generated.resources.explosion
import kubriko.examples.demo_physics.generated.resources.ic_chain
import kubriko.examples.demo_physics.generated.resources.ic_explosion
import kubriko.examples.demo_physics.generated.resources.ic_shape
import kubriko.examples.demo_physics.generated.resources.shape
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun PhysicsDemo(
    modifier: Modifier = Modifier,
    stateHolder: PhysicsDemoStateHolder = createPhysicsDemoStateHolder(),
) {
    stateHolder as PhysicsDemoStateHolderImpl
    val selectedActionType = stateHolder.physicsDemoManager.actionType.collectAsState()
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        DebugMenu(
            modifier = modifier,
            kubriko = stateHolder.kubriko,
        ) {
            KubrikoViewport(
                modifier = Modifier.fillMaxSize(),
                kubriko = stateHolder.kubriko,
            ) {
                AnimatedVisibility(
                    visible = stateHolder.physicsDemoManager.shouldShowLoadingIndicator.collectAsState().value,
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
                FloatingActionButton(
                    modifier = Modifier.padding(16.dp).size(40.dp).align(Alignment.BottomEnd),
                    containerColor = MaterialTheme.colorScheme.primary,
                    onClick = stateHolder.physicsDemoManager::changeSelectedActionType,
                ) {
                    Icon(
                        painter = painterResource(
                            when (selectedActionType.value) {
                                ActionType.SHAPE -> Res.drawable.ic_shape
                                ActionType.CHAIN -> Res.drawable.ic_chain
                                ActionType.EXPLOSION -> Res.drawable.ic_explosion
                            }
                        ),
                        contentDescription = stringResource(
                            when (selectedActionType.value) {
                                ActionType.SHAPE -> Res.string.shape
                                ActionType.CHAIN -> Res.string.chain
                                ActionType.EXPLOSION -> Res.string.explosion
                            }
                        ),
                    )
                }
            }
        }
    }
}

sealed interface PhysicsDemoStateHolder

fun createPhysicsDemoStateHolder(): PhysicsDemoStateHolder = PhysicsDemoStateHolderImpl()

internal class PhysicsDemoStateHolderImpl : PhysicsDemoStateHolder {
    private val json = Json { ignoreUnknownKeys = true }
    val serializationManager = EditableMetadata.newSerializationManagerInstance(
        EditableMetadata(
            typeId = "staticBox",
            deserializeState = { serializedState -> json.decodeFromString<StaticBox.State>(serializedState) },
            instantiate = { StaticBox.State(body = RectangleBody(initialPosition = it, initialSize = SceneSize(100.sceneUnit, 100.sceneUnit))) },
        ),
        EditableMetadata(
            typeId = "staticCircle",
            deserializeState = { serializedState -> json.decodeFromString<StaticCircle.State>(serializedState) },
            instantiate = { StaticCircle.State(body = CircleBody(initialPosition = it, initialRadius = 100.sceneUnit)) },
        ),
        EditableMetadata(
            typeId = "staticPolygon",
            deserializeState = { serializedState -> json.decodeFromString<StaticPolygon.State>(serializedState) },
            instantiate = {
                StaticPolygon.State(
                    body = PolygonBody(
                        initialPosition = it,
                        vertices = (3..10).random().let { sideCount ->
                            (0..sideCount).map { sideIndex ->
                                val angle = AngleRadians.TwoPi / sideCount * (sideIndex + 0.75f)
                                SceneOffset(
                                    x = (30..120).random().sceneUnit * angle.cos,
                                    y = (30..120).random().sceneUnit * angle.sin,
                                )
                            }
                        },
                    )
                )
            },
        ),
    )
    val physicsDemoManager by lazy { PhysicsDemoManager(sceneJson = sceneJson) }
    val kubriko by lazy {
        Kubriko.newInstance(
            ViewportManager.newInstance(aspectRatioMode = ViewportManager.AspectRatioMode.FitVertical(defaultHeight = 1920.sceneUnit)),
            PhysicsManager.newInstance(),
            PointerInputManager.newInstance(),
            physicsDemoManager,
            serializationManager,
        )
    }
}