package com.bitronix.bycompas.ui.screens.events

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser

    // --- ESTADOS DEL FORMULARIO ---
    val deportes = listOf("Fútbol", "Pádel", "Running", "Baloncesto", "Tenis", "Ciclismo")
    var selectedSport by remember { mutableStateOf(deportes[0]) }
    var expandedSport by remember { mutableStateOf(false) }

    val nivelesPorDeporte = mapOf(
        "Fútbol" to listOf("Abierto a todos", "Pachanga / Amigos", "Amateur", "Alto Nivel"),
        "Pádel" to listOf("Abierto a todos", "Iniciación", "Intermedio", "Avanzado"),
        "Running" to listOf("Abierto a todos", "Trote Suave", "Ritmo < 5:30", "Ritmo < 4:30"),
        "Baloncesto" to listOf("Abierto a todos", "Parque", "Amateur", "Federado"),
        "Tenis" to listOf("Abierto a todos", "Iniciación", "Intermedio", "Avanzado"),
        "Ciclismo" to listOf("Abierto a todos", "Paseo", "Ruta Media", "Ruta Montaña")
    )
    var selectedLevel by remember { mutableStateOf(nivelesPorDeporte[selectedSport]!![0]) }
    var expandedLevel by remember { mutableStateOf(false) }

    var dateText by remember { mutableStateOf("Seleccionar fecha") }
    var timeText by remember { mutableStateOf("Seleccionar hora") }

    var plazasLibres by remember { mutableIntStateOf(1) }
    var notas by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    // --- ESTADO DEL MINI-MAPA ---
    val defaultLocation = LatLng(40.4167, -3.7032) // Empezamos en Madrid por defecto
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 15f)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear Evento", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. SELECTOR DE DEPORTE
            ExposedDropdownMenuBox(
                expanded = expandedSport,
                onExpandedChange = { expandedSport = !expandedSport },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedSport,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("¿Qué deporte?") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSport) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expandedSport, onDismissRequest = { expandedSport = false }) {
                    deportes.forEach { sport ->
                        DropdownMenuItem(
                            text = { Text(sport) },
                            onClick = {
                                selectedSport = sport
                                selectedLevel = nivelesPorDeporte[sport]!![0] // Reseteamos el nivel al cambiar de deporte
                                expandedSport = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. FECHA Y HORA (En una fila)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val calendar = Calendar.getInstance()
                        DatePickerDialog(context, { _, year, month, dayOfMonth ->
                            dateText = "$dayOfMonth/${month + 1}/$year"
                        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                ) {
                    Text(dateText)
                }

                Button(
                    onClick = {
                        val calendar = Calendar.getInstance()
                        TimePickerDialog(context, { _, hourOfDay, minute ->
                            val min = if (minute < 10) "0$minute" else minute.toString()
                            timeText = "$hourOfDay:$min"
                        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                ) {
                    Text(timeText)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. LA MAGIA: EL MINI-MAPA DE UBICACIÓN
            Text("Ubicación exacta:", modifier = Modifier.align(Alignment.Start), fontWeight = FontWeight.Bold)
            Text("Mueve el mapa para colocar la chincheta donde se jugará", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(zoomControlsEnabled = false, compassEnabled = false)
                )
                // ¡El Pin de Uber! Se queda fijo en el centro de la caja mientras el mapa se mueve por debajo
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = "Punto exacto",
                    tint = Color.Red,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(48.dp)
                        .padding(bottom = 24.dp) // Lo subimos un poco para que la "punta" apunte al centro real
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. PLAZAS LIBRES (Contador)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Plazas Libres:", fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { if (plazasLibres > 1) plazasLibres-- }) { Text("-", fontSize = 24.sp, fontWeight = FontWeight.Bold) }
                    Text(text = "$plazasLibres", fontSize = 20.sp, modifier = Modifier.padding(horizontal = 16.dp))
                    IconButton(onClick = { if (plazasLibres < 50) plazasLibres++ }) { Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold) }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. NIVEL EXIGIDO
            ExposedDropdownMenuBox(
                expanded = expandedLevel,
                onExpandedChange = { expandedLevel = !expandedLevel },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedLevel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Nivel exigido") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedLevel) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expandedLevel, onDismissRequest = { expandedLevel = false }) {
                    nivelesPorDeporte[selectedSport]?.forEach { level ->
                        DropdownMenuItem(
                            text = { Text(level) },
                            onClick = {
                                selectedLevel = level
                                expandedLevel = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 6. NOTAS OPCIONALES
            OutlinedTextField(
                value = notas,
                onValueChange = { notas = it },
                label = { Text("Notas (Ej: Llevad balón, pista 3...)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 7. BOTÓN GIGANTE: LANZAR AVISO
            Button(
                onClick = {
                    if (dateText == "Seleccionar fecha" || timeText == "Seleccionar hora") {
                        Toast.makeText(context, "Por favor, elige fecha y hora", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (currentUser == null) return@Button

                    isSaving = true

                    // Obtenemos la coordenada exacta donde el usuario dejó el centro del mapa
                    val finalLocation = cameraPositionState.position.target

                    // Construimos el documento que irá a la base de datos
                    val eventMap = hashMapOf(
                        "sport" to selectedSport,
                        "level" to selectedLevel,
                        "date" to dateText,
                        "time" to timeText,
                        "location" to GeoPoint(finalLocation.latitude, finalLocation.longitude), // Formato GPS de Firebase
                        "availableSlots" to plazasLibres,
                        "notes" to notas,
                        "creatorId" to currentUser.uid,
                        "participants" to listOf(currentUser.uid), // Al crearlo, ya estás apuntado tú
                        "status" to "open",
                        "createdAt" to FieldValue.serverTimestamp() // Fecha de creación real
                    )

                    // Lo guardamos en una nueva colección llamada "events"
                    db.collection("events").add(eventMap)
                        .addOnSuccessListener {
                            isSaving = false
                            Toast.makeText(context, "¡Aviso lanzado con éxito!", Toast.LENGTH_LONG).show()
                            navController.popBackStack() // Volvemos al mapa
                        }
                        .addOnFailureListener { e ->
                            isSaving = false
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                } else {
                    Text("🚀 LANZAR AVISO", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}