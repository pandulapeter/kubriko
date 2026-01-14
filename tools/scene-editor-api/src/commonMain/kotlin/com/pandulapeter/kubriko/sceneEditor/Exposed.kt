/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor

import com.pandulapeter.kubriko.actor.Actor


/**
 * Use this annotation on property setters of [Editable] [Actor]s to expose those properties to the Scene Editor.
 * @param name - The name the Scene Editor will display on its UI for the property
 */
@Target(AnnotationTarget.PROPERTY_SETTER)
annotation class Exposed(
    val name: String,
)