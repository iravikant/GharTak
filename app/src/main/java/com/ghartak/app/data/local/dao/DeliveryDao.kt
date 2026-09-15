package com.ghartak.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ghartak.app.data.local.entity.DeliveryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeliveryDao {
    @Upsert
    suspend fun upsertAll(deliveries: List<DeliveryEntity>)

    @Upsert
    suspend fun upsert(delivery: DeliveryEntity)

    @Query("SELECT * FROM deliveries ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DeliveryEntity>>

    @Query("SELECT * FROM deliveries WHERE id = :deliveryId")
    suspend fun getById(deliveryId: String): DeliveryEntity?

    @Query("DELETE FROM deliveries WHERE status IN ('DELIVERED','CANCELLED','FAILED') AND createdAt < :beforeTimestamp")
    suspend fun pruneOldTerminalDeliveries(beforeTimestamp: Long)
}
