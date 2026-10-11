package com.ghartak.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ghartak.app.domain.model.DeliveryStatus

private val TIMELINE_LABELS = listOf(
    "Delivery booked",
    "Rider assigned",
    "Rider reached pickup",
    "Parcel picked up",
    "Out for delivery",
    "Delivered"
)

/** Maps the 11-state delivery machine onto the 6 customer-facing steps. */
private fun DeliveryStatus.timelineIndex(): Int = when (this) {
    DeliveryStatus.CREATED, DeliveryStatus.SEARCHING_RIDER -> 0
    DeliveryStatus.RIDER_ASSIGNED, DeliveryStatus.RIDER_GOING_TO_PICKUP -> 1
    DeliveryStatus.ARRIVED_AT_PICKUP -> 2
    DeliveryStatus.PARCEL_PICKED_UP -> 3
    DeliveryStatus.OUT_FOR_DELIVERY, DeliveryStatus.RIDER_NEAR_DESTINATION -> 4
    DeliveryStatus.DELIVERED -> 5
    DeliveryStatus.CANCELLED, DeliveryStatus.FAILED -> 0
}

/** Vertical stepper: ✓ completed, ● current, ○ upcoming. */
@Composable
fun DeliveryStatusTimeline(currentStatus: DeliveryStatus, modifier: Modifier = Modifier) {
    val current = currentStatus.timelineIndex()
    val allDone = currentStatus == DeliveryStatus.DELIVERED

    Column(modifier = modifier) {
        TIMELINE_LABELS.forEachIndexed { index, label ->
            val done = index < current || (allDone && index == current)
            val isCurrent = index == current && !allDone
            val isLast = index == TIMELINE_LABELS.lastIndex

            Row(modifier = Modifier.height(44.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(24.dp)) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    done -> MaterialTheme.colorScheme.primary
                                    isCurrent -> MaterialTheme.colorScheme.primaryContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            done -> Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            isCurrent -> Box(
                                Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                    if (!isLast) {
                        Box(
                            Modifier
                                .width(2.dp)
                                .fillMaxHeight()
                                .background(
                                    if (done) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    text = label,
                    style = if (isCurrent) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
                    color = if (done || isCurrent) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
