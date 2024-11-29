package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
internal fun EditorButton(
    modifier: Modifier = Modifier,
    text: String,
    isEnabled: Boolean = true,
    onClick: () -> Unit,
) {
    val colors = ButtonDefaults.buttonColors()
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(24.dp).padding(horizontal = 8.dp, vertical = 4.dp).semantics { role = Role.Button },
        enabled = isEnabled,
        shape = ButtonDefaults.shape,
        color = if (isEnabled) colors.containerColor else colors.disabledContainerColor,
        contentColor = if (isEnabled) colors.contentColor else colors.disabledContentColor,
        interactionSource = remember { MutableInteractionSource() },
    ) {
        CompositionLocalProvider(
            LocalTextStyle provides LocalTextStyle.current.merge(MaterialTheme.typography.labelLarge),
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EditorText(
                    modifier = Modifier.fillMaxWidth(),
                    isCenterAligned = true,
                    text = text,
                )
            }
        }
    }
}