package com.example.skillswaper.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.skillswaper.ui.screens.main.*

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Post : Screen("post", "Post", Icons.Default.Add)
    object Notifications : Screen("notifications", "Alerts", Icons.Default.Notifications)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
    
    // Sub-screens
    object InquiryForm : Screen("inquiry/{skillId}/{skillName}/{toUserId}", "Inquiry") {
        fun createRoute(skillId: String, skillName: String, toUserId: String) = "inquiry/$skillId/$skillName/$toUserId"
    }
    object UserProfile : Screen("user_profile/{userId}", "User Profile") {
        fun createRoute(userId: String) = "user_profile/$userId"
    }
    object Payment : Screen("payment/{skillId}/{price}/{creatorId}/{skillName}", "Payment") {
        fun createRoute(skillId: String, price: String, creatorId: String, skillName: String) = 
            "payment/$skillId/${price.replace("$", "").trim()}/$creatorId/$skillName"
    }
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Post,
    Screen.Notifications,
    Screen.Profile
)

@Composable
fun MainNavigation(onSignOut: () -> Unit) {
    val navController = rememberNavController()
    
    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            
            // Only show bottom bar for main screens
            if (bottomNavItems.any { it.route == currentRoute }) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon!!, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { 
                HomeScreen(
                    onInquiryNavigate = { id, name, owner -> 
                        navController.navigate(Screen.InquiryForm.createRoute(id, name, owner)) 
                    },
                    onPayNavigate = { id, price, owner, name ->
                        navController.navigate(Screen.Payment.createRoute(id, price, owner, name))
                    }
                ) 
            }
            composable(Screen.Post.route) { 
                PostScreen(
                    onBack = { navController.popBackStack() },
                    onPostCreated = { navController.navigate(Screen.Home.route) }
                ) 
            }
            composable(Screen.Notifications.route) { 
                NotificationsScreen(onViewProfile = { userId ->
                    navController.navigate(Screen.UserProfile.createRoute(userId))
                }) 
            }
            composable(Screen.Profile.route) { 
                ProfileScreen(
                    onSignOut = onSignOut,
                    onInquiryNavigate = { id, name, owner -> 
                        navController.navigate(Screen.InquiryForm.createRoute(id, name, owner)) 
                    },
                    onPayNavigate = { id, price, owner, name ->
                        navController.navigate(Screen.Payment.createRoute(id, price, owner, name))
                    }
                ) 
            }
            
            // Sub-routes
            composable(
                route = Screen.InquiryForm.route,
                arguments = listOf(
                    navArgument("skillId") { type = NavType.StringType },
                    navArgument("skillName") { type = NavType.StringType },
                    navArgument("toUserId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val skillId = backStackEntry.arguments?.getString("skillId") ?: ""
                val skillName = backStackEntry.arguments?.getString("skillName") ?: ""
                val toUserId = backStackEntry.arguments?.getString("toUserId") ?: ""
                InquiryFormScreen(
                    skillId = skillId,
                    skillName = skillName,
                    toUserId = toUserId,
                    onBack = { navController.popBackStack() },
                    onInquirySent = { navController.popBackStack() }
                )
            }
            
            composable(
                route = Screen.UserProfile.route,
                arguments = listOf(navArgument("userId") { type = NavType.StringType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                ProfileScreen(
                    userId = userId,
                    onBack = { navController.popBackStack() },
                    onInquiryNavigate = { id, name, owner -> 
                        navController.navigate(Screen.InquiryForm.createRoute(id, name, owner)) 
                    },
                    onPayNavigate = { id, price, owner, name ->
                        navController.navigate(Screen.Payment.createRoute(id, price, owner, name))
                    }
                )
            }
            
            composable(
                route = Screen.Payment.route,
                arguments = listOf(
                    navArgument("skillId") { type = NavType.StringType },
                    navArgument("price") { type = NavType.StringType },
                    navArgument("creatorId") { type = NavType.StringType },
                    navArgument("skillName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val skillId = backStackEntry.arguments?.getString("skillId") ?: ""
                val price = backStackEntry.arguments?.getString("price") ?: "0.0"
                val creatorId = backStackEntry.arguments?.getString("creatorId") ?: ""
                val skillName = backStackEntry.arguments?.getString("skillName") ?: ""
                
                PaymentScreen(
                    skillId = skillId,
                    price = price.toDoubleOrNull() ?: 0.0,
                    creatorId = creatorId,
                    skillName = skillName,
                    onBack = { navController.popBackStack() },
                    onPurchaseSuccess = { navController.popBackStack() }
                )
            }
        }
    }
}
