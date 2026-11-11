/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands

import com.pandulapeter.kubrikoCreator.foundation.DomainText

/**
 * Why a command couldn't find what it names: a type, a Property, a Scene, a file, a key, a tool, or an index. Each says
 * what to use instead where there is something to say: the close names (a `_SIMILAR` reason, its last argument), the
 * other kind of type the name is, or the indexes there are.
 */
public enum class LookupProblem(override val english: String) : DomainText {
    ACTOR_PROPERTY_MISSING("%1\$s has no Property called %2\$s."),
    ACTOR_PROPERTY_MISSING_SIMILAR("%1\$s has no Property called %2\$s. Close matches: %3\$s."),
    ACTOR_TYPE_IS_MANAGER_TYPE("%1\$s is a Manager type, not an Actor type."),
    ACTOR_TYPE_MISSING("There's no Actor type called %1\$s."),
    ACTOR_TYPE_MISSING_SIMILAR("There's no Actor type called %1\$s. Close matches: %2\$s."),
    BLOCKS_COUNT_TOO_LARGE("Event %2\$s of %1\$s has %3\$s Blocks from index %4\$s on, so remove at most that many."),
    BLOCK_INSERTION_INDEX_INVALID("Event %2\$s of %1\$s has %3\$s Blocks, so Blocks go in at an index from 0 to %3\$s."),
    BLOCK_MISSING("Event %2\$s of %1\$s has no Block at index %3\$s. Its Blocks are at indexes 0 to %4\$s."),
    BLOCK_MISSING_NONE("Event %2\$s of %1\$s has no Blocks yet, so none at index %3\$s."),
    DOCUMENT_MISSING("There's no document at %1\$s."),
    DOCUMENT_MISSING_SIMILAR("There's no document at %1\$s. Close matches: %2\$s."),
    EVENT_MISSING("%1\$s has no Event at index %2\$s. Its Events are at indexes 0 to %3\$s."),
    EVENT_MISSING_NONE("%1\$s has no Events yet, so none at index %2\$s."),
    FOLDER_MISSING("There's no folder called %1\$s."),
    FOLDER_MISSING_SIMILAR("There's no folder called %1\$s. Close matches: %2\$s."),
    GAME_KEY_UNKNOWN("There's no key called %1\$s."),
    GAME_KEY_UNKNOWN_SIMILAR("There's no key called %1\$s. Close matches: %2\$s."),
    INDEX_INVALID("%1\$s isn't a valid index there. Use one from 0 to %2\$s."),
    MANAGER_TYPE_IS_ACTOR_TYPE("%1\$s is an Actor type, not a Manager type."),
    MANAGER_TYPE_MISSING("There's no Manager type called %1\$s."),
    MANAGER_TYPE_MISSING_SIMILAR("There's no Manager type called %1\$s. Close matches: %2\$s."),
    MUSIC_MISSING("There's no music at %1\$s. Music is an .mp3 or .ogg file in the files folder of composeResources."),
    MUSIC_MISSING_SIMILAR("There's no music at %1\$s. Close matches: %2\$s."),
    PAGE_MISSING("There's no page called %1\$s."),
    PAGE_MISSING_SIMILAR("There's no page called %1\$s. Close matches: %2\$s."),
    PAGE_SECTION_MISSING("%1\$s has no section headed %2\$s."),
    PAGE_SECTION_MISSING_SIMILAR("%1\$s has no section headed %2\$s. Close matches: %3\$s."),
    PAGE_SECTION_MISSING_LISTED("%1\$s has no section headed %2\$s. Its sections are: %3\$s."),
    PROPERTY_UNKNOWN("%1\$s has no Property called %2\$s."),
    PROPERTY_UNKNOWN_SIMILAR("%1\$s has no Property called %2\$s. Close matches: %3\$s."),
    RELATIONS_PROPERTY_MISSING("There's no Property called %1\$s in %2\$s."),
    RELATIONS_PROPERTY_MISSING_SIMILAR("There's no Property called %1\$s in %2\$s. Close matches: %3\$s."),
    SCENE_ACTOR_MISSING("%1\$s has no Actor with the ID %2\$s."),
    SCENE_ACTOR_MISSING_SIMILAR("%1\$s has no Actor with the ID %2\$s. Close matches: %3\$s."),
    SCENE_MISSING("There's no Scene called %1\$s."),
    SCENE_MISSING_SIMILAR("There's no Scene called %1\$s. Close matches: %2\$s."),
    SOUND_MISSING("There's no sound at %1\$s. Sounds are .wav files in the files folder of composeResources."),
    SOUND_MISSING_SIMILAR("There's no sound at %1\$s. Close matches: %2\$s."),
    SPRITE_MISSING("There's no sprite at %1\$s. Sprites are .png, .jpg, or .webp images right in the drawable folder of composeResources."),
    SPRITE_MISSING_SIMILAR("There's no sprite at %1\$s. Close matches: %2\$s."),
    TEXT_MISSING("%1\$s has no text %2\$s. Its texts are numbered 0 to %3\$s."),
    TEXT_MISSING_NONE("%1\$s has no texts yet, so none at index %2\$s."),
    TIMER_UNKNOWN("%1\$s has no Timer called %2\$s."),
    TIMER_UNKNOWN_SIMILAR("%1\$s has no Timer called %2\$s. Close matches: %3\$s."),
    TYPE_MISSING("There's no Actor type or Manager type called %1\$s."),
    TYPE_MISSING_SIMILAR("There's no Actor type or Manager type called %1\$s. Close matches: %2\$s."),
    UNKNOWN("There's no command called %1\$s."),
    UNKNOWN_SIMILAR("There's no command called %1\$s. Close matches: %2\$s."),
}
