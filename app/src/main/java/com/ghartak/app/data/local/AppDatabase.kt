package com.ghartak.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ghartak.app.data.local.dao.DeliveryDao
import com.ghartak.app.data.local.dao.SavedAddressDao
import com.ghartak.app.data.local.entity.DeliveryEntity
import com.ghartak.app.data.local.entity.SavedAddressEntity

@Database(
    entities = [DeliveryEntity::class, SavedAddressEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deliveryDao(): DeliveryDao
    abstract fun savedAddressDao(): SavedAddressDao

    companion object {
        const val DATABASE_NAME = "ghartak.db"
    }
}
