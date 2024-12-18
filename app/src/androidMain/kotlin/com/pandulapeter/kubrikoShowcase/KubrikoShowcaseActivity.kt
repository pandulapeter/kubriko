package com.pandulapeter.kubrikoShowcase

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf

class KubrikoShowcaseActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KubrikoShowcase(
                isInFullscreenMode = isInFullscreenMode.value,
                onFullscreenModeToggled = { isInFullscreenMode.value = !isInFullscreenMode.value },
            )
        }
    }
}

private val isInFullscreenMode = mutableStateOf(false)