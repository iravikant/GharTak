package com.ghartak.app.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PriceBreakdown(
    pickupDistanceKm: Double,
    deliveryDistanceKm: Double,
    estimatedMinutes: Int,
    deliveryFee: Double,
    platformFee: Double,
    modifier: Modifier = Modifier
) {
    val total = deliveryFee + platformFee
    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        PriceRow("Pickup distance", "%.1f km".format(pickupDistanceKm))
        PriceRow("Delivery distance", "%.1f km".format(deliveryDistanceKm))
        PriceRow("Estimated time", "$estimatedMinutes min")
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        PriceRow("Delivery fee", "₹%.0f".format(deliveryFee))
        PriceRow("Platform fee", "₹%.0f".format(platformFee))
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        PriceRow("Total amount", "₹%.0f".format(total), emphasize = true)
    }
}

@Composable
private fun PriceRow(label: String, value: String, emphasize: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Normal
        )
    }
}
