package com.example.instagramapp.navigation

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.instagramapp.screens.*
import com.example.instagramapp.viewmodels.AuthViewModel
import com.example.instagramapp.viewmodels.ProfileViewModel

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object Username : Screen("username")
    object Home : Screen("home")
    object Profile : Screen("profile/{userId}") {
        fun createRoute(userId: String) = "profile/$userId"
    }
    object Search : Screen("search")
    object Activity : Screen("activity")
    object Create : Screen("create")

}

data class BottomNavItem(
    val name: String,
    val route: String,
    val icon: ImageVector
)

@Composable
fun InstagramNavigation(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Auth.route,
        modifier = modifier
    ) {
        composable(Screen.Auth.route) { backStackEntry ->
            EmailPasswordScreen(
                authViewModel = authViewModel,
                onAuthenticated = { userId ->
                    navController.navigate(Screen.Username.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Username.route) {
            SetProfileUsernameScreen(
                authViewModel = authViewModel,
                profileViewModel = profileViewModel,
                onProfileCreated = { userUid ->
                    navController.navigate(Screen.Profile.createRoute(userUid)) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Home.route) {
            //HomeScreen(navController = navController)
        }
        composable(
            route = Screen.Profile.route,
            arguments = listOf(navArgument("userId") {
                type = NavType.StringType
            })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            ProfileScreen(
                userId = userId,
                navController = navController
            )
        }
        composable(Screen.Search.route) {
            //SearchScreen(navController = navController)
        }
        composable(Screen.Activity.route) {
            //ActivityScreen(navController = navController)
        }
        composable(Screen.Create.route) {
            //CreateScreen(navController = navController)
        }
    }
}

@Composable
fun InstagramBottomBar(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        BottomNavItem(
            name = "Home",
            route = Screen.Home.route,
            icon = Icons.Default.Home
        ),
        BottomNavItem(
            name = "Search",
            route = Screen.Search.route,
            icon = Icons.Default.Search
        ),
        BottomNavItem(
            name = "Create",
            route = Screen.Create.route,
            icon = Icons.Default.Add
        ),
        BottomNavItem(
            name = "Profile",
            route = Screen.Profile.createRoute("current_user"), // Замените на реальный userId
            icon = Icons.Default.Person
        )
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.name) },
                label = { Text(item.name) },
                selected = when {
                    item.route.startsWith("profile/") && currentRoute?.startsWith("profile/") == true -> true
                    else -> currentRoute == item.route
                },
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    }
}