/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.schema

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialInfo

/**
 * Marks a sealed type whose subclasses, or an enum whose values, a read tool describes in full, so the tool schemas
 * generated from command inputs ([com.pandulapeter.kubrikoCreator.commands.JsonSchema]) list only their names, described
 * by [text], rather than repeating every one in each tool that takes one.
 */
@OptIn(ExperimentalSerializationApi::class)
@SerialInfo
@Target(AnnotationTarget.CLASS)
public annotation class SummarizedSchema(val text: String)
