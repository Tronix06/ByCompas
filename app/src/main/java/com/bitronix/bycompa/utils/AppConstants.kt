package com.bitronix.bycompa.utils

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.GeoPoint

object AppConstants {
    // Ubicación por defecto: Puerta del Sol, Madrid
    val DEFAULT_LAT_LNG = LatLng(40.4167, -3.7032)
    val DEFAULT_GEO_POINT = GeoPoint(40.4167, -3.7032)
    
    const val DEFAULT_MAP_ZOOM = 15f
    const val RADAR_MAP_ZOOM = 14f

    val SPORTS_LIST = listOf(
        "Fútbol", "Pádel", "Running", "Baloncesto", "Tenis", "Ciclismo",
        "Natación", "Voleibol", "Gym / Fitness", "Crossfit", "Yoga",
        "Pilates", "Senderismo", "Golf", "Boxeo", "Skate", "Ajedrez"
    )
}
