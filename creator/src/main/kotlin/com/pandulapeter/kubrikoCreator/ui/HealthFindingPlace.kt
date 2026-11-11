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

import com.pandulapeter.kubrikoCreator.context.HealthFinding
import com.pandulapeter.kubrikoCreator.context.RelationSubject

/**
 * Where in Kubriko Creator a [HealthFinding] is shown and fixed: the editor of a type (a Property's too), a Scene, or a
 * page, or the game's settings, which name the Scene the game starts with.
 */
internal sealed interface HealthFindingPlace {
    data class Type(val name: String) : HealthFindingPlace

    data class Scene(val name: String) : HealthFindingPlace

    data class Page(val name: String) : HealthFindingPlace

    data object GameSettings : HealthFindingPlace
}

/**
 * Where this finding is shown and fixed, or null for one about an image among the pages, which nothing edits.
 */
internal val HealthFinding.place: HealthFindingPlace?
    get() = if (kind == HealthFinding.Kind.FIRST_SCENE_MISSING) {
        HealthFindingPlace.GameSettings
    } else {
        when (val subject = subject) {
            is RelationSubject.Type -> HealthFindingPlace.Type(subject.name)
            is RelationSubject.Property -> HealthFindingPlace.Type(subject.owner)
            is RelationSubject.Timer -> HealthFindingPlace.Type(subject.owner)
            is RelationSubject.Scene -> HealthFindingPlace.Scene(subject.name)
            is RelationSubject.Page -> HealthFindingPlace.Page(subject.name)
            is RelationSubject.Image, is RelationSubject.Resource, is RelationSubject.Folder -> null
        }
    }
