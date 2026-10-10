package com.ghartak.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.ghartak.app.presentation.navigation.GharTakNavHost
import com.ghartak.app.presentation.theme.GharTakTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // The manifest launches with the branded splash theme; switch to the normal theme
        // before the first frame of content so the window background isn't the splash colour.
        setTheme(R.style.Theme_GharTak)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GharTakTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GharTakNavHost()
                }
            }
        }
    }
}
