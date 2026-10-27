/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.creatorSession.implementation

import com.pandulapeter.kubriko.creatorSession.SessionFailure
import java.lang.reflect.InvocationTargetException

/**
 * Runs the game through its own main function, so that it behaves exactly as when started from its build.
 */
internal object PlaySession {

    fun run(arguments: SessionArguments.Play) {
        val mainFunction = try {
            Class.forName(arguments.mainClassName).getMethod("main", Array<String>::class.java)
        } catch (exception: ReflectiveOperationException) {
            throw SessionFailure("The game's main class ${arguments.mainClassName} has no main function.", exception)
        }
        try {
            mainFunction.invoke(null, emptyArray<String>())
        } catch (exception: InvocationTargetException) {
            throw exception.targetException
        }
    }
}
