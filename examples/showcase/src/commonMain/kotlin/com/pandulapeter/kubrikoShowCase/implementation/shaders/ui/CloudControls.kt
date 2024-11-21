package com.pandulapeter.kubrikoShowcase.implementation.shaders.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.shader.collection.CloudShader

@Composable
internal fun CloudControls(
    modifier: Modifier = Modifier,
    properties: CloudShader.Properties,
    onPropertiesChanged: (CloudShader.Properties) -> Unit,
) = Card(
    modifier = modifier,
) {
    Column(
        modifier = Modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ColorSlider(
            title = "Sky 1",
            red = properties.sky1Red,
            green = properties.sky1Green,
            blue = properties.sky1Blue,
            onValueChanged = { red, green, blue ->
                onPropertiesChanged(
                    properties.copy(
                        sky1Red = red,
                        sky1Green = green,
                        sky1Blue = blue
                    )
                )
            },
            valueRange = 0f..1f,
        )
        ColorSlider(
            title = "Sky 2",
            red = properties.sky2Red,
            green = properties.sky2Green,
            blue = properties.sky2Blue,
            onValueChanged = { red, green, blue ->
                onPropertiesChanged(
                    properties.copy(
                        sky2Red = red,
                        sky2Green = green,
                        sky2Blue = blue
                    )
                )
            },
            valueRange = 0f..1f,
        )
    }
}