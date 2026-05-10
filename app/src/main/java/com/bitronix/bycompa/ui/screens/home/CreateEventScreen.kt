package com.bitronix.bycompa.ui.screens.home

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.LocationOn
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState
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
    var showMapDialog by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(40.41, -3.70), 12f)
    }

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
            Text("Detalles del evento", fontSize = 20.sp, fontWeight = FontWeight.Bold)

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

            // Selector de Ubicación fluido mediante ventana (Dialog)
            Text("Ubicación del evento", fontWeight = FontWeight.Medium)
            
            OutlinedButton(
                onClick = { showMapDialog = true },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Red)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Seleccionar ubicación en el mapa")
            }

            if (showMapDialog) {
                Dialog(onDismissRequest = { showMapDialog = false }) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(450.dp)
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                GoogleMap(
                                    modifier = Modifier.fillMaxSize(),
                                    cameraPositionState = cameraPositionState
                                )
                                // Chincheta flotante estática en el centro
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Pin de ubicación",
                                    tint = Color.Red,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .padding(bottom = 36.dp)
                                        .size(44.dp)
                                )
                            }
                            Button(
                                onClick = { showMapDialog = false },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text("Confirmar Ubicación")
                            }
                        }
                    }
                }
            }

            // Botón de Publicar
            Button(
                onClick = {
                    val currentUser = auth.currentUser
                    if (currentUser != null && sport.isNotEmpty() && date.isNotEmpty()) {
                        isPublishing = true

                        val targetLocation = cameraPositionState.position.target
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
                            "location" to GeoPoint(targetLocation.latitude, targetLocation.longitude),
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
                else Text("Publicar Evento", fontSize = 18.sp)
            }
        }
    }
}