package com.ghartak.app.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ghartak.app.domain.model.Delivery

/** Summary card used on Home for the active delivery and in delivery history lists. */
@Composable
fun ParcelCard(delivery: Delivery, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = delivery.pickup.formattedAddress,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )
            }
            Text(
                text = "→ ${delivery.drop.formattedAddress}",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(
                    text = delivery.status.name.replace('_', ' '),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
