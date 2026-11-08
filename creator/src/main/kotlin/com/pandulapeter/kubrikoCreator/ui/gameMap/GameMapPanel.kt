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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.context.HealthFinding
import com.pandulapeter.kubrikoCreator.context.PageSummary
import com.pandulapeter.kubrikoCreator.context.ProjectDigest
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import com.pandulapeter.kubrikoCreator.ui.components.Accent
import com.pandulapeter.kubrikoCreator.ui.components.AccentIcon
import com.pandulapeter.kubrikoCreator.ui.components.fullBleed
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.block_property_of_manager
import kubriko.creator.generated.resources.game_map_actor_types
import kubriko.creator.generated.resources.game_map_empty
import kubriko.creator.generated.resources.game_map_hint
import kubriko.creator.generated.resources.game_map_inherits_from
import kubriko.creator.generated.resources.game_map_manager_types
import kubriko.creator.generated.resources.game_map_no_description
import kubriko.creator.generated.resources.game_map_page_title
import kubriko.creator.generated.resources.game_map_pages
import kubriko.creator.generated.resources.game_map_places
import kubriko.creator.generated.resources.game_map_scenes
import kubriko.creator.generated.resources.ic_actor_type
import kubriko.creator.generated.resources.ic_manager_type
import kubriko.creator.generated.resources.ic_page
import kubriko.creator.generated.resources.ic_scene
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The map of the game: the same digest that opens every conversation of the assistant, shown to the user, with each
 * type and Scene opening on a click.
 *
 * @param digest The digest of the open project, or null while it is worked out.
 * @param findings What about the game is worth a look, shown first.
 * @param pages The screenplay's pages, listed first, since they are the game's design.
 * @param onPageSelected Opens a page.
 */
@Composable
internal fun GameMapPanel(
    digest: ProjectDigest?,
    onTypeSelected: (String) -> Unit,
    onSceneSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    findings: List<HealthFinding> = emptyList(),
    pages: List<PageSummary> = emptyList(),
    onPageSelected: (String) -> Unit = {},
) = Column(
    modifier = modifier,
) {
    val references = remember(digest) { digest?.let(::referencesByOwner).orEmpty() }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = GAME_MAP_GUTTER, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item {
            Text(
                modifier = Modifier.padding(bottom = 6.dp),
                text = stringResource(Res.string.game_map_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (digest == null) return@LazyColumn
        healthSection(findings, onTypeSelected, onSceneSelected, onPageSelected)
        if (pages.isEmpty() && digest.actorTypes.isEmpty() && digest.managerTypes.isEmpty() && digest.scenes.isEmpty()) {
            item { Text(stringResource(Res.string.game_map_empty), style = MaterialTheme.typography.bodySmall) }
        }
        if (pages.isNotEmpty()) {
            item(key = "pages") { SectionTitle(Res.string.game_map_pages) }
            items(pages, key = { "page:${it.name}" }) { page ->
                MapEntry(
                    icon = Res.drawable.ic_page,
                    accent = Accent.SCREENPLAY,
                    name = page.title?.let { stringResource(Res.string.game_map_page_title, page.name, it) } ?: page.name,
                    description = page.summary,
                    details = emptyList(),
                    onClick = { onPageSelected(page.name) },
                )
            }
        }
        typeSection(Res.string.game_map_actor_types, Res.drawable.ic_actor_type, Accent.CAST, digest.actorTypes, references, onTypeSelected)
        typeSection(Res.string.game_map_manager_types, Res.drawable.ic_manager_type, Accent.CREW, digest.managerTypes, references, onTypeSelected)
        if (digest.scenes.isNotEmpty()) {
            item(key = "scenes") { SectionTitle(Res.string.game_map_scenes) }
            items(digest.scenes, key = { "scene:${it.name}" }) { scene ->
                MapEntry(
                    icon = Res.drawable.ic_scene,
                    accent = Accent.SCENE,
                    name = scene.name,
                    description = scene.description,
                    details = listOfNotNull(
                        scene.actorCounts.takeIf { it.isNotEmpty() }?.let { stringResource(Res.string.game_map_places, placedText(it)) },
                    ),
                    onClick = { onSceneSelected(scene.name) },
                )
            }
        }
    }
}

private fun LazyListScope.typeSection(
    title: StringResource,
    icon: DrawableResource,
    accent: Accent,
    types: List<ProjectDigest.Type>,
    references: Map<RelationSubject, List<ProjectDigest.RelationLine>>,
    onTypeSelected: (String) -> Unit,
) {
    if (types.isEmpty()) return
    item(key = title.key) { SectionTitle(title) }
    items(types, key = { "type:${it.name}" }) { type ->
        MapEntry(
            icon = icon,
            accent = accent,
            name = if (type.folder.isEmpty()) type.name else "${type.folder}/${type.name}",
            description = type.description,
            details = listOfNotNull(type.parent?.let { stringResource(Res.string.game_map_inherits_from, it) }) +
                references[RelationSubject.Type(type.name)].orEmpty().map { line -> stringResource(line.kind.label, line.subjects.map { subjectText(it) }.joinToString(", ")) },
            onClick = { onTypeSelected(type.name) },
        )
    }
}

@Composable
private fun SectionTitle(
    title: StringResource,
) = Text(
    modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
    text = stringResource(title),
    style = MaterialTheme.typography.titleSmall,
)

@Composable
private fun MapEntry(
    icon: DrawableResource,
    accent: Accent,
    name: String,
    description: String?,
    details: List<String>,
    onClick: () -> Unit,
) = Row(
    modifier = Modifier.gameMapRow(onClick),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.Top,
) {
    AccentIcon(
        icon = icon,
        accent = accent,
        size = 20.dp,
    )
    Column {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = description ?: stringResource(Res.string.game_map_no_description),
            style = MaterialTheme.typography.bodySmall.let { if (description == null) it.copy(fontStyle = FontStyle.Italic) else it },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        details.forEach { detail ->
            Text(
                text = detail,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * How far the map's content is kept from the panel's edges, which its clickable rows reach past.
 */
internal val GAME_MAP_GUTTER = 12.dp

/**
 * A clickable row of the map, its background and press feedback reaching both edges of the panel.
 */
internal fun Modifier.gameMapRow(onClick: () -> Unit): Modifier = fullBleed(GAME_MAP_GUTTER)
    .fillMaxWidth()
    .clickable(onClick = onClick)
    .padding(horizontal = GAME_MAP_GUTTER, vertical = 6.dp)

@Composable
private fun subjectText(subject: RelationSubject) = when (subject) {
    is RelationSubject.Type -> subject.name
    is RelationSubject.Property -> stringResource(Res.string.block_property_of_manager, subject.name, subject.owner)
    is RelationSubject.Scene -> subject.name
    is RelationSubject.Page -> subject.name
    is RelationSubject.Image -> subject.path
}
