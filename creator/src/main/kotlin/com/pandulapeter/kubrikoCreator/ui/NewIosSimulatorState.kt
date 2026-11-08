/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.pandulapeter.kubrikoCreator.ui.components.FieldError
import com.pandulapeter.kubrikoCreator.workspace.IosDeviceType
import com.pandulapeter.kubrikoCreator.workspace.IosRuntime
import com.pandulapeter.kubrikoCreator.workspace.IosSimulatorCreation
import com.pandulapeter.kubrikoCreator.workspace.IosSimulatorOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.new_ios_simulator_name_taken

/**
 * What the dialog making a new iOS simulator shows: what Xcode offers, read as it opens and again on [reload], and the
 * simulator being put together from it, made by [create] in [scope].
 *
 * @param read Lists what a simulator can be made of.
 * @param create Makes the simulator, which Play then uses.
 * @param onCreated Closes the dialog once the simulator is made.
 */
internal class NewIosSimulatorState(
    private val read: suspend () -> IosSimulatorOptions,
    private val create: suspend (name: String, deviceType: IosDeviceType, runtime: IosRuntime) -> IosSimulatorCreation,
    private val scope: CoroutineScope,
    private val onCreated: () -> Unit,
) {
    /**
     * What Xcode offers, or null while it is being read.
     */
    var options: IosSimulatorOptions? by mutableStateOf(null)
        private set
    var deviceType: IosDeviceType? by mutableStateOf(null)
        private set
    var runtime: IosRuntime? by mutableStateOf(null)
        private set
    var name by mutableStateOf("")
        private set

    /**
     * Why [name] can't be used, once Create was pressed with it, until it changes.
     */
    var nameError: FieldError? by mutableStateOf(null)
        private set

    var isCreating by mutableStateOf(false)
        private set

    /**
     * Why the last try to make the simulator failed, until the next one.
     */
    var failure: IosSimulatorCreation? by mutableStateOf(null)
        private set

    /**
     * The iOS versions [deviceType] runs, newest first.
     */
    val runtimes: List<IosRuntime> get() = deviceType?.let { (options as? IosSimulatorOptions.Ready)?.runtimesFor(it) }.orEmpty()

    /**
     * Whether Create can be pressed: a name is typed, and nothing is being made.
     */
    val canCreate: Boolean get() = options is IosSimulatorOptions.Ready && name.isNotBlank() && deviceType != null && runtime != null && !isCreating

    private var isNameTyped = false
    private var reading: Job? = null

    /**
     * Reads what Xcode offers again, starting the simulator afresh from it, as a Mac that just got an iOS version needs.
     */
    fun reload() {
        reading?.cancel()
        options = null
        deviceType = null
        runtime = null
        reading = scope.launch {
            val found = try {
                read()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                IosSimulatorOptions.Unreadable(exception.message ?: exception.toString())
            }
            options = found
            (found as? IosSimulatorOptions.Ready)?.suggestedDeviceType?.let(::chooseDeviceType)
        }
    }

    /**
     * Makes the simulator a [type], keeping its iOS version if [type] runs it too, and its name if one was typed.
     */
    fun chooseDeviceType(type: IosDeviceType) {
        val ready = options as? IosSimulatorOptions.Ready ?: return
        deviceType = type
        runtime = runtime?.takeIf { it.runs(type) } ?: ready.runtimesFor(type).firstOrNull()
        if (!isNameTyped) name = ready.suggestedName(type)
    }

    fun chooseRuntime(runtime: IosRuntime) {
        this.runtime = runtime
    }

    /**
     * Names the simulator [text], with the line breaks, tabs, and other control characters a paste may bring as spaces.
     */
    fun changeName(text: String) {
        name = text.map { if (it.isISOControl()) ' ' else it }.joinToString("")
        isNameTyped = true
        nameError = null
    }

    /**
     * Makes the simulator, unless its name is taken, which the name field then says, or one is already being made.
     */
    fun create() {
        val ready = options as? IosSimulatorOptions.Ready ?: return
        val type = deviceType ?: return
        val runtime = runtime ?: return
        if (!canCreate) return
        if (ready.isNameTaken(name)) {
            nameError = FieldError(Res.string.new_ios_simulator_name_taken, listOf(name.trim()))
            return
        }
        val requested = name
        isCreating = true
        failure = null
        scope.launch {
            try {
                failure = made(requested, type, runtime)
                if (failure == null) onCreated() else refreshTakenNames()
            } finally {
                isCreating = false
            }
        }
    }

    /**
     * Makes the simulator, returning how it failed, or null once it is made.
     */
    private suspend fun made(name: String, type: IosDeviceType, runtime: IosRuntime) = try {
        create(name, type, runtime).takeUnless { it is IosSimulatorCreation.Created }
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        IosSimulatorCreation.Refused(exception.message ?: exception.toString())
    }

    /**
     * Reads the simulators' names again after a try that failed, which may still have made one (`simctl` stopped for taking
     * too long, for one), so another try under the same name is refused rather than making a second.
     */
    private suspend fun refreshTakenNames() {
        val found = try {
            read()
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            return
        }
        val current = options as? IosSimulatorOptions.Ready ?: return
        if (found is IosSimulatorOptions.Ready) options = current.copy(takenNames = found.takenNames)
    }
}

/**
 * The state of the dialog making a new iOS simulator, reading what Xcode offers as it first shows.
 */
@Composable
internal fun rememberNewIosSimulatorState(
    read: suspend () -> IosSimulatorOptions,
    create: suspend (name: String, deviceType: IosDeviceType, runtime: IosRuntime) -> IosSimulatorCreation,
    onCreated: () -> Unit,
): NewIosSimulatorState {
    val scope = rememberCoroutineScope()
    val currentOnCreated by rememberUpdatedState(onCreated)
    val state = remember { NewIosSimulatorState(read, create, scope) { currentOnCreated() } }
    LaunchedEffect(state) { state.reload() }
    return state
}
