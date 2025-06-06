package com.example.instagramapp.navigation

import androidx.compose.runtime.getValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.instagramapp.models.Story
import com.example.instagramapp.screens.*
import com.example.instagramapp.viewmodels.AuthViewModel
import com.example.instagramapp.viewmodels.PostsViewModel
import com.example.instagramapp.viewmodels.ProfileViewModel
import com.example.instagramapp.viewmodels.StoriesViewModel

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Auth : Screen("auth")
    object Username : Screen("username")
    object Home : Screen("home")
    object Profile : Screen("profile/{userId}") {
        fun createRoute(userId: String) = "profile/$userId"
    }
    object Post : Screen("post/{postId}") {
        fun createRoute(postId: String) = "post/$postId"
    }
    object Search : Screen("search")
    object Create : Screen("create")
    object CreatePost : Screen("create/post")
    object CreateStory : Screen("create/story")
    object CreateContent : Screen("create/content")
    object StoryViewer : Screen("storyViewer/{startIndex}") {
        fun createRoute(startIndex: Int) = "storyViewer/${startIndex}"
    }
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
    val currentUser = authViewModel.currentUser
    val hasProfile by authViewModel.hasProfile.collectAsState()

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
                            Screen.Profile.createRoute(currentUser!!.uid)
                        }
                        else -> Screen.Username.route
                    }
                    navController.navigate(startDestination) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        authGraph(navController, authViewModel)
        usernameGraph(navController, authViewModel, profileViewModel)
        contentCreationGraph(navController)

        composable(Screen.Home.route) {
            HomeScreen(
                navController = navController,
            )
        }
        composable(
            route = Screen.StoryViewer.route,
            arguments = listOf(navArgument("startIndex") {
                type = NavType.IntType
            })
        ) { backStackEntry ->
            val stories = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<List<Story>>("stories") ?: emptyList()
            val startIndex = backStackEntry.arguments?.getInt("startIndex") ?: 0

            StoryViewerScreen(
                stories = stories,
                startIndex = startIndex,
                onBack = { navController.popBackStack() }
            )
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
        composable(
            route = Screen.Post.route,
            arguments = listOf(navArgument("postId") {
                type = NavType.StringType
            })
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            PostScreen(
                postId = postId,
                navController = navController
            )
        }
        composable(Screen.Search.route) {
            SearchScreen(
                onProfileClick = { userId ->
                    navController.navigate(Screen.Profile.createRoute(userId))
                }
            )
        }
        composable(Screen.Create.route) {
            CreateContentScreen(navController = navController)
        }
    }
}

private fun NavGraphBuilder.contentCreationGraph(
    navController: NavController
) {
    composable(Screen.CreateContent.route) {
        CreateContentScreen(navController = navController)
    }

    composable(Screen.CreatePost.route) {
        CreatePostScreen(
            navController = navController,
            postsViewModel = hiltViewModel()
        )
    }

    composable(Screen.CreateStory.route) {
        CreateStoryScreen(
            navController = navController,
            storiesViewModel = hiltViewModel()
        )
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
            route = "profile",
            icon = Icons.Default.Person,
            needsUserId = true
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
                        if (item.route == Screen.Create.route) {
                            popUpTo(navController.graph.startDestinationId)
                        }
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

private fun NavGraphBuilder.authGraph(
    navController: NavController,
    authViewModel: AuthViewModel
) {
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
}

private fun NavGraphBuilder.usernameGraph(
    navController: NavController,
    authViewModel: AuthViewModel,
    profileViewModel: ProfileViewModel
) {
    composable(Screen.Username.route) {
        val userId = authViewModel.currentUser?.uid ?: run {
            navController.navigate(Screen.Auth.route) {
                popUpTo(Screen.Username.route) { inclusive = true }
            }
            return@composable
        }

        SetProfileUsernameScreen(
            authViewModel = authViewModel,
            profileViewModel = profileViewModel,
            onProfileCreated = {
                navController.navigate(Screen.Profile.createRoute(userId)) {
                    popUpTo(Screen.Auth.route) { inclusive = true }
                }
            }
        )
    }
}