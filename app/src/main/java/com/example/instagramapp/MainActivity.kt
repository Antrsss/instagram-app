package com.example.instagramapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.instagramapp.navigation.InstagramBottomBar
import com.example.instagramapp.navigation.InstagramNavigation
import com.example.instagramapp.navigation.InstagramTopBar
import com.example.instagramapp.navigation.Screen
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
                        InstagramApp()
                    }
                    val padding = innerPadding
                }
            }
        }
    }
}

@Composable
fun InstagramApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route ?: Screen.Auth.route

    Scaffold(
        topBar = {
            when (currentRoute) {
                Screen.Profile.route -> InstagramTopBar(
                    title = "Profile",
                    canNavigateBack = navController.previousBackStackEntry != null,
                    navigateUp = { navController.navigateUp() }
                )
                // Добавьте другие экраны по необходимости
                else -> InstagramTopBar(
                    title = "Instagram",
                    canNavigateBack = navController.previousBackStackEntry != null,
                    navigateUp = { navController.navigateUp() }
                )
            }
        },
        bottomBar = {
            if (currentRoute != Screen.Auth.route) {
                InstagramBottomBar(navController = navController)
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            InstagramNavigation(navController = navController, modifier = Modifier)
        }
    }
}