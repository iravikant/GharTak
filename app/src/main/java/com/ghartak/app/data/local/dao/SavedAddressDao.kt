package com.ghartak.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.ghartak.app.data.local.entity.SavedAddressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedAddressDao {
    @Insert
    suspend fun insert(address: SavedAddressEntity): Long

    @Delete
    suspend fun delete(address: SavedAddressEntity)

    @Query("SELECT * FROM saved_addresses ORDER BY id DESC")
    fun observeAll(): Flow<List<SavedAddressEntity>>
}
