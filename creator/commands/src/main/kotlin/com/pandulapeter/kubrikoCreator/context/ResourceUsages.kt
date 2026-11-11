/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.context

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.SpriteFiles
import com.pandulapeter.kubrikoCreator.screenplay.PageLinks

/**
 * The usages of the sprite, sound, or music file [subject] in Kubriko Creator's model ([ProjectReferences.usagesOf]):
 * the Actor types drawn as it, the Blocks playing it, the Scenes playing it as their music ([sceneMusic], by the
 * Scene's name), and the Kotlin Blocks and Kotlin UI naming it, which are left as they are.
 */
internal fun resourceUsagesOf(subject: RelationSubject.Resource, types: List<BlockTypeDefinition>, sceneMusic: Map<String, String>): List<Usage> = buildList {
    val isNaming = resourceNaming(subject)
    types.forEach { type ->
        if (type is ActorTypeDefinition && type.sprite == subject.path) {
            add(Usage(UsagePlace.InModel(RelationPlace.Type(type.name)), RelationKind.DRAWN_AS, subject, UsageEffect.SPRITE_CLEARED))
        }
        type.events.forEachIndexed { eventIndex, event ->
            event.blocks.forEachIndexed { blockIndex, block ->
                val place = UsagePlace.InModel(RelationPlace.Type(type.name, eventIndex, blockIndex))
                when {
                    block is BlockDefinition.PlaySound && block.sound == subject.path -> add(Usage(place, RelationKind.PLAYS, subject, UsageEffect.BLOCK_REMOVED))
                    block is BlockDefinition.PlayMusic && block.music == subject.path -> add(Usage(place, RelationKind.PLAYS, subject, UsageEffect.BLOCK_REMOVED))
                    block is BlockDefinition.Kotlin && isNaming(block.code) -> add(Usage(place, RelationKind.NAMES_IN_KOTLIN, subject, UsageEffect.KOTLIN_TO_CHANGE))
                }
            }
        }
        if ((type as? ManagerTypeDefinition)?.kotlinUi?.let(isNaming) == true) {
            add(Usage(UsagePlace.InModel(RelationPlace.KotlinUi(type.name)), RelationKind.NAMES_IN_KOTLIN, subject, UsageEffect.KOTLIN_TO_CHANGE))
        }
    }
    sceneMusic.keys.sorted().filter { sceneMusic[it] == subject.path }.forEach { scene ->
        add(Usage(UsagePlace.InModel(RelationPlace.Scene(scene)), RelationKind.PLAYS, subject, UsageEffect.MUSIC_CLEARED))
    }
}

/**
 * Whether Kotlin code names the sprite, sound, or music file [subject]: a sprite by its resource accessor
 * (`Res.drawable.ball`), a sound or music by its path in a string (`"files/sounds/hit.wav"`), as the game's code reads them.
 */
internal fun resourceNaming(subject: RelationSubject.Resource): (String) -> Boolean {
    val accessor = SpriteFiles.accessorOf(subject.path)
    return if (accessor != null) {
        Regex("\\bdrawable\\s*\\.\\s*" + Regex.escape(accessor) + "\\b")::containsMatchIn
    } else {
        "\"${subject.path}\"".let { quoted -> { text -> quoted in text } }
    }
}

/**
 * The pages among [markdown] (the text of each, by its path from the project's root) showing the image at [path] (from
 * the project's root), each once with how many images of it it has.
 */
internal fun imageUsagesOf(subject: RelationSubject.Resource, path: String, markdown: Map<String, String>): List<Usage> = markdown.keys.sorted().mapNotNull { file ->
    val count = PageLinks.linksOf(markdown.getValue(file)).count { it.isImage && PageLinks.resolve(file, it.destination) == path }
    if (count == 0) null else Usage(UsagePlace.Markdown(file), RelationKind.SHOWS, subject, UsageEffect.IMAGE_COPIED, count)
}
