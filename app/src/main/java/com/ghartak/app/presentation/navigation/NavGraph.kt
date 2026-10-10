package com.ghartak.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ghartak.app.presentation.auth.AuthScreen
import com.ghartak.app.presentation.components.ComingSoonScreen
import com.ghartak.app.presentation.components.MainTab
import com.ghartak.app.presentation.home.HomeScreen
import com.ghartak.app.presentation.onboarding.OnboardingScreen
import com.ghartak.app.presentation.orders.OrdersScreen
import com.ghartak.app.presentation.profile.ProfileScreen
import com.ghartak.app.presentation.splash.SplashScreen

@Composable
fun GharTakNavHost(navController: NavHostController = rememberNavController()) {

    val openTab: (MainTab) -> Unit = { tab ->
        val route = when (tab) {
            MainTab.HOME -> Screen.Home.route
            MainTab.ORDERS -> Screen.Orders.route
            MainTab.PROFILE -> Screen.Profile.route
        }
        navController.navigate(route) {
            popUpTo(Screen.Home.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    val openFeature: (String) -> Unit = { feature ->
        navController.navigate(Screen.ComingSoon.createRoute(feature))
    }

    NavHost(navController = navController, startDestination = Screen.Splash.route) {

        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToOnboarding = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToAuth = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Auth.route) {
            AuthScreen(
                onAuthSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onSendParcel = { openFeature(Screen.ComingSoon.FEATURE_SEND_PARCEL) },
                onOpenDelivery = { openFeature(Screen.ComingSoon.FEATURE_TRACKING) },
                onOpenNotifications = { openFeature(Screen.ComingSoon.FEATURE_NOTIFICATIONS) },
                onTabSelected = openTab
            )
        }

        composable(Screen.Orders.route) {
            OrdersScreen(
                onOpenDelivery = { openFeature(Screen.ComingSoon.FEATURE_TRACKING) },
                onSendParcel = { openFeature(Screen.ComingSoon.FEATURE_SEND_PARCEL) },
                onTabSelected = openTab
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onTabSelected = openTab,
                onOpenFeature = openFeature,
                onLoggedOut = {
                    navController.navigate(Screen.Auth.route) {
                        // Clear the whole back stack so Back can't return to a signed-in screen.
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.ComingSoon.route,
            arguments = listOf(navArgument(Screen.ComingSoon.ARG_FEATURE) { type = NavType.StringType })
        ) { entry ->
            val feature = entry.arguments?.getString(Screen.ComingSoon.ARG_FEATURE).orEmpty()
            val (title, message) = when (feature) {
                Screen.ComingSoon.FEATURE_SEND_PARCEL ->
                    "Send a parcel" to "Booking a delivery — pickup, drop, parcel details and price — is the next screen we're building."
                Screen.ComingSoon.FEATURE_TRACKING ->
                    "Track delivery" to "Live rider tracking on the map will appear here."
                Screen.ComingSoon.FEATURE_NOTIFICATIONS ->
                    "Notifications" to "Delivery updates and offers will show up here."
                Screen.ComingSoon.FEATURE_SAVED_ADDRESSES ->
                    "Saved addresses" to "Save Home, Office and other places for one-tap booking."
                Screen.ComingSoon.FEATURE_HELP ->
                    "Help & support" to "Chat with us and report delivery issues from here."
                else -> "GharTak" to "This part of the app is on its way."
            }
            ComingSoonScreen(title = title, message = message, onBack = { navController.popBackStack() })
        }
    }
}
