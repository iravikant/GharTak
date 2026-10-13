package com.ghartak.app.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ghartak.app.domain.model.Delivery
import com.ghartak.app.domain.model.DeliveryStatus
import com.ghartak.app.domain.model.ParcelType
import com.ghartak.app.presentation.components.Avatar
import com.ghartak.app.presentation.components.BannerType
import com.ghartak.app.presentation.components.EmptyState
import com.ghartak.app.presentation.components.InfoBanner
import com.ghartak.app.presentation.components.MainScaffold
import com.ghartak.app.presentation.components.MainTab
import com.ghartak.app.presentation.components.ParcelCard
import com.ghartak.app.presentation.components.PrimaryButton
import com.ghartak.app.presentation.components.RouteLine
import com.ghartak.app.presentation.components.SkeletonBox
import com.ghartak.app.presentation.components.SystemBarsStyle
import com.ghartak.app.presentation.components.icon
import com.ghartak.app.presentation.components.label
import com.ghartak.app.presentation.components.negativeVerticalOffset
import com.ghartak.app.presentation.theme.BrandGradient
import com.ghartak.app.presentation.theme.PromoGradient
import java.util.Calendar

@Composable
fun HomeScreen(
    onSendParcel: () -> Unit,
    onServiceSelected: (ParcelType) -> Unit,
    onSeeAllServices: () -> Unit,
    onOpenDelivery: (String) -> Unit,
    onOpenNotifications: () -> Unit,
    onTabSelected: (MainTab) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    SystemBarsStyle(darkIcons = false)
    val state by viewModel.uiState.collectAsState()

    MainScaffold(selected = MainTab.HOME, onTabSelected = onTabSelected) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 24.dp)
        ) {
            item {
                HomeHeader(userName = state.userName, onNotifications = onOpenNotifications)
            }

            item {
                SendParcelCard(
                    onClick = onSendParcel,
                    modifier = Modifier
                        .negativeVerticalOffset(36.dp)
                        .padding(horizontal = 20.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 8.dp, top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Services", style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = onSeeAllServices) { Text("See all") }
                }
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    items(ParcelType.entries.toList()) { type ->
                        ServiceTile(type = type, onClick = { onServiceSelected(type) })
                    }
                }
            }

            state.activeDelivery?.let { active ->
                item { SectionTitle("Active delivery", modifier = Modifier.padding(top = 28.dp)) }
                item {
                    ActiveDeliveryCard(
                        delivery = active,
                        onClick = { onOpenDelivery(active.id) },
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp)
                    )
                }
            }

            item { PromoBanner(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 28.dp)) }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 8.dp, top = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Recent deliveries", style = MaterialTheme.typography.titleLarge)
                    if (state.recentDeliveries.isNotEmpty()) {
                        TextButton(onClick = { onTabSelected(MainTab.ORDERS) }) { Text("See all") }
                    }
                }
            }

            when {
                state.isLoading -> items(2) {
                    SkeletonBox(
                        modifier = Modifier
                            .padding(start = 20.dp, end = 20.dp, top = 12.dp)
                            .fillMaxWidth()
                            .height(132.dp),
                        shape = RoundedCornerShape(20.dp)
                    )
                }

                state.errorMessage != null -> item {
                    Column(
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        InfoBanner(message = state.errorMessage.orEmpty(), type = BannerType.ERROR)
                        TextButton(onClick = viewModel::loadHome) { Text("Try again") }
                    }
                }

                state.recentDeliveries.isEmpty() && state.activeDelivery == null -> item {
                    EmptyState(
                        icon = Icons.Filled.Inventory2,
                        title = "No deliveries yet",
                        subtitle = "Parcels you send will show up here so you can track and re-order them.",
                        actionLabel = "Send your first parcel",
                        onAction = onSendParcel
                    )
                }

                else -> items(state.recentDeliveries.take(3), key = { it.id }) { delivery ->
                    ParcelCard(
                        delivery = delivery,
                        onClick = { onOpenDelivery(delivery.id) },
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(userName: String, onNotifications: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(BrandGradient)
            .statusBarsPadding()
            .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 64.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(name = userName, photoUrl = null, size = 46.dp, onGradient = true)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = greeting(),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
            Text(
                text = userName.substringBefore(' ').ifBlank { "there" },
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
        }
        IconButton(onClick = onNotifications) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = "Notifications",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun SendParcelCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text("Where are we delivering?", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(14.dp))
            LocationField(
                dotColor = MaterialTheme.colorScheme.primary,
                text = "Pickup location",
                onClick = onClick
            )
            Spacer(Modifier.height(10.dp))
            LocationField(
                dotColor = MaterialTheme.colorScheme.secondary,
                text = "Drop location",
                onClick = onClick
            )
            Spacer(Modifier.height(18.dp))
            PrimaryButton(text = "Send a parcel", icon = Icons.Filled.LocalShipping, onClick = onClick)
        }
    }
}

@Composable
private fun LocationField(dotColor: Color, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        modifier = modifier.padding(horizontal = 20.dp)
    )
}

@Composable
private fun ServiceTile(type: ParcelType, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(86.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = type.icon(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = type.displayName,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun ActiveDeliveryCard(
    delivery: Delivery,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(BrandGradient)
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Live now",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.75f)
                )
                Text(
                    text = delivery.status.label(),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
            }
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Track delivery",
                    tint = Color.White
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.25f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(delivery.status.progress())
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }

        Spacer(Modifier.height(16.dp))

        RouteLine(
            pickup = delivery.pickup.formattedAddress,
            drop = delivery.drop.formattedAddress,
            textColor = Color.White,
            pickupDotColor = Color.White,
            dropDotColor = Color(0xFFFBBF24),
            lineColor = Color.White.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun PromoBanner(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(PromoGradient)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "₹50 off your first delivery",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Use code YELO50 at checkout",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
        Icon(
            imageVector = Icons.Filled.LocalOffer,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.size(44.dp)
        )
    }
}

private fun DeliveryStatus.progress(): Float = when (this) {
    DeliveryStatus.CREATED, DeliveryStatus.SEARCHING_RIDER -> 0.1f
    DeliveryStatus.RIDER_ASSIGNED -> 0.25f
    DeliveryStatus.RIDER_GOING_TO_PICKUP -> 0.4f
    DeliveryStatus.ARRIVED_AT_PICKUP -> 0.55f
    DeliveryStatus.PARCEL_PICKED_UP -> 0.7f
    DeliveryStatus.OUT_FOR_DELIVERY -> 0.85f
    DeliveryStatus.RIDER_NEAR_DESTINATION -> 0.95f
    else -> 1f
}

private fun greeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
}
