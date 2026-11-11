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

import androidx.compose.runtime.Composable
import com.pandulapeter.kubrikoCreator.context.HealthFinding
import com.pandulapeter.kubrikoCreator.context.KotlinBlockScopeChecks
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.block_property_of_manager
import kubriko.creator.generated.resources.health_collision_with_absent_type
import kubriko.creator.generated.resources.health_first_scene_missing
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
import kubriko.creator.generated.resources.health_unreached_scene_not_first
import kubriko.creator.generated.resources.health_unread_property
import kubriko.creator.generated.resources.health_unused_actor_type
import kubriko.creator.generated.resources.health_unused_page_image
import kubriko.creator.generated.resources.timer_of_type
import org.jetbrains.compose.resources.stringResource

/**
 * What [this] says, in the user's words.
 */
@Composable
internal fun HealthFinding.text(): String {
    val subjectText = when (val subject = subject) {
        is RelationSubject.Type -> subject.name
        is RelationSubject.Property -> stringResource(Res.string.block_property_of_manager, subject.name, subject.owner)
        is RelationSubject.Scene -> subject.name
        is RelationSubject.Page -> subject.name
        is RelationSubject.Image -> subject.path
        is RelationSubject.Resource -> subject.path
        is RelationSubject.Timer -> stringResource(Res.string.timer_of_type, subject.name, subject.owner)
    }
    return when (kind) {
        HealthFinding.Kind.UNUSED_ACTOR_TYPE -> stringResource(Res.string.health_unused_actor_type, subjectText)
        HealthFinding.Kind.UNREACHED_SCENE -> detail?.let { stringResource(Res.string.health_unreached_scene_not_first, subjectText, it) }
            ?: stringResource(Res.string.health_unreached_scene, subjectText)
        HealthFinding.Kind.FIRST_SCENE_MISSING -> stringResource(Res.string.health_first_scene_missing, subjectText)
        HealthFinding.Kind.UNREAD_PROPERTY -> stringResource(Res.string.health_unread_property, subjectText)
        HealthFinding.Kind.TIMER_NEVER_STARTED -> stringResource(Res.string.health_timer_never_started, subjectText, detail.orEmpty())
        HealthFinding.Kind.COLLISION_WITH_ABSENT_TYPE -> stringResource(Res.string.health_collision_with_absent_type, subjectText, detail.orEmpty())
        HealthFinding.Kind.UNUSED_PAGE_IMAGE -> stringResource(Res.string.health_unused_page_image, subjectText)
        HealthFinding.Kind.PAGE_IMAGE_MISSING -> stringResource(Res.string.health_page_image_missing, subjectText, usage.orEmpty())
        HealthFinding.Kind.UNMENTIONED -> stringResource(Res.string.health_unmentioned, subjectText)
        HealthFinding.Kind.MENTION_MISSING -> stringResource(Res.string.health_mention_missing, subjectText, usage.orEmpty())
        HealthFinding.Kind.PAGE_WITHOUT_TITLE -> stringResource(Res.string.health_page_no_title, subjectText)
        HealthFinding.Kind.PAGE_WITHOUT_SUMMARY -> stringResource(Res.string.health_page_no_summary, subjectText)
        HealthFinding.Kind.PLANNED_MAY_BE_BUILT -> stringResource(Res.string.health_planned_may_be_built, subjectText, detail.orEmpty())
        HealthFinding.Kind.REFERENCE_MISSING -> stringResource(Res.string.health_reference_missing, detail.orEmpty(), usage.orEmpty())
        HealthFinding.Kind.KOTLIN_BLOCK_SCOPE_CHANGED -> if (usage == KotlinBlockScopeChecks.RETURN) {
            stringResource(Res.string.health_kotlin_block_return, detail.orEmpty())
        } else {
            stringResource(Res.string.health_kotlin_block_removed_local, detail.orEmpty(), usage.orEmpty())
        }
    }
}
