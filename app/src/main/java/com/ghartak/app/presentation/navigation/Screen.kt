package com.ghartak.app.presentation.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Onboarding : Screen("onboarding")
    data object Auth : Screen("auth")
    data object Home : Screen("home")
    data object Orders : Screen("orders")
    data object Profile : Screen("profile")
    data object EditProfile : Screen("edit_profile")
    data object Services : Screen("services")

    /** Nested graph that holds the booking steps so they can share one BookingViewModel. */
    data object BookingGraph : Screen("booking")

    data object BookingRoute : Screen("booking_route?type={type}") {
        const val ARG_TYPE = "type"
        fun createRoute(parcelTypeName: String? = null) =
            if (parcelTypeName == null) "booking_route" else "booking_route?type=$parcelTypeName"
    }

    data object BookingDetails : Screen("booking_details")
    data object BookingSummary : Screen("booking_summary")

    data object BookingPicker : Screen("booking_picker/{target}") {
        const val ARG_TARGET = "target"
        const val TARGET_PICKUP = "pickup"
        const val TARGET_DROP = "drop"
        fun createRoute(target: String) = "booking_picker/$target"
    }

    data object DeliveryDetails : Screen("delivery/{deliveryId}") {
        const val ARG_ID = "deliveryId"
        fun createRoute(deliveryId: String) = "delivery/$deliveryId"
    }

    data object SavedAddresses : Screen("saved_addresses") {
        // Keys used to hand the picked location back from the map picker.
        const val KEY_LAT = "picked_lat"
        const val KEY_LNG = "picked_lng"
        const val KEY_TEXT = "picked_text"
    }

    data object SavedAddressPicker : Screen("saved_address_picker")

    // ---- rider mode ----
    data object RiderHome : Screen("rider_home")
    data object RiderRegister : Screen("rider_register")
    data object RiderDelivery : Screen("rider_delivery/{deliveryId}") {
        const val ARG_ID = "deliveryId"
        fun createRoute(deliveryId: String) = "rider_delivery/$deliveryId"
    }

    /** Placeholder destination for flows that aren't built yet (see FEATURE_* keys). */
    data object ComingSoon : Screen("coming_soon/{feature}") {
        const val ARG_FEATURE = "feature"
        const val FEATURE_NOTIFICATIONS = "notifications"
        const val FEATURE_HELP = "help"

        fun createRoute(feature: String) = "coming_soon/$feature"
    }
}
