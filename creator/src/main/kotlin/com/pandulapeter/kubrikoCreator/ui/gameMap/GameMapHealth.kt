/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.gameMap

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.context.HealthFinding
import com.pandulapeter.kubrikoCreator.context.KotlinBlockScopeChecks
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.block_property_of_manager
import kubriko.creator.generated.resources.game_map_worth_a_look
import kubriko.creator.generated.resources.health_collision_with_absent_type
import kubriko.creator.generated.resources.health_kotlin_block_removed_local
import kubriko.creator.generated.resources.health_kotlin_block_return
import kubriko.creator.generated.resources.health_mention_missing
import kubriko.creator.generated.resources.health_page_image_missing
import kubriko.creator.generated.resources.health_page_no_summary
import kubriko.creator.generated.resources.health_page_no_title
import kubriko.creator.generated.resources.health_planned_may_be_built
import kubriko.creator.generated.resources.health_reference_missing
import kubriko.creator.generated.resources.health_timer_never_started
import kubriko.creator.generated.resources.health_unmentioned
import kubriko.creator.generated.resources.health_unreached_scene
import kubriko.creator.generated.resources.health_unread_property
import kubriko.creator.generated.resources.health_unused_actor_type
import kubriko.creator.generated.resources.health_unused_page_image
import org.jetbrains.compose.resources.stringResource

/**
 * What about the game is worth a look, at the top of the map, each opening what it is about on a click.
 */
internal fun LazyListScope.healthSection(
    findings: List<HealthFinding>,
    onTypeSelected: (String) -> Unit,
    onSceneSelected: (String) -> Unit,
    onPageSelected: (String) -> Unit,
) {
    if (findings.isEmpty()) return
    item(key = "health") {
        Text(
            modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
            text = stringResource(Res.string.game_map_worth_a_look),
            style = MaterialTheme.typography.titleSmall,
        )
    }
    items(findings, key = { "health:$it" }) { finding ->
        Text(
            modifier = Modifier.gameMapRow {
                when (val subject = finding.subject) {
                    is RelationSubject.Type -> onTypeSelected(subject.name)
                    is RelationSubject.Property -> onTypeSelected(subject.owner)
                    is RelationSubject.Scene -> onSceneSelected(subject.name)
                    is RelationSubject.Page -> onPageSelected(subject.name)
                    is RelationSubject.Image -> Unit
                }
            },
            text = finding.text(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}
