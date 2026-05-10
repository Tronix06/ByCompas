package com.bitronix.bycompa.models

import com.google.firebase.Timestamp

data class UserReview(
    val reviewerId: String = "",
    val reviewerName: String = "Usuario",
    val reviewerImageUrl: String? = null,
    val rating: Double = 5.0,
    val comment: String = "",
    val date: Timestamp? = null
)
