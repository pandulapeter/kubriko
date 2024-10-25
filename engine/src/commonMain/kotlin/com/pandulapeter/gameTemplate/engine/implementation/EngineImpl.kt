package com.pandulapeter.gameTemplate.engine.implementation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.pandulapeter.gameTemplate.engine.Engine
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Dynamic
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Visible
import com.pandulapeter.gameTemplate.engine.implementation.extensions.isVisible
import com.pandulapeter.gameTemplate.engine.managers.GameObjectManager
import com.pandulapeter.gameTemplate.engine.managers.MetadataManager
import com.pandulapeter.gameTemplate.engine.managers.StateManager
import com.pandulapeter.gameTemplate.engine.managers.ViewportManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlin.math.max
import kotlin.math.min

// TODO: Break up into smaller classes
internal object EngineImpl : Engine,
    ViewportManager,
    StateManager,
    GameObjectManager,
    MetadataManager,
    CoroutineScope {
    override val viewportManager = this
    override val stateManager = this
    override val gameObjectManager = this
    override val metadataManager = this

    override val coroutineContext = SupervisorJob() + Dispatchers.Default

    // Viewport management
    private val _size = MutableStateFlow(Size.Zero)
    override val size = _size.asStateFlow()
    private val _offset = MutableStateFlow(Offset.Zero)
    override val offset = _offset.asStateFlow()
    private val _scaleFactor = MutableStateFlow(1f)
    override val scaleFactor = _scaleFactor.asStateFlow()

    private const val SCALE_MIN = 0.2f
    private const val SCALE_MAX = 10f

    override fun addToOffset(
        offset: Offset,
    ) = _offset.update { currentValue -> currentValue + (offset / _scaleFactor.value) }

    override fun multiplyScaleFactor(
        scaleFactor: Float
    ) = _scaleFactor.update { currentValue -> max(SCALE_MIN, min(currentValue * scaleFactor, SCALE_MAX)) }

    fun updateSize(size: Size) = _size.update { size }

    // State management
    private val _isFocused = MutableStateFlow(false)
    override val isFocused = _isFocused.asStateFlow()
    private val _isRunning = MutableStateFlow(false)
    override val isRunning = combine(
        isFocused,
        _isRunning
    ) { isFocused, isRunning ->
        isFocused && isRunning
    }.stateIn(this, SharingStarted.Eagerly, false)

    fun updateFocus(
        isFocused: Boolean,
    ) = _isFocused.update { isFocused }

    override fun updateIsRunning(
        isRunning: Boolean,
    ) = _isRunning.update { isRunning }

    // GameObject management
    private val gameObjects = MutableStateFlow(emptySet<GameObject>())
    internal val dynamicGameObjects = gameObjects.map { it.filterIsInstance<Dynamic>() }.stateIn(this, SharingStarted.Eagerly, emptyList())
    internal val visibleGameObjectsInViewport = combine(
        gameObjects.map { it.filterIsInstance<Visible>() }.stateIn(this, SharingStarted.Eagerly, emptyList()),
        size,
        offset,
        scaleFactor,
    ) { allVisibleGameObjects, viewportSize, viewportOffset, viewportScaleFactor ->
        (viewportSize / viewportScaleFactor).let { scaledViewportSize ->
            allVisibleGameObjects.filter {
                it.isVisible(
                    scaledViewportSize = scaledViewportSize,
                    viewportOffset = viewportOffset,
                    viewportScaleFactor = viewportScaleFactor,
                )
            }
        }
    }.stateIn(this, SharingStarted.Eagerly, emptyList())

    override fun register(gameObject: GameObject) = gameObjects.update { currentValue ->
        currentValue + gameObject
    }

    override fun register(gameObjects: Collection<GameObject>) = this.gameObjects.update { currentValue ->
        currentValue + gameObjects
    }

    override fun remove(gameObject: GameObject) = gameObjects.update { currentValue ->
        currentValue.filterNot { it == gameObject }.toSet()
    }

    override fun remove(gameObjects: Collection<GameObject>) = this.gameObjects.update { currentValue ->
        currentValue.filterNot { it in gameObjects }.toSet()
    }

    override fun removeAll() = gameObjects.update { emptySet() }

    // Metadata management
    private val _fps = MutableStateFlow(0f)
    override val fps = _fps.asStateFlow()
    override val visibleGameObjectCount = visibleGameObjectsInViewport.map { it.count() }.stateIn(this, SharingStarted.Eagerly, 0)
    override val totalGameObjectCount = gameObjects.map { it.count() }.stateIn(this, SharingStarted.Eagerly, 0)
    private var lastFpsUpdateTimestamp = 0L

    fun updateFps(
        gameTimeNanos: Long,
        deltaTimeMillis: Float,
    ) {
        if (gameTimeNanos - lastFpsUpdateTimestamp >= 1000000000L) {
            _fps.update { currentValue ->
                if (deltaTimeMillis == 0f) currentValue else 1000f / deltaTimeMillis
            }
            lastFpsUpdateTimestamp = gameTimeNanos
        }
    }
}