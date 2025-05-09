package com.example.instagramapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.instagramapp.screens.EmailPasswordScreen
import com.example.instagramapp.ui.theme.InstagramAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InstagramAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    InstagramAppTheme {
                        EmailPasswordScreen()
                    }
                    val padding = innerPadding
                }
            }
        }
    }
}