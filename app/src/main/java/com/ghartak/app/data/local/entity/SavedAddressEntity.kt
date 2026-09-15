package com.ghartak.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_addresses")
data class SavedAddressEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val formattedAddress: String,
    val lat: Double,
    val lng: Double
)
