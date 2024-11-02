package com.pandulapeter.kubriko.editor.implementation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.editor.implementation.helpers.exitApp
import com.pandulapeter.kubriko.editor.implementation.helpers.handleKeyReleased
import com.pandulapeter.kubriko.editor.implementation.helpers.handleKeys
import com.pandulapeter.kubriko.editor.implementation.helpers.loadFile
import com.pandulapeter.kubriko.editor.implementation.helpers.saveFile
import com.pandulapeter.kubriko.engine.Kubriko
import com.pandulapeter.kubriko.engine.implementation.extensions.toSceneOffset
import com.pandulapeter.kubriko.engine.traits.Editable
import com.pandulapeter.kubriko.engine.traits.Overlay
import com.pandulapeter.kubriko.engine.traits.Unique
import com.pandulapeter.kubriko.engine.traits.Visible
import com.pandulapeter.kubriko.engine.types.SceneOffset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class EditorController(val kubriko: Kubriko) : CoroutineScope, Overlay, Unique {

    override val coroutineContext = SupervisorJob() + Dispatchers.Default
    val allInstances = kubriko.actorManager.allActors
        .map { it.filterIsInstance<Editable<*>>() }
        .stateIn(this, SharingStarted.Eagerly, emptyList())
    val visibleInstancesWithinViewport = kubriko.actorManager.visibleActorsWithinViewport
        .map { it.filterIsInstance<Editable<*>>() }
        .stateIn(this, SharingStarted.Eagerly, emptyList())
    val totalActorCount = kubriko.actorManager.allActors
        .map { it.filterIsInstance<Editable<*>>().count() }
        .stateIn(this, SharingStarted.Eagerly, 0)
    private val mouseScreenCoordinates = MutableStateFlow(Offset.Zero)
    val mouseSceneOffset = combine(
        mouseScreenCoordinates,
        kubriko.viewportManager.center,
        kubriko.viewportManager.size,
        kubriko.viewportManager.scaleFactor,
    ) { mouseScreenCoordinates, viewportCenter, viewportSize, viewportScaleFactor ->
        mouseScreenCoordinates.toSceneOffset(
            viewportCenter = viewportCenter,
            viewportSize = viewportSize,
            viewportScaleFactor = viewportScaleFactor,
        )
    }.stateIn(this, SharingStarted.Eagerly, SceneOffset.Zero)
    private val instanceUpdateTrigger = MutableStateFlow(false)
    private val _selectedInstance = MutableStateFlow<Editable<*>?>(null)
    val selectedUpdatableInstance = combine(
        _selectedInstance,
        instanceUpdateTrigger,
    ) { selectedGameObject, triggerGameObjectUpdate ->
        selectedGameObject to triggerGameObjectUpdate
    }.stateIn(this, SharingStarted.Eagerly, null to false)
    private val _selectedTypeId = MutableStateFlow<String?>(null)
    val selectedTypeId = _selectedTypeId.asStateFlow()
    private val _currentFileName = MutableStateFlow(DEFAULT_MAP_FILE_NAME)
    val currentFileName = _currentFileName.asStateFlow()
    private val _shouldShowVisibleOnly = MutableStateFlow(false)
    val shouldShowVisibleOnly = _shouldShowVisibleOnly.asStateFlow()

    init {
        kubriko.isEditor = true
        kubriko.inputManager.activeKeys
            .filter { it.isNotEmpty() }
            .onEach(kubriko.viewportManager::handleKeys)
            .launchIn(this)
        kubriko.inputManager.onKeyReleased
            .onEach { key ->
                handleKeyReleased(
                    key = key,
                    onNavigateBackRequested = ::navigateBack,
                )
            }
            .launchIn(this)
    }

    fun onShouldShowVisibleOnlyToggled() = _shouldShowVisibleOnly.update { currentValue ->
        !currentValue
    }

    fun getSelectedInstance() = selectedUpdatableInstance.value.first

    fun getMouseWorldCoordinates() = mouseSceneOffset.value

    fun onLeftClick(screenCoordinates: Offset) {
        val positionInWorld = screenCoordinates.toSceneOffset(kubriko.viewportManager)
        findInstanceOnPosition(positionInWorld).let { gameObjectAtPosition ->
            selectedUpdatableInstance.value.first.let { currentSelectedGameObject ->
                if (gameObjectAtPosition == null) {
                    if (currentSelectedGameObject == null) {
                        launch {
                            val typeId = selectedTypeId.value
                            kubriko.serializationManager.deserializeActors(
                                serializedStates = "[{\"typeId\":\"$typeId\",\"state\":\"{\\\"position\\\":{\\\"x\\\":${positionInWorld.x.raw},\\\"y\\\":${positionInWorld.y.raw}}}\"}]"
                            ).firstOrNull()?.let { actor ->
                                kubriko.actorManager.add(actor)
                            }
                        }
                    } else {
                        deselectSelectedInstance()
                    }
                } else {
                    (gameObjectAtPosition as? Editable<*>)?.let { availableInEditor ->
                        selectInstance(gameObjectAtPosition)
                    }
                }
            }
        }
    }

    fun onRightClick(screenCoordinates: Offset) {
        findInstanceOnPosition(screenCoordinates.toSceneOffset(kubriko.viewportManager)).let { actorAtPosition ->
            if (actorAtPosition != null) {
                if (actorAtPosition == _selectedInstance.value) {
                    deleteSelectedInstance()
                } else {
                    kubriko.actorManager.remove(actorAtPosition)
                }
            }
        }
    }

    private fun findInstanceOnPosition(positionInWorld: SceneOffset) = kubriko.actorManager
        .findVisibleInstancesWithBoundsInPosition(positionInWorld)
        .minByOrNull { (it as? Visible)?.drawingOrder ?: 0f }

    fun selectInstance(gameObject: Editable<*>) {
        val currentSelectedGameObject = selectedUpdatableInstance.value.first
        // TODO currentSelectedGameObject?.isSelectedInEditor = false
        _selectedInstance.update {
            if (currentSelectedGameObject == gameObject) {
                null
            } else {
                gameObject// TODO .also { it.isSelectedInEditor = true }
            }
        }
    }

    fun deleteSelectedInstance() {
        _selectedInstance.value?.let { selectedGameObject ->
            _selectedInstance.update { null }
            kubriko.actorManager.remove(selectedGameObject)
        }
    }

    fun onMouseMove(screenCoordinates: Offset) = mouseScreenCoordinates.update { screenCoordinates }

    fun locateSelectedInstance() {
        (_selectedInstance.value as? Visible)?.let { visibleTrait ->
            kubriko.viewportManager.setCenter(visibleTrait.position)
        }
    }

    fun notifySelectedInstanceUpdate() = instanceUpdateTrigger.update { !it }

    fun selectInstance(typeId: String) = _selectedTypeId.update { typeId }

    fun deselectSelectedInstance() {
        // TODO _selectedInstance.value?.isSelectedInEditor = false
        _selectedInstance.update { null }
    }

    fun reset() {
        kubriko.viewportManager.setCenter(SceneOffset.Zero)
        _currentFileName.update { DEFAULT_MAP_FILE_NAME }
        _selectedInstance.update { null }
        kubriko.actorManager.removeAll()
        kubriko.actorManager.add(this)
    }

    fun loadMap(path: String) {
        launch {
            loadFile(path)?.let { json ->
                kubriko.actorManager.deserializeState(json)
                kubriko.actorManager.add(this@EditorController)
                _currentFileName.update { path.split('/').last() }
            }
        }
    }

    fun saveMap(path: String) {
        launch {
            saveFile(
                path = path,
                content = kubriko.actorManager.serializeState(),
            )
        }
    }

    private fun navigateBack() {
        if (selectedUpdatableInstance.value.first == null) {
            exitApp()
        } else {
            deselectSelectedInstance()
        }
    }

    override fun drawToViewport(scope: DrawScope) {
        kubriko.viewportManager.size.value.let { viewportSize ->
            kubriko.viewportManager.center.value.let { viewportCenter ->
                kubriko.viewportManager.scaleFactor.value.let { viewportScaleFactor ->
                    // Calculate the viewport boundaries in world coordinates
                    val viewportTopLeft = Offset.Zero.toSceneOffset(
                        viewportCenter = viewportCenter,
                        viewportSize = viewportSize,
                        viewportScaleFactor = viewportScaleFactor,
                    )
                    val viewportBottomRight = Offset(viewportSize.width, viewportSize.height).toSceneOffset(
                        viewportCenter = viewportCenter,
                        viewportSize = viewportSize,
                        viewportScaleFactor = viewportScaleFactor,
                    )

                    // Precomputed values for major and minor lines
                    val strokeWidth = 1f / viewportScaleFactor

                    // Calculate the starting point for vertical lines and ensure alignment with (0,0)
                    var startX = (viewportTopLeft.x / GRID_CELL_SIZE).raw.toInt() * GRID_CELL_SIZE
                    if (startX > viewportTopLeft.x.raw) startX -= GRID_CELL_SIZE
                    val startXLineIndex = (startX / GRID_CELL_SIZE).toInt() // Adjust to always align with origin

                    // Draw vertical grid lines
                    var currentX = startX
                    var iterationX = 0
                    while (currentX <= viewportBottomRight.x.raw) {
                        val alpha = if ((startXLineIndex + iterationX) % 10 == 0) ALPHA_MAJOR else ALPHA_MINOR
                        scope.drawLine(
                            color = Color.Gray.copy(alpha = alpha),
                            start = Offset(currentX, viewportTopLeft.y.raw),
                            end = Offset(currentX, viewportBottomRight.y.raw),
                            strokeWidth = strokeWidth
                        )
                        currentX += GRID_CELL_SIZE
                        iterationX++
                    }

                    // Calculate the starting point for horizontal lines, aligning with (0,0)
                    var startY = (viewportTopLeft.y / GRID_CELL_SIZE).raw.toInt() * GRID_CELL_SIZE
                    if (startY > viewportTopLeft.y.raw) startY -= GRID_CELL_SIZE
                    val startYLineIndex = (startY / GRID_CELL_SIZE).toInt() // Align with origin

                    // Draw horizontal grid lines
                    var currentY = startY
                    var iterationY = 0
                    while (currentY <= viewportBottomRight.y.raw) {
                        val alpha = if ((startYLineIndex + iterationY) % 10 == 0) ALPHA_MAJOR else ALPHA_MINOR
                        scope.drawLine(
                            color = Color.Gray.copy(alpha = alpha),
                            start = Offset(viewportTopLeft.x.raw, currentY),
                            end = Offset(viewportBottomRight.x.raw, currentY),
                            strokeWidth = strokeWidth
                        )
                        currentY += GRID_CELL_SIZE
                        iterationY++
                    }
                }
            }
        }
    }

    companion object {
        const val MAPS_DIRECTORY = "./src/commonMain/composeResources/files/maps"
        private const val DEFAULT_MAP_FILE_NAME = "map_untitled.json"
        private const val GRID_CELL_SIZE = 100f
        private const val ALPHA_MAJOR = 0.4f
        private const val ALPHA_MINOR = 0.2f
    }
}