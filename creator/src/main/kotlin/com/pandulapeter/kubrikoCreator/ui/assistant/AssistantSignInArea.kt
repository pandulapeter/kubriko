/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.assistant

import androidx.compose.runtime.Composable
import com.pandulapeter.kubrikoCreator.ai.assistant.SignInMethod
import com.pandulapeter.kubrikoCreator.ai.assistant.SignInSession

/**
 * Whatever of signing in the panel shows above the composer: the running sign-in command, the API key form, or, while
 * the agent isn't signed in, the ways of signing in.
 */
@Composable
internal fun AssistantSignInArea(
    session: SignInSession?,
    isEnteringApiKey: Boolean,
    needsSignIn: Boolean,
    methods: List<SignInMethod>,
    canKeepApiKey: Boolean,
    hasApiKeyFailed: Boolean,
    onSignIn: (SignInMethod) -> Unit,
    onCloseSignIn: () -> Unit,
    onUseApiKey: () -> Unit,
    onSaveApiKey: (String) -> Unit,
    onCancelApiKey: () -> Unit,
) = when (signInAreaContent(session != null, isEnteringApiKey, needsSignIn)) {
    SignInAreaContent.TERMINAL -> session?.let { SignInTerminal(session = it, onClose = onCloseSignIn) }
    SignInAreaContent.API_KEY -> ApiKeyForm(hasFailed = hasApiKeyFailed, onSave = onSaveApiKey, onCancel = onCancelApiKey)
    SignInAreaContent.METHODS -> SignInCard(methods = methods, canKeepApiKey = canKeepApiKey, onSignIn = onSignIn, onUseApiKey = onUseApiKey)
    null -> Unit
}

internal enum class SignInAreaContent { TERMINAL, API_KEY, METHODS }

/**
 * What the sign-in area shows: a running sign-in first, then the API key form the user asked for, then the methods
 * while signing in is needed.
 */
internal fun signInAreaContent(isSigningIn: Boolean, isEnteringApiKey: Boolean, needsSignIn: Boolean) = when {
    isSigningIn -> SignInAreaContent.TERMINAL
    isEnteringApiKey -> SignInAreaContent.API_KEY
    needsSignIn -> SignInAreaContent.METHODS
    else -> null
}
