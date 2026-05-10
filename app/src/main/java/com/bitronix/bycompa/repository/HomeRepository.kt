package com.bitronix.bycompa.repository

import com.bitronix.bycompa.database.EventDao
import com.bitronix.bycompa.database.EventEntity
import com.bitronix.bycompa.models.EventData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.tasks.await

class HomeRepository(
    private val eventDao: EventDao,
    private val db: FirebaseFirestore
) {
    suspend fun deleteEvent(eventId: String) {
        // 1. Borrar de Firestore
        db.collection("events").document(eventId).delete().await()
        
        // 2. Borrar chat asociado (Tienen el mismo ID)
        db.collection("chats").document(eventId).delete().await()
        
        // 3. Borrar de caché local
        eventDao.clearEvents(isRecommended = false)
    }

    suspend fun getJoinedEvents(uid: String): List<EventData> {
        // 1. Obtener de caché local
        val local = eventDao.getJoinedEvents().map { it.toEventData() }
        
        // 2. Intentar actualizar desde Firestore
        try {
            val snapshot = db.collection("events")
                .whereArrayContains("participants", uid)
                .get()
                .await()
            
            val remote = snapshot.documents.mapNotNull { 
                it.toObject(EventData::class.java)?.copy(id = it.id)
            }
            
            // Actualizar caché
            val entities = remote.map { it.toEntity(isRecommended = false) }
            eventDao.clearEvents(isRecommended = false)
            eventDao.insertEvents(entities)
            
            return remote
        } catch (e: Exception) {
            // Si falla la red, ya tenemos los datos locales (o lista vacía)
            return local
        }
    }

    suspend fun getRecommendedEvents(favorites: List<String>): List<EventData> {
        // 1. Obtener de caché local
        val local = eventDao.getRecommendedEvents().map { it.toEventData() }
        
        // 2. Intentar actualizar desde Firestore
        try {
            if (favorites.isEmpty()) return local
            
            val snapshot = db.collection("events")
                .whereIn("sport", favorites)
                .limit(10)
                .get()
                .await()
            
            val remote = snapshot.documents.mapNotNull { 
                it.toObject(EventData::class.java)?.copy(id = it.id)
            }.filter { it.participants.size < it.availableSlots } // Filtrar por plazas libres
            
            // Actualizar caché
            val entities = remote.map { it.toEntity(isRecommended = true) }
            eventDao.clearEvents(isRecommended = true)
            eventDao.insertEvents(entities)
            
            return remote
        } catch (e: Exception) {
            return local
        }
    }

    // Funciones de extensión para mapeo
    private fun EventData.toEntity(isRecommended: Boolean): EventEntity {
        return EventEntity(
            id = id,
            title = title,
            sport = sport,
            level = level,
            date = date,
            time = time,
            latitude = location.latitude,
            longitude = location.longitude,
            availableSlots = availableSlots,
            notes = notes,
            creatorId = creatorId,
            participants = participants.joinToString(","),
            pendingRequests = pendingRequests.joinToString(","),
            isRecommended = isRecommended
        )
    }

    private fun EventEntity.toEventData(): EventData {
        return EventData(
            id = id,
            title = title,
            sport = sport,
            level = level,
            date = date,
            time = time,
            location = GeoPoint(latitude, longitude),
            availableSlots = availableSlots,
            notes = notes,
            creatorId = creatorId,
            participants = participants.split(",").filter { it.isNotEmpty() },
            pendingRequests = pendingRequests.split(",").filter { it.isNotEmpty() }
        )
    }
}
