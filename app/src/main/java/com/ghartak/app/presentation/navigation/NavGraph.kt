package com.ghartak.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ghartak.app.presentation.auth.LoginScreen
import com.ghartak.app.presentation.auth.RegisterScreen
import com.ghartak.app.presentation.home.HomeScreen
import com.ghartak.app.presentation.onboarding.OnboardingScreen
import com.ghartak.app.presentation.splash.SplashScreen

/**
 * Customer-flow navigation graph (spec section 3). Delivery creation, tracking, history, and
 * profile screens are stubbed as routes here and get their own composables as those flows are
 * built out; a separate graph would host the rider-app screens (spec section 22).
 */
@Composable
fun GharTakNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Screen.Splash.route) {

        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToOnboarding = {
                    navController.navigate(Screen.Onboarding.route) {
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
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onSendParcel = { navController.navigate(Screen.CreateDelivery.route) },
                onOpenDelivery = { deliveryId ->
                    navController.navigate(Screen.LiveTracking.createRoute(deliveryId))
                },
                onOpenNotifications = { /* TODO: notifications screen */ }
            )
        }

        // TODO: CreateDelivery, PriceSummary, LiveTracking, DeliveryHistory, Profile screens.
        // Routes are already declared in Screen.kt so navigation calls above compile against
        // their final destinations as each screen is implemented.
    }
}
