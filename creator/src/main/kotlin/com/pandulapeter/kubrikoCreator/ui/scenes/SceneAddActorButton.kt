/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.scenes

import androidx.compose.runtime.Composable
import com.pandulapeter.kubrikoCreator.actors.codegen.SceneSettingsCodeGenerator
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import com.pandulapeter.kubrikoCreator.ui.components.SearchableChoiceButton
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_add_actor_to_scene
import kubriko.creator.generated.resources.ic_add_actor
import kubriko.creator.generated.resources.scene_add_actor_none
import org.jetbrains.compose.resources.stringResource

/**
 * Places an Actor of one of [actorTypeNames], chosen from a list that filters once it is long, in the middle of what the
 * Scene shows ([onPlace]).
 */
@Composable
internal fun SceneAddActorButton(
    actorTypeNames: List<String>,
    onPlace: (actorTypeName: String) -> Unit,
) = SearchableChoiceButton(
    label = stringResource(Res.string.action_add_actor_to_scene),
    options = actorTypeNames,
    optionText = { it },
    onSelected = onPlace,
    hint = if (actorTypeNames.isEmpty()) stringResource(Res.string.scene_add_actor_none) else null,
    face = { onClick -> IconActionButton(icon = Res.drawable.ic_add_actor, label = Res.string.action_add_actor_to_scene, onClick = onClick) },
)

/**
 * The Actor types an Actor can be placed of: the block-built ones among [blockBuiltActorTypes], then those written by
 * hand, which the project's classes register ([registeredTypeIds]) besides the block-built Actor types and Manager types
 * ([managerTypes]) and the Scene settings, each group in alphabetical order.
 */
internal fun placeableActorTypesOf(registeredTypeIds: Set<String>, blockBuiltActorTypes: List<String>, managerTypes: Collection<String>): List<String> {
    val blockBuilt = blockBuiltActorTypes.toSet()
    val handWritten = registeredTypeIds.filter { it !in blockBuilt && it !in managerTypes && it != SceneSettingsCodeGenerator.SETTINGS_NAME }
    return blockBuiltActorTypes.sorted() + handWritten.sorted()
}
