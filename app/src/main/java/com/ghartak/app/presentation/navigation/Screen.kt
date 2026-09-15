package com.ghartak.app.presentation.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Onboarding : Screen("onboarding")
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Home : Screen("home")
    data object CreateDelivery : Screen("create_delivery")
    data object PriceSummary : Screen("price_summary/{deliveryId}") {
        fun createRoute(deliveryId: String) = "price_summary/$deliveryId"
    }
    data object LiveTracking : Screen("live_tracking/{deliveryId}") {
        fun createRoute(deliveryId: String) = "live_tracking/$deliveryId"
    }
    data object DeliveryHistory : Screen("delivery_history")
    data object Profile : Screen("profile")
}
