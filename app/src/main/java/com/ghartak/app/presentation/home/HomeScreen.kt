package com.ghartak.app.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ghartak.app.presentation.components.ErrorView
import com.ghartak.app.presentation.components.ParcelCard

@Composable
fun HomeScreen(
    onSendParcel: () -> Unit,
    onOpenDelivery: (String) -> Unit,
    onOpenNotifications: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Home") },
                actions = {
                    IconButton(onClick = onOpenNotifications) {
                        Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading && uiState.activeDelivery == null && uiState.recentDeliveries.isEmpty() -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.errorMessage != null && uiState.activeDelivery == null && uiState.recentDeliveries.isEmpty() -> {
                ErrorView(
                    message = uiState.errorMessage ?: "Something went wrong",
                    onRetry = viewModel::loadHome,
                    modifier = Modifier.padding(padding)
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(20.dp)
                ) {
                    item {
                        Card(
                            onClick = onSendParcel,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.LocalShipping,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Send a Parcel", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "Book a rider for pickup and delivery",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    uiState.activeDelivery?.let { active ->
                        item {
                            Text("Active delivery", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            ParcelCard(delivery = active, onClick = { onOpenDelivery(active.id) })
                            Spacer(modifier = Modifier.height(20.dp))
                        }
                    }

                    if (uiState.recentDeliveries.isNotEmpty()) {
                        item {
                            Text("Recent deliveries", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        items(uiState.recentDeliveries) { delivery ->
                            ParcelCard(
                                delivery = delivery,
                                onClick = { onOpenDelivery(delivery.id) },
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
