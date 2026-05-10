package com.bitronix.bycompa.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val sport: String,
    val level: String,
    val date: String,
    val time: String,
    val latitude: Double,
    val longitude: Double,
    val availableSlots: Int,
    val notes: String,
    val creatorId: String,
    val participants: String, // Stored as a comma-separated string or via converter
    val pendingRequests: String,
    val isRecommended: Boolean = false // To differentiate from joined events
)
