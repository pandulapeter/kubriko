/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

import com.pandulapeter.kubrikoCreator.foundation.DomainText

/**
 * Why a name can't be used for an Actor or Manager type, a Property, a Timer, a Scene, a page, a document, or a folder:
 * one entry per rule, so that each rejection names the rule the name actually breaks.
 */
public enum class NameProblem(override val english: String) : DomainText {
    NAME_EMPTY("Give it a name."),
    NAME_LETTER_BEYOND_A_TO_Z("Use only the letters a to z, without accents, since the name becomes part of the game's code or files."),
    NAME_KEYWORD("That word means something in Kotlin, the language of the game's code, so it can't be a name there. Try another one."),
    PATH_PART_EMPTY("Put a name on both sides of every /."),
    FOLDER_FIRST_CHARACTER("Start with a letter or a number."),
    FOLDER_CHARACTERS("Use only letters, numbers, spaces, dashes, and underscores."),
    FOLDER_END_SPACE("Don't end with a space."),
    PROPERTY_FIRST_CHARACTER("A Property's name starts with a lowercase letter, like score or isAlive."),
    PROPERTY_CHARACTERS("A Property's name uses only letters and numbers, with no spaces or other signs."),
    PROPERTY_RESERVED("That name is already used by the generated code. Try another one."),
    PROPERTY_TAKEN("This type already has a Property with that name, or one differing only in capital letters."),
    TIMER_FIRST_CHARACTER("A Timer's name starts with a lowercase letter, like spawnEnemy."),
    TIMER_CHARACTERS("A Timer's name uses only letters and numbers, with no spaces or other signs."),
    TYPE_BASE_SUFFIX("Names ending in \"Base\" are kept for the classes Kubriko Creator generates for parent types. Pick another one."),
    TYPE_FIRST_CHARACTER("Start with an uppercase letter, like Player or Enemy2."),
    TYPE_CHARACTERS("Use only letters and numbers, with no spaces or other signs, like Player or Enemy2."),
    TYPE_RESERVED("That name is used by Kubriko itself. Pick another one."),
    TYPE_TAKEN("There's already an Actor type or Manager type with that name."),
    SCENE_FIRST_CHARACTER("Start the Scene's name, and every folder in it, with a letter or a number."),
    SCENE_CHARACTERS("Use only letters, numbers, dashes, and underscores, like level-1, with a / between folders."),
    PAGE_NAME_TOO_LONG("A page's name, with its folders, can be at most %1\$s characters long."),
    DOCUMENT_PATH_TOO_LONG("A document's path can be at most %1\$s characters long."),
    DOCUMENT_NOT_MARKDOWN("A document's name ends with .md."),
    DOCUMENT_IN_SCREENPLAY("The screenplay folder holds pages, not documents. Choose another folder."),
    DOCUMENT_IN_LEFT_OUT_FOLDER("Kubriko Creator leaves build and tool folders out, so a document can't go in one. Choose another folder."),
    DOCUMENT_FIRST_CHARACTER("Start the document's name, and every folder it's in, with a letter or a number."),
    DOCUMENT_CHARACTERS("Use only letters, numbers, spaces, dots, dashes, and underscores in the document's name and its folders."),
    DOCUMENT_END("Don't end a folder's name with a space or a dot, which Windows drops."),
}
