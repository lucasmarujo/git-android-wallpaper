package com.lucasmarujo.gitwallpaper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lucasmarujo.gitwallpaper.ui.GitWallpaperApp
import com.lucasmarujo.gitwallpaper.ui.theme.GitWallpaperTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as GitWallpaperApplication).container
        setContent {
            GitWallpaperTheme {
                GitWallpaperApp(container)
            }
        }
    }
}
