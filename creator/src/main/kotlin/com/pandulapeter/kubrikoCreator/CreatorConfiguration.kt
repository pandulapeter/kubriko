/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator

import java.util.Properties

/**
 * The versions Kubriko Creator writes into the projects it creates.
 *
 * They are bundled at build time (a development build bundles the version it just published to Maven Local, along with
 * `useMavenLocal`); the `kubriko.creator.kubrikoVersion` and `kubriko.creator.useMavenLocal` system properties override them.
 */
internal data class CreatorConfiguration(
    val kubrikoVersion: String,
    val kotlinVersion: String,
    val composeVersion: String,
    val gradleVersion: String,
    val useMavenLocal: Boolean,
) {
    companion object {
        private const val RESOURCE_NAME = "creator.properties"

        fun load() = from(
            bundled = Properties().apply {
                CreatorConfiguration::class.java.classLoader.getResourceAsStream(RESOURCE_NAME)?.use(::load)
                    ?: error("$RESOURCE_NAME is missing from the app's resources.")
            },
            overrides = System.getProperties(),
        )

        internal fun from(bundled: Properties, overrides: Properties) = CreatorConfiguration(
            kubrikoVersion = overrides.getProperty("kubriko.creator.kubrikoVersion") ?: bundled.require("kubrikoVersion"),
            kotlinVersion = bundled.require("kotlinVersion"),
            composeVersion = bundled.require("composeVersion"),
            gradleVersion = bundled.require("gradleVersion"),
            useMavenLocal = (overrides.getProperty("kubriko.creator.useMavenLocal") ?: bundled.getProperty("useMavenLocal")) == "true",
        )

        private fun Properties.require(key: String) = getProperty(key) ?: error("$RESOURCE_NAME has no $key.")
    }
}
