package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.Coil
import coil.compose.LocalImageLoader
import com.example.ui.components.shelf.ShelfImageLoader
import com.example.ui.screens.GameLibraryScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.GameVaultViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val imageLoader = ShelfImageLoader.getImageLoader(this)
        Coil.setImageLoader(imageLoader)

        setContent {
            CompositionLocalProvider(LocalImageLoader provides imageLoader) {
                MyApplicationTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color(0xFF090D16)
                    ) {
                        val viewModel: GameVaultViewModel = viewModel()
                        GameLibraryScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

