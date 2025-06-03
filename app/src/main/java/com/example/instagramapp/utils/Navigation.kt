package com.example.instagramapp.navigation

import androidx.compose.runtime.getValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
    object Splash : Screen("splash")

    object Auth : Screen("auth")
    object Username : Screen("username")

    object Home : Screen("home")
    object Profile : Screen("profile/{userId}") {
        fun createRoute(userId: String) = "profile/$userId"
    }
    object Search : Screen("search")
    object Create : Screen("create")

}

data class BottomNavItem(
    val name: String,
    val route: String,
    val icon: ImageVector,
    val needsUserId: Boolean = false
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
        startDestination = Screen.Splash.route,
        modifier = modifier
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onLoadingComplete = {
                    val startDestination = when {
                        authViewModel.currentUser == null -> Screen.Auth.route
                        authViewModel.hasProfile.value -> {
                            val userUid = authViewModel.currentUser!!.uid
                            Screen.Profile.createRoute(userUid)
                        }
                        else -> Screen.Username.route
                    }
                    navController.navigate(startDestination) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Auth.route) { backStackEntry ->
            EmailPasswordScreen(
                authViewModel = authViewModel,
                onNavigateToUsername = {
                    navController.navigate(Screen.Username.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                },
                onNavigateToProfile = { userUid ->
                    navController.navigate(Screen.Profile.createRoute(userUid)) {
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
        composable(Screen.Create.route) {
            //CreateScreen(navController = navController)
        }
    }
}

@Composable
fun InstagramBottomBar(
    navController: NavController,
    authViewModel: AuthViewModel = hiltViewModel(),
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
            route = "profile",  // Базовый маршрут без ID
            icon = Icons.Default.Person,
            needsUserId = true  // Указываем, что требуется userId
        )
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val currentUser = authViewModel.currentUser

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
                    val finalRoute = if (item.needsUserId && currentUser != null) {
                        Screen.Profile.createRoute(currentUser.uid)
                    } else {
                        item.route
                    }

                    navController.navigate(finalRoute) {
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