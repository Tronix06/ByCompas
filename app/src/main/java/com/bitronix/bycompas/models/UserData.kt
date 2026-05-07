package com.bitronix.bycompas.models

data class UserData(
    val uid: String = "",
    val name: String = "",
    val nameLower: String = "",
    val email: String = "",
    val profileImageUrl: String? = null,
    val phone: String? = null,
    val birthDate: String? = null,
    val gender: String? = null,
    val sports: List<String> = emptyList(),
    val medals: List<String> = emptyList(),
    val followers: List<String> = emptyList(),
    val following: List<String> = emptyList(),
    val followRequests: List<String> = emptyList(),
    val rating: Double = 5.0,
    val totalRatingsCount: Int = 0,
    val totalRatingSum: Double = 0.0,
    val ratingCounts: Map<String, Int> = emptyMap(), // Ej: "5" -> 10, "4" -> 2
    val radarRadius: Double = 15.0,
    val xp: Int = 0,
    val level: Int = 1,
    val chatWallpapers: Map<String, String> = emptyMap(),
    val isOnline: Boolean = false,
    val lastSeen: Long = 0L
)
