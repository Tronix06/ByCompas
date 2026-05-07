package com.bitronix.bycompas.models

import java.util.Date

data class Message(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Date? = null,
    val edited: Boolean = false,
    val readAt: Date? = null,
    val likes: List<String> = emptyList(),
    val favorites: List<String> = emptyList(),
    val deletedFor: List<String> = emptyList(),
    val deletedForEveryone: Boolean = false
)
