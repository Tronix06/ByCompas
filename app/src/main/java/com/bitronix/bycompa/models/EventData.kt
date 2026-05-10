package com.bitronix.bycompa.models

import com.google.firebase.firestore.GeoPoint

data class EventData(
    val id: String = "",
    val title: String = "",
    val sport: String = "",
    val level: String = "",
    val date: String = "",
    val time: String = "",
    val location: GeoPoint = GeoPoint(0.0, 0.0),
    val availableSlots: Int = 0,
    val occupiedSlots: Int = 0,
    val totalSlots: Int = 0,
    val creationMode: String = "Solo yo",
    val friendCount: Int = 0,
    val notes: String = "",
    val creatorId: String = "",
    val participants: List<String> = emptyList(),
    val pendingRequests: List<String> = emptyList()
)
