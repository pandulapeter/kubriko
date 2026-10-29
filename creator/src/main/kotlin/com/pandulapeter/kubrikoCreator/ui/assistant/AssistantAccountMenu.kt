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
import com.pandulapeter.kubrikoCreator.ai.assistant.AuthStatus
import com.pandulapeter.kubrikoCreator.ai.assistant.SignInMethod
import com.pandulapeter.kubrikoCreator.ui.components.MenuButton
import com.pandulapeter.kubrikoCreator.ui.components.WithTooltip
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.assistant_account_api_key
import kubriko.creator.generated.resources.assistant_account_menu
import kubriko.creator.generated.resources.assistant_account_not_signed_in
import kubriko.creator.generated.resources.assistant_account_remove_api_key
import kubriko.creator.generated.resources.assistant_account_sign_in
import kubriko.creator.generated.resources.assistant_account_signed_in
import kubriko.creator.generated.resources.assistant_account_use_api_key
import org.jetbrains.compose.resources.stringResource

/**
 * Who the agent works as, as a menu of the ways of signing in another way: the agent's own sign-in methods and an API key.
 * Shown once the agent has said who it works as, or when the user has given an API key.
 */
@Composable
internal fun AssistantAccountMenu(
    status: AuthStatus?,
    methods: List<SignInMethod>,
    hasApiKey: Boolean,
    canKeepApiKey: Boolean,
    onSignIn: (SignInMethod) -> Unit,
    onUseApiKey: () -> Unit,
    onRemoveApiKey: () -> Unit,
) {
    if (status == null && !hasApiKey) return
    WithTooltip(stringResource(Res.string.assistant_account_menu)) {
        MenuButton(
            label = status.label(),
            options = accountActions(methods, hasApiKey, canKeepApiKey),
            optionText = { action ->
                when (action) {
                    is AccountAction.SignIn -> stringResource(Res.string.assistant_account_sign_in, action.method.name)
                    AccountAction.UseApiKey -> stringResource(Res.string.assistant_account_use_api_key)
                    AccountAction.RemoveApiKey -> stringResource(Res.string.assistant_account_remove_api_key)
                }
            },
            onSelected = { action ->
                when (action) {
                    is AccountAction.SignIn -> onSignIn(action.method)
                    AccountAction.UseApiKey -> onUseApiKey()
                    AccountAction.RemoveApiKey -> onRemoveApiKey()
                }
            },
        )
    }
}

internal sealed interface AccountAction {
    data class SignIn(
        val method: SignInMethod,
    ) : AccountAction

    data object UseApiKey : AccountAction

    data object RemoveApiKey : AccountAction
}

/**
 * What the account menu offers: the agent's sign-in methods (which an API key outranks, so they are left out while one is
 * used), then giving an API key or another one, and forgetting it.
 */
internal fun accountActions(methods: List<SignInMethod>, hasApiKey: Boolean, canKeepApiKey: Boolean): List<AccountAction> =
    methods.takeUnless { hasApiKey }.orEmpty().map(AccountAction::SignIn) +
        listOfNotNull(
            AccountAction.UseApiKey.takeIf { canKeepApiKey },
            AccountAction.RemoveApiKey.takeIf { hasApiKey },
        )

/**
 * Before the agent says who it works as, only a given API key is known.
 */
@Composable
private fun AuthStatus?.label() = when {
    this == null -> stringResource(Res.string.assistant_account_api_key)
    !isSignedIn -> stringResource(Res.string.assistant_account_not_signed_in)
    email != null -> stringResource(Res.string.assistant_account_signed_in, label, email)
    else -> label
}
