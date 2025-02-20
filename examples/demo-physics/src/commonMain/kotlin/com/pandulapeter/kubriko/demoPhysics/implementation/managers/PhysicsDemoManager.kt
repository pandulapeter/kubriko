/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoPhysics.implementation.managers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.CircleBody
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.demoPhysics.implementation.PlatformSpecificContent
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.DynamicBox
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.DynamicChain
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.DynamicCircle
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.DynamicPolygon
import com.pandulapeter.kubriko.demoPhysics.implementation.ui.ActionType
import com.pandulapeter.kubriko.extensions.cos
import com.pandulapeter.kubriko.extensions.sceneUnit
import com.pandulapeter.kubriko.extensions.sin
import com.pandulapeter.kubriko.extensions.toSceneOffset
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.physics.PhysicsManager
import com.pandulapeter.kubriko.physics.implementation.geometry.Polygon
import com.pandulapeter.kubriko.physics.implementation.math.Vec2
import com.pandulapeter.kubriko.pointerInput.PointerInputAware
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.sceneEditor.EditableMetadata
import com.pandulapeter.kubriko.serialization.SerializationManager
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import com.pandulapeter.kubriko.uiComponents.FloatingButton
import com.pandulapeter.kubriko.uiComponents.LoadingOverlay
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kubriko.examples.demo_physics.generated.resources.Res
import kubriko.examples.demo_physics.generated.resources.chain
import kubriko.examples.demo_physics.generated.resources.explosion
import kubriko.examples.demo_physics.generated.resources.ic_chain
import kubriko.examples.demo_physics.generated.resources.ic_explosion
import kubriko.examples.demo_physics.generated.resources.ic_shape
import kubriko.examples.demo_physics.generated.resources.shape
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.MissingResourceException
import org.jetbrains.compose.resources.stringResource

internal class PhysicsDemoManager(
    private val sceneJson: MutableStateFlow<String>?,
) : Manager(), PointerInputAware, Unique {

    private val _actionType = MutableStateFlow(ActionType.SHAPE)
    private val actionType = _actionType.asStateFlow()
    private val actorManager by manager<ActorManager>()
    private val stateManager by manager<StateManager>()
    private val physicsManager by manager<PhysicsManager>()
    private val serializationManager by manager<SerializationManager<EditableMetadata<*>, Editable<*>>>()
    private val viewportManager by manager<ViewportManager>()
    private val _shouldShowLoadingIndicator = MutableStateFlow(true)
    private val shouldShowLoadingIndicator = _shouldShowLoadingIndicator.asStateFlow()

    override fun onInitialize(kubriko: Kubriko) {
        stateManager.isFocused
            .onEach(stateManager::updateIsRunning)
            .launchIn(scope)
        actorManager.add(this)
        actorManager.allActors
            .filter { it.size > 1 }
            .distinctUntilChanged()
            .onEach {
                delay(100)
                _shouldShowLoadingIndicator.update { false }
            }
            .launchIn(scope)
        sceneJson?.filter { it.isNotBlank() }?.onEach(::processJson)?.launchIn(scope)
        loadMap()
    }

    @Composable
    override fun Composable(windowInsets: WindowInsets) = LoadingOverlay(
        modifier = Modifier.windowInsetsPadding(windowInsets),
        shouldShowLoadingIndicator = shouldShowLoadingIndicator.collectAsState().value,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End,
        ) {
            PlatformSpecificContent()
            Spacer(
                modifier = Modifier.weight(1f),
            )
            val selectedActionType = actionType.collectAsState()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(
                    modifier = Modifier.weight(1f),
                )
                FloatingButton(
                    icon = when (selectedActionType.value) {
                        ActionType.SHAPE -> Res.drawable.ic_shape
                        ActionType.CHAIN -> Res.drawable.ic_chain
                        ActionType.EXPLOSION -> Res.drawable.ic_explosion
                    },
                    onButtonPressed = ::changeSelectedActionType,
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

    private fun changeSelectedActionType() = _actionType.update { currentActionType ->
        val values = ActionType.entries
        val nextIndex = (currentActionType.ordinal + 1) % values.size
        values[nextIndex]
    }

    override fun onPointerReleased(screenOffset: Offset) {
        if (!shouldShowLoadingIndicator.value) {
            screenOffset.toSceneOffset(viewportManager).let { pointerSceneOffset ->
                when (actionType.value) {
                    ActionType.SHAPE -> actorManager.add(
                        when (ShapeType.entries.random()) {
                            ShapeType.BOX -> createDynamicBox(pointerSceneOffset)
                            ShapeType.CIRCLE -> createDynamicCircle(pointerSceneOffset)
                            ShapeType.POLYGON -> createDynamicPolygon(pointerSceneOffset)
                        }
                    )

                    ActionType.CHAIN -> actorManager.add(
                        DynamicChain.State(
                            linkCount = (10..20).random(),
                            initialCenterOffset = pointerSceneOffset,
                        ).restore()
                    )

                    ActionType.EXPLOSION -> Unit // TODO: Explosion
                }
            }
        }
    }

    @OptIn(ExperimentalResourceApi::class)
    private fun loadMap() = scope.launch {
        try {
            val json = Res.readBytes("files/scenes/$SCENE_NAME").decodeToString()
            sceneJson?.update { json } ?: processJson(json)
        } catch (_: MissingResourceException) {
        }
    }

    private fun processJson(json: String) {
        _shouldShowLoadingIndicator.update { true }
        actorManager.removeAll()
        actorManager.add(this)
        val deserializedActors = serializationManager.deserializeActors(json)
        actorManager.add(deserializedActors)
    }

    private fun createDynamicBox(
        pointerSceneOffset: SceneOffset,
    ) = DynamicBox.State(
        body = RectangleBody(
            initialSize = SceneSize(
                width = (60..120).random().toFloat().sceneUnit,
                height = (60..120).random().toFloat().sceneUnit,
            ),
            initialPosition = pointerSceneOffset,
        )
    ).restore()

    private fun createDynamicCircle(
        pointerSceneOffset: SceneOffset,
    ) = DynamicCircle.State(
        body = CircleBody(
            initialRadius = (30..60).random().toFloat().sceneUnit,
            initialPosition = pointerSceneOffset,
        )
    ).restore()

    private fun createDynamicPolygon(
        pointerSceneOffset: SceneOffset,
    ) = DynamicPolygon(
        initialOffset = pointerSceneOffset,
        shape = Polygon(
            vertList = (3..10).random().let { sideCount ->
                (0..sideCount).map { sideIndex ->
                    val angle = AngleRadians.TwoPi / sideCount * (sideIndex + 0.75f)
                    Vec2(
                        x = (30..120).random().sceneUnit * angle.cos,
                        y = (30..120).random().sceneUnit * angle.sin,
                    )
                }
            },
        ),
    )

    private enum class ShapeType {
        BOX, CIRCLE, POLYGON
    }

    companion object {
        const val SCENE_NAME = "scene_physics_test.json"
    }
}