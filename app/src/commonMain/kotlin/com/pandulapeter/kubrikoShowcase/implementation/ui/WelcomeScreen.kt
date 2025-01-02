package com.pandulapeter.kubrikoShowcase.implementation.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.shared.ui.LargeButton
import kubriko.app.generated.resources.Res
import kubriko.app.generated.resources.ic_github
import kubriko.app.generated.resources.ic_youtube
import kubriko.app.generated.resources.img_logo
import kubriko.app.generated.resources.welcome_additional_information
import kubriko.app.generated.resources.welcome_app_details
import kubriko.app.generated.resources.welcome_app_details_call_to_action_collapsed
import kubriko.app.generated.resources.welcome_app_details_call_to_action_expanded
import kubriko.app.generated.resources.welcome_engine_details
import kubriko.app.generated.resources.welcome_github
import kubriko.app.generated.resources.welcome_message
import kubriko.app.generated.resources.welcome_show_more
import kubriko.app.generated.resources.welcome_youtube
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun WelcomeScreen(
    modifier: Modifier = Modifier,
    shouldUseCompactUi: Boolean,
) = Column(
    modifier = modifier
        .fillMaxWidth()
        .padding(vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            text = stringResource(Res.string.welcome_message),
        )
        Image(
            modifier = Modifier.height(72.dp),
            painter = painterResource(Res.drawable.img_logo),
            contentDescription = null,
        )
    }
    val shouldShowMoreInfo = remember { mutableStateOf(false) }
    if (shouldUseCompactUi && !shouldShowMoreInfo.value) {
        Text(
            modifier = Modifier.clickable { shouldShowMoreInfo.value = true }.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            text = stringResource(Res.string.welcome_show_more),
        )
    }
    if (!shouldUseCompactUi || shouldShowMoreInfo.value) {
        Text(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            style = MaterialTheme.typography.bodySmall,
            text = stringResource(Res.string.welcome_engine_details),
        )
        Text(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            style = MaterialTheme.typography.bodySmall,
            text = stringResource(Res.string.welcome_additional_information),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = if (shouldUseCompactUi) Arrangement.Center else Arrangement.Start,
        ) {
            val uriHandler = LocalUriHandler.current
            LargeButton(
                icon = Res.drawable.ic_github,
                title = Res.string.welcome_github,
                onButtonPressed = { uriHandler.openUri("https://github.com/pandulapeter/kubriko") },
            )
            Spacer(
                modifier = Modifier.width(8.dp),
            )
            LargeButton(
                icon = Res.drawable.ic_youtube,
                title = Res.string.welcome_youtube,
                isEnabled = false,
                onButtonPressed = {}, // TODO
            )
        }
    }
    Text(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        style = MaterialTheme.typography.bodySmall,
        text = stringResource(Res.string.welcome_app_details),
    )
    Text(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        style = MaterialTheme.typography.bodySmall,
        text = stringResource(if (shouldUseCompactUi) Res.string.welcome_app_details_call_to_action_collapsed else Res.string.welcome_app_details_call_to_action_expanded),
    )
}