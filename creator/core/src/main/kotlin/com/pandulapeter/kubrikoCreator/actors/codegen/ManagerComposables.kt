/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors.codegen

import com.pandulapeter.kubrikoCreator.actors.GeneratedCodeLocation
import com.pandulapeter.kubrikoCreator.actors.KotlinIdentifiers
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition

/**
 * The hand-written Composables Manager types show ([ManagerTypeDefinition.composable]): what one is called, and the file
 * Kubriko Creator starts a new one in, which is the user's from then on.
 */
public object ManagerComposables {
    private val identifier = Regex("[A-Za-z_][A-Za-z0-9_]*")

    /**
     * Names a new Composable's file imports, which the function itself can't have.
     */
    private val importedNames = setOf("Composable", "WindowInsets")

    /**
     * The package new Composables go to, beside the generated one.
     */
    public const val PACKAGE: String = "ui"

    /**
     * Whether [name] can be a Composable's fully qualified name: one in a package, since the generated code imports it.
     */
    public fun isValid(name: String): Boolean = name.split('.').let { parts -> parts.size > 1 && parts.all(::isIdentifier) }

    /**
     * Whether [name] can be what a new Composable for the Manager type [managerTypeName] is called: a Kotlin identifier
     * that none of its file's imports has.
     */
    public fun isValidFunctionName(name: String, managerTypeName: String): Boolean = isIdentifier(name) && name !in importedNames && name != managerTypeName

    private fun isIdentifier(name: String) = identifier.matches(name) && name !in KotlinIdentifiers.hardKeywords

    /**
     * What a new Composable for the Manager type [managerTypeName] is called by default.
     */
    public fun defaultNameOf(managerTypeName: String): String = managerTypeName + "Ui"

    /**
     * The fully qualified name of the Composable called [functionName] made in a project whose code is at [location].
     */
    public fun qualifiedNameOf(location: GeneratedCodeLocation, functionName: String): String = "${location.packageName}.$PACKAGE.$functionName"

    /**
     * The path of the file the Composable called [functionName] is made in, relative to the project root.
     */
    public fun pathOf(location: GeneratedCodeLocation, functionName: String): String = "${location.directoryOf(PACKAGE)}/$functionName.kt"

    /**
     * The name the Manager is passed to a Composable as, as other Manager types are reached in the generated code.
     */
    public fun parameterNameOf(managerTypeName: String): String = managerTypeName.replaceFirstChar { it.lowercase() } + "Manager"

    /**
     * What a new Composable called [functionName] for the Manager type [managerTypeName] starts as: a function taking the
     * Manager, whose Properties are Compose state, and the game view's insets, drawing nothing yet.
     */
    public fun textOf(location: GeneratedCodeLocation, managerTypeName: String, functionName: String): String = """
        |package ${location.packageName}.$PACKAGE
        |
        |import androidx.compose.foundation.layout.WindowInsets
        |import androidx.compose.runtime.Composable
        |import ${location.packageName}.${ActorTypeCodeGenerator.GENERATED_PACKAGE}.$managerTypeName
        |
        |/**
        | * What the Manager type $managerTypeName shows on top of the game, after its texts. Its Properties are Compose state,
        | * so what reads them here follows them.
        | */
        |@Composable
        |fun $functionName(${parameterNameOf(managerTypeName)}: $managerTypeName, windowInsets: WindowInsets) {
        |}
        |
    """.trimMargin()
}
