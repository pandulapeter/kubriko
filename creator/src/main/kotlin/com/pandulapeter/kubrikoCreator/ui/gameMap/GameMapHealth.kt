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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.context.HealthFinding
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.game_map_worth_a_look
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
