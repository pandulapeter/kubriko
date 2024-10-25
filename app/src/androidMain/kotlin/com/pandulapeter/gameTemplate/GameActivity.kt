package com.pandulapeter.gameTemplate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.Modifier

class GameActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            App(
                modifier = Modifier.systemBarsPadding(),
            )
        }
    }
}