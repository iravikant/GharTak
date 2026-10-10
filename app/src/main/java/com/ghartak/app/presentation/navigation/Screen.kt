package com.ghartak.app.presentation.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Onboarding : Screen("onboarding")
    data object Auth : Screen("auth")
    data object Home : Screen("home")
    data object Orders : Screen("orders")
    data object Profile : Screen("profile")

    /** Placeholder destination for flows that aren't built yet (see FEATURE_* keys). */
    data object ComingSoon : Screen("coming_soon/{feature}") {
        const val ARG_FEATURE = "feature"
        const val FEATURE_SEND_PARCEL = "send_parcel"
        const val FEATURE_TRACKING = "tracking"
        const val FEATURE_NOTIFICATIONS = "notifications"
        const val FEATURE_SAVED_ADDRESSES = "saved_addresses"
        const val FEATURE_HELP = "help"

        fun createRoute(feature: String) = "coming_soon/$feature"
    }
}
