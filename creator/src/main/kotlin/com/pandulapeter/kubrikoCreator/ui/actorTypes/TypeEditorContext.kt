/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.actorTypes

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.ActorTypeFamily
import com.pandulapeter.kubrikoCreator.actors.AudioFiles
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import java.util.IdentityHashMap

/**
 * What the type editor is handed besides the type itself: what its Blocks can refer to, and what it inherits.
 */
internal data class TypeEditorContext(
    val scope: BlockScope,
    val inheritance: Inheritance,
)

/**
 * Makes [TypeEditorContext]s, building their parts again only when what they are made of changes: the types' outlines
 * ([TypeOutline]), the Scenes, and the audio files for the scope, and for the inheritance also the Events of the type's
 * ancestors and the triggers of its own. Typing into a Block changes none of these, so the editor keeps the same scope and
 * inheritance, and a project of hundreds of types isn't built into a family again per keystroke.
 */
internal class TypeEditorContextCache {
    private var types: List<BlockTypeDefinition>? = null
    private var outlines: List<TypeOutline>? = null
    private var outlinesByDefinition = IdentityHashMap<BlockTypeDefinition, TypeOutline>()
    private var family: ActorTypeFamily? = null
    private var managerTypes: List<ManagerTypeDefinition> = emptyList()
    private var actorTypesByName: Map<String, ActorTypeDefinition> = emptyMap()
    private var scopeKey: List<Any?>? = null
    private var scope: BlockScope? = null
    private var inheritanceKey: List<Any?>? = null
    private var inheritance: Inheritance? = null

    fun contextOf(
        definition: BlockTypeDefinition,
        types: List<BlockTypeDefinition>,
        sceneNames: List<String>,
        audioFiles: AudioFiles,
    ): TypeEditorContext {
        val family = familyOf(types)
        val scopeKey = listOf(family, definition.name, sceneNames, audioFiles)
        val scope = scope?.takeIf { scopeKey == this.scopeKey } ?: BlockScope.of(
            definition = definition,
            types = types,
            sceneNames = sceneNames,
            audioFiles = audioFiles,
            family = family,
        ).copy(managerTypes = managerTypes).also {
            this.scopeKey = scopeKey
            this.scope = it
        }
        val inheritanceKey = listOf(
            family,
            definition.name,
            definition.events.map { it.trigger },
            family.ancestorsOf(definition.name).map { actorTypesByName[it]?.events },
        )
        val inheritance = inheritance?.takeIf { inheritanceKey == this.inheritanceKey } ?: Inheritance.of(definition, types, family).also {
            this.inheritanceKey = inheritanceKey
            this.inheritance = it
        }
        return TypeEditorContext(scope, inheritance)
    }

    /**
     * The family of [types]' Actor types, the same instance for as long as their outlines stay the same. A definition
     * kept from the last list keeps its outline, so a keystroke outlines only the type it changed. The family and the
     * scope's Manager types are built from the outlines ([TypeOutline.toDefinition]), so what is kept holds no Events.
     */
    private fun familyOf(types: List<BlockTypeDefinition>): ActorTypeFamily {
        val current = family
        if (current != null && types === this.types) return current
        this.types = types
        actorTypesByName = types.filterIsInstance<ActorTypeDefinition>().associateBy { it.name }
        val previousOutlines = outlinesByDefinition
        outlinesByDefinition = IdentityHashMap<BlockTypeDefinition, TypeOutline>(types.size)
        val outlines = types.map { definition -> (previousOutlines[definition] ?: TypeOutline.of(definition)).also { outlinesByDefinition[definition] = it } }
        if (current != null && outlines == this.outlines) return current
        this.outlines = outlines
        val definitions = outlines.map(TypeOutline::toDefinition)
        managerTypes = definitions.filterIsInstance<ManagerTypeDefinition>()
        return ActorTypeFamily(definitions.filterIsInstance<ActorTypeDefinition>()).also { family = it }
    }
}
