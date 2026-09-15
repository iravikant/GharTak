package com.ghartak.app.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import com.ghartak.app.domain.model.DeliveryStatus

private val TIMELINE_STEPS = listOf(
    DeliveryStatus.CREATED to "Delivery booked",
    DeliveryStatus.RIDER_ASSIGNED to "Rider assigned",
    DeliveryStatus.ARRIVED_AT_PICKUP to "Rider reached pickup",
    DeliveryStatus.PARCEL_PICKED_UP to "Parcel picked up",
    DeliveryStatus.OUT_FOR_DELIVERY to "Out for delivery",
    DeliveryStatus.DELIVERED to "Delivered"
)

/**
 * Renders the customer-facing checklist described in spec section 7:
 * ✓ Delivery booked / ✓ Rider assigned / ... / ● Out for delivery / ○ Delivered
 */
@Composable
fun DeliveryStatusTimeline(currentStatus: DeliveryStatus, modifier: Modifier = Modifier) {
    val currentIndex = TIMELINE_STEPS.indexOfFirst { it.first == currentStatus }
        .let { if (it == -1) 0 else it }

    Column(modifier = modifier) {
        TIMELINE_STEPS.forEachIndexed { index, (_, label) ->
            val isDone = index < currentIndex
            val isCurrent = index == currentIndex
            Row(modifier = Modifier.padding(vertical = 6.dp)) {
                val dotColor = when {
                    isDone -> MaterialTheme.colorScheme.primary
                    isCurrent -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.outlineVariant
                }
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = label,
                    style = if (isCurrent) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                    color = if (isDone || isCurrent) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
