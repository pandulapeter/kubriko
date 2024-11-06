package com.pandulapeter.kubrikoKeyboardInputTest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat

class GameActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameKeyboardInputTest(
                modifier = Modifier
                    .systemBarsPadding()
                    .displayCutoutPadding()
                    .imePadding(),
            )
        }
    }

    override fun onResume() {
        super.onResume()
        WindowCompat.getInsetsController(window, window.decorView).run {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }
}