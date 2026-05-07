package com.bitronix.bycompas.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface EventDao {
    @Query("SELECT * FROM events WHERE isRecommended = 0 ORDER BY date ASC, time ASC")
    suspend fun getJoinedEvents(): List<EventEntity>

    @Query("SELECT * FROM events WHERE isRecommended = 1")
    suspend fun getRecommendedEvents(): List<EventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<EventEntity>)

    @Query("DELETE FROM events WHERE isRecommended = :isRecommended")
    suspend fun clearEvents(isRecommended: Boolean)
}
