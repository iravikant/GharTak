package com.ghartak.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ghartak.app.domain.model.Address
import com.ghartak.app.domain.model.LatLngPoint
import com.ghartak.app.presentation.addresses.SavedAddressesScreen
import com.ghartak.app.presentation.auth.AuthScreen
import com.ghartak.app.presentation.booking.BookingDetailsScreen
import com.ghartak.app.presentation.booking.BookingRouteScreen
import com.ghartak.app.presentation.booking.BookingSummaryScreen
import com.ghartak.app.presentation.booking.BookingViewModel
import com.ghartak.app.presentation.components.ComingSoonScreen
import com.ghartak.app.presentation.components.MainTab
import com.ghartak.app.presentation.delivery.DeliveryDetailsScreen
import com.ghartak.app.presentation.home.HomeScreen
import com.ghartak.app.presentation.location.LocationPickerScreen
import com.ghartak.app.presentation.onboarding.OnboardingScreen
import com.ghartak.app.presentation.orders.OrdersScreen
import com.ghartak.app.presentation.profile.EditProfileScreen
import com.ghartak.app.presentation.profile.ProfileScreen
import com.ghartak.app.presentation.rider.RiderDeliveryScreen
import com.ghartak.app.presentation.rider.RiderHomeScreen
import com.ghartak.app.presentation.rider.RiderRegisterScreen
import com.ghartak.app.presentation.services.ServicesScreen
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
    val startBooking: (String?) -> Unit = { parcelTypeName ->
        navController.navigate(Screen.BookingRoute.createRoute(parcelTypeName))
    }
    val openDelivery: (String) -> Unit = { id ->
        navController.navigate(Screen.DeliveryDetails.createRoute(id))
    }
    val openFeature: (String) -> Unit = { feature ->
        navController.navigate(Screen.ComingSoon.createRoute(feature))
    }
    val back: () -> Unit = { navController.popBackStack() }

    NavHost(navController = navController, startDestination = Screen.Splash.route) {

        // ------------------------------------------------------------ entry flow
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

        // ------------------------------------------------------------ main tabs
        composable(Screen.Home.route) {
            HomeScreen(
                onSendParcel = { startBooking(null) },
                onServiceSelected = { type -> startBooking(type.name) },
                onSeeAllServices = { navController.navigate(Screen.Services.route) },
                onOpenDelivery = openDelivery,
                onOpenNotifications = { openFeature(Screen.ComingSoon.FEATURE_NOTIFICATIONS) },
                onTabSelected = openTab
            )
        }

        composable(Screen.Orders.route) {
            OrdersScreen(
                onOpenDelivery = openDelivery,
                onSendParcel = { startBooking(null) },
                onTabSelected = openTab
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onTabSelected = openTab,
                onEditProfile = { navController.navigate(Screen.EditProfile.route) },
                onOpenSavedAddresses = { navController.navigate(Screen.SavedAddresses.route) },
                onOpenRiderMode = { navController.navigate(Screen.RiderHome.route) },
                onOpenFeature = openFeature,
                onLoggedOut = {
                    navController.navigate(Screen.Auth.route) {
                        // Clear the whole back stack so Back can't return to a signed-in screen.
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.EditProfile.route) {
            EditProfileScreen(onBack = back)
        }

        composable(Screen.Services.route) {
            ServicesScreen(
                onBack = back,
                onSelectService = { type -> startBooking(type.name) }
            )
        }

        // ------------------------------------------------------------ booking (shared ViewModel)
        navigation(startDestination = Screen.BookingRoute.route, route = Screen.BookingGraph.route) {

            composable(
                route = Screen.BookingRoute.route,
                arguments = listOf(
                    navArgument(Screen.BookingRoute.ARG_TYPE) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { entry ->
                BookingRouteScreen(
                    viewModel = bookingViewModel(navController, entry),
                    initialType = entry.arguments?.getString(Screen.BookingRoute.ARG_TYPE),
                    onPickPickup = {
                        navController.navigate(Screen.BookingPicker.createRoute(Screen.BookingPicker.TARGET_PICKUP))
                    },
                    onPickDrop = {
                        navController.navigate(Screen.BookingPicker.createRoute(Screen.BookingPicker.TARGET_DROP))
                    },
                    onContinue = { navController.navigate(Screen.BookingDetails.route) },
                    onBack = back
                )
            }

            composable(Screen.BookingDetails.route) { entry ->
                BookingDetailsScreen(
                    viewModel = bookingViewModel(navController, entry),
                    onContinue = { navController.navigate(Screen.BookingSummary.route) },
                    onBack = back
                )
            }

            composable(Screen.BookingSummary.route) { entry ->
                BookingSummaryScreen(
                    viewModel = bookingViewModel(navController, entry),
                    onBooked = { deliveryId ->
                        navController.navigate(Screen.DeliveryDetails.createRoute(deliveryId)) {
                            // Leave the booking steps behind: Back from the receipt goes to Home.
                            popUpTo(Screen.BookingGraph.route) { inclusive = true }
                        }
                    },
                    onBack = back
                )
            }

            composable(
                route = Screen.BookingPicker.route,
                arguments = listOf(navArgument(Screen.BookingPicker.ARG_TARGET) { type = NavType.StringType })
            ) { entry ->
                val booking = bookingViewModel(navController, entry)
                val state by booking.uiState.collectAsState()
                val isPickup = entry.arguments?.getString(Screen.BookingPicker.ARG_TARGET) ==
                    Screen.BookingPicker.TARGET_PICKUP
                val existing = if (isPickup) state.pickup else state.drop

                LocationPickerScreen(
                    title = if (isPickup) "Pickup location" else "Drop location",
                    initialPoint = existing?.point,
                    confirmLabel = if (isPickup) "Confirm pickup" else "Confirm drop",
                    onConfirm = { address ->
                        if (isPickup) booking.setPickup(address) else booking.setDrop(address)
                        navController.popBackStack()
                    },
                    onBack = back
                )
            }
        }

        composable(
            route = Screen.DeliveryDetails.route,
            arguments = listOf(navArgument(Screen.DeliveryDetails.ARG_ID) { type = NavType.StringType })
        ) {
            DeliveryDetailsScreen(onBack = back)
        }

        // ------------------------------------------------------------ saved addresses
        composable(Screen.SavedAddresses.route) { entry ->
            val handle = entry.savedStateHandle
            val lat by handle.getStateFlow<Double?>(Screen.SavedAddresses.KEY_LAT, null).collectAsState()
            val lng by handle.getStateFlow<Double?>(Screen.SavedAddresses.KEY_LNG, null).collectAsState()
            val text by handle.getStateFlow<String?>(Screen.SavedAddresses.KEY_TEXT, null).collectAsState()

            val pickedLat = lat
            val pickedLng = lng
            val pickedText = text
            val picked = if (pickedLat != null && pickedLng != null && pickedText != null) {
                Address(LatLngPoint(pickedLat, pickedLng), pickedText)
            } else null

            SavedAddressesScreen(
                pickedAddress = picked,
                onPickedConsumed = {
                    // Set to null (don't remove): remove() detaches the StateFlow the dialog observes,
                    // so the UI would never see the change and the dialog would stay open.
                    handle.set<Double?>(Screen.SavedAddresses.KEY_LAT, null)
                    handle.set<Double?>(Screen.SavedAddresses.KEY_LNG, null)
                    handle.set<String?>(Screen.SavedAddresses.KEY_TEXT, null)
                },
                onAddAddress = { navController.navigate(Screen.SavedAddressPicker.route) },
                onBack = back
            )
        }

        composable(Screen.SavedAddressPicker.route) {
            LocationPickerScreen(
                title = "Address to save",
                initialPoint = null,
                confirmLabel = "Confirm location",
                onConfirm = { address ->
                    navController.previousBackStackEntry?.savedStateHandle?.apply {
                        set(Screen.SavedAddresses.KEY_LAT, address.point.lat)
                        set(Screen.SavedAddresses.KEY_LNG, address.point.lng)
                        set(Screen.SavedAddresses.KEY_TEXT, address.formattedAddress)
                    }
                    navController.popBackStack()
                },
                onBack = back
            )
        }

        // ------------------------------------------------------------ rider mode
        composable(Screen.RiderHome.route) {
            RiderHomeScreen(
                onExitRiderMode = back,
                onRegister = { navController.navigate(Screen.RiderRegister.route) },
                onOpenDelivery = { id -> navController.navigate(Screen.RiderDelivery.createRoute(id)) }
            )
        }

        composable(Screen.RiderRegister.route) {
            RiderRegisterScreen(onBack = back, onDone = back)
        }

        composable(
            route = Screen.RiderDelivery.route,
            arguments = listOf(navArgument(Screen.RiderDelivery.ARG_ID) { type = NavType.StringType })
        ) {
            RiderDeliveryScreen(onBack = back)
        }

        // ------------------------------------------------------------ placeholders
        composable(
            route = Screen.ComingSoon.route,
            arguments = listOf(navArgument(Screen.ComingSoon.ARG_FEATURE) { type = NavType.StringType })
        ) { entry ->
            val feature = entry.arguments?.getString(Screen.ComingSoon.ARG_FEATURE).orEmpty()
            val (title, message) = when (feature) {
                Screen.ComingSoon.FEATURE_NOTIFICATIONS ->
                    "Notifications" to "Delivery updates and offers will show up here."
                Screen.ComingSoon.FEATURE_HELP ->
                    "Help & support" to "Chat with us and report delivery issues from here."
                else -> "GharTak" to "This part of the app is on its way."
            }
            ComingSoonScreen(title = title, message = message, onBack = back)
        }
    }
}

/** The BookingViewModel lives on the "booking" graph entry, so every step sees the same instance. */
@Composable
private fun bookingViewModel(navController: NavHostController, entry: NavBackStackEntry): BookingViewModel {
    val graphEntry = remember(entry) { navController.getBackStackEntry(Screen.BookingGraph.route) }
    return hiltViewModel(graphEntry)
}
