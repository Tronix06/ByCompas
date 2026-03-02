package com.bitronix.bycompas.ui.screens.home

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(navController: NavController) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    // Estados para el formulario
    var sport by remember { mutableStateOf("") }
    var level by remember { mutableStateOf("Principiante") }
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var slots by remember { mutableStateOf("4") }
    var notes by remember { mutableStateOf("") }
    var expandedSport by remember { mutableStateOf(false) }
    var isPublishing by remember { mutableStateOf(false) }

    val sportsOptions = listOf("⚽ Fútbol", "🎾 Pádel", "🏃‍♂️ Running", "🏀 Baloncesto")
    val levelsOptions = listOf("Principiante", "Intermedio", "Avanzado")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear Nuevo Evento") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Detalles del partido", fontSize = 20.sp, fontWeight = FontWeight.Bold)

            // Selector de Deporte (Dropdown corregido 2026)
            ExposedDropdownMenuBox(
                expanded = expandedSport,
                onExpandedChange = { expandedSport = !expandedSport }
            ) {
                OutlinedTextField(
                    value = sport,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Selecciona Deporte") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSport) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                )
                ExposedDropdownMenu(
                    expanded = expandedSport,
                    onDismissRequest = { expandedSport = false }
                ) {
                    sportsOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                sport = option
                                expandedSport = false
                            }
                        )
                    }
                }
            }

            // Nivel
            Text("Nivel requerido", fontWeight = FontWeight.Medium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                levelsOptions.forEach { option ->
                    FilterChip(
                        selected = level == option,
                        onClick = { level = option },
                        label = { Text(option) }
                    )
                }
            }

            // Fecha y Hora (Simples TextFields por ahora)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Fecha (DD/MM)") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("Hora (HH:MM)") },
                    modifier = Modifier.weight(1f)
                )
            }

            // Plazas disponibles
            OutlinedTextField(
                value = slots,
                onValueChange = { slots = it },
                label = { Text("Plazas totales") },
                modifier = Modifier.fillMaxWidth()
            )

            // Notas
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notas adicionales (ej. Pista 4)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(modifier = Modifier.weight(1f))

            // Botón de Publicar
            Button(
                onClick = {
                    val currentUser = auth.currentUser
                    if (currentUser != null && sport.isNotEmpty() && date.isNotEmpty()) {
                        isPublishing = true

                        // Creamos el objeto del evento
                        val eventMap = hashMapOf(
                            "sport" to sport,
                            "level" to level,
                            "date" to date,
                            "time" to time,
                            "availableSlots" to (slots.toIntOrNull() ?: 0),
                            "notes" to notes,
                            "creatorId" to currentUser.uid,
                            "status" to "open",
                            "location" to GeoPoint(40.41, -3.70), // Ubicación por defecto (Madrid)
                            "participants" to listOf(currentUser.uid),
                            "pendingRequests" to emptyList<String>()
                        )

                        db.collection("events")
                            .add(eventMap)
                            .addOnSuccessListener {
                                Toast.makeText(context, "¡Evento publicado!", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            }
                            .addOnFailureListener {
                                isPublishing = false
                                Toast.makeText(context, "Error al publicar", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(context, "Rellena los campos básicos", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !isPublishing
            ) {
                if (isPublishing) CircularProgressIndicator(color = Color.White)
                else Text("Publicar Partido", fontSize = 18.sp)
            }
        }
    }
}