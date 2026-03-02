package com.bitronix.bycompas.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun InicioScreen() {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser

    var userName by remember { mutableStateOf("...") }
    var upcomingEvents by remember { mutableStateOf<List<EventData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Cargar datos del usuario y sus eventos vinculados
    LaunchedEffect(currentUser) {
        currentUser?.uid?.let { uid ->
            // 1. Obtener nombre del perfil
            db.collection("users").document(uid).get().addOnSuccessListener { doc ->
                userName = doc.getString("name") ?: "Usuario"
            }

            // 2. Escuchar eventos donde el usuario es participante (Tiempo Real)
            db.collection("events")
                .whereArrayContains("participants", uid)
                .addSnapshotListener { snap, _ ->
                    upcomingEvents = snap?.documents?.mapNotNull {
                        it.toObject(EventData::class.java)?.copy(id = it.id)
                    } ?: emptyList()
                    isLoading = false
                }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- CABECERA PERSONALIZADA ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "¡Hola, $userName! 👋",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Tu agenda de eventos para hoy",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                }
                IconButton(onClick = { /* TODO: Notificaciones */ }) {
                    Icon(Icons.Default.Notifications, contentDescription = "Notificaciones")
                }
            }
        }

        // --- ESTADO DE VERIFICACIÓN (Punto clave para el TFG) ---
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Sello de Confianza", fontWeight = FontWeight.Bold)
                        Text("Verifica tu cuenta para desbloquear todas las funciones.", fontSize = 12.sp)
                    }
                }
            }
        }

        // --- SECCIÓN: MIS PRÓXIMOS EVENTOS ---
        item {
            Text(
                text = "Mis próximos eventos",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (isLoading) {
            item {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                }
            }
        } else if (upcomingEvents.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.LightGray.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("No estás inscrito en ningún evento.", color = Color.Gray)
                    Text("¡Explora el Radar para unirte a uno!", fontSize = 12.sp, color = Color.Gray)
                }
            }
        } else {
            // Listado dinámico de tarjetas de evento
            items(upcomingEvents) { event ->
                EventSummaryCard(event)
            }
        }
    }
}

@Composable
fun EventSummaryCard(event: EventData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono del Evento
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Lógica de iconos según el deporte/tipo de evento
                val icon = when {
                    event.sport.contains("⚽") -> "⚽"
                    event.sport.contains("🎾") -> "🎾"
                    event.sport.contains("🏃") -> "🏃"
                    event.sport.contains("🏀") -> "🏀"
                    else -> "🏆"
                }
                Text(icon, fontSize = 24.sp)
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.sport,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${event.date} • ${event.time}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            // Badge de estado (Confirmado)
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Confirmado",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}