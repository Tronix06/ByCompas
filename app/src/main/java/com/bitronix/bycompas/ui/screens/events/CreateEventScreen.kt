package com.bitronix.bycompas.ui.screens.events

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.bitronix.bycompas.navigation.Screen
import com.bitronix.bycompas.utils.AppConstants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import java.util.Calendar
import java.util.Locale
import android.location.Geocoder
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser

    // --- ESTADOS DEL FORMULARIO ---
    val deportes = AppConstants.SPORTS_LIST
    var selectedSport by rememberSaveable { mutableStateOf(deportes[0]) }
    var expandedSport by rememberSaveable { mutableStateOf(false) }
    var selectedLevel by rememberSaveable { mutableStateOf("Abierto a todos") }
    var expandedLevel by rememberSaveable { mutableStateOf(false) }

    var dateText by rememberSaveable { mutableStateOf("Fecha") }
    var timeText by rememberSaveable { mutableStateOf("Hora") }

    var eventTitle by rememberSaveable { mutableStateOf("") }
    var plazasLibres by rememberSaveable { mutableIntStateOf(4) }
    var creationMode by rememberSaveable { mutableStateOf("Solo yo") }
    var friendCount by rememberSaveable { mutableIntStateOf(0) }
    var notas by rememberSaveable { mutableStateOf("") }
    var isSaving by rememberSaveable { mutableStateOf(false) }

    // --- ESTADO DE UBICACIÓN ---
    var selectedLatLng by rememberSaveable { mutableStateOf(AppConstants.DEFAULT_LAT_LNG) }
    var selectedAddress by rememberSaveable { mutableStateOf("Toca para seleccionar ubicación") }
    var locationSelected by rememberSaveable { mutableStateOf(false) }

    val levelsOptions = listOf("Abierto a todos", "Principiante", "Intermedio", "Avanzado")

    // Escuchar el resultado de la pantalla de selección de ubicación
    LaunchedEffect(navController.currentBackStackEntry) {
        val lat = navController.currentBackStackEntry?.savedStateHandle?.get<Double>("lat")
        val lng = navController.currentBackStackEntry?.savedStateHandle?.get<Double>("lng")
        if (lat != null && lng != null) {
            val latLng = LatLng(lat, lng)
            selectedLatLng = latLng
            locationSelected = true
            
            // Revertimos Geocoding para obtener la dirección
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                selectedAddress = if (!addresses.isNullOrEmpty()) {
                    addresses[0].getAddressLine(0) ?: "Ubicación seleccionada"
                } else {
                    "Ubicación fijada"
                }
            } catch (e: Exception) {
                selectedAddress = "Ubicación fijada (${lat.toString().take(6)}, ${lng.toString().take(6)})"
            }
        }
    }

    val premiumGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF1565C0),
            Color(0xFF1E88E5)
        )
    )

    val inputFieldGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0D47A1),
            Color(0xFF1565C0)
        )
    )

    Scaffold(
        topBar = {
            // --- CABECERA PREMIUM REFINADA ---
            Surface(
                modifier = Modifier.fillMaxWidth().height(110.dp),
                color = Color.Transparent,
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(inputFieldGradient)
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {

                    
                    // Título Centrado (Bajo)
                    Text(
                        "Creación de eventos", 
                        fontSize = 22.sp, 
                        fontWeight = FontWeight.Black, 
                        color = Color.White,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f), MaterialTheme.colorScheme.surface)))
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // --- SECCIÓN 0: TÍTULO CON DEGRADADO PREMIUM ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color.Transparent,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .background(premiumGradient)
                        .padding(16.dp), 
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Título del evento", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = eventTitle,
                        onValueChange = { eventTitle = it },
                        label = { Text("Escribe el nombre aquí...", color = Color.White.copy(alpha = 0.7f)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 1,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.White.copy(alpha = 0.1f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.1f),
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                            focusedLabelColor = Color.White,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
                        )
                    )
                }
            }

            // --- SECCIÓN 1: EL DEPORTE CON DEGRADADO PREMIUM ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color.Transparent,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .background(premiumGradient)
                        .padding(16.dp), 
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Text("Elige deporte", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    ExposedDropdownMenuBox(
                        expanded = expandedSport,
                        onExpandedChange = { expandedSport = !expandedSport },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedSport,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Seleccionar deporte", color = Color.White.copy(alpha = 0.7f)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSport) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color.White.copy(alpha = 0.1f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.1f),
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                                focusedLabelColor = Color.White,
                                unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                                focusedTrailingIconColor = Color.White,
                                unfocusedTrailingIconColor = Color.White
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = expandedSport, 
                            onDismissRequest = { expandedSport = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            deportes.forEach { sport ->
                                DropdownMenuItem(
                                    text = { Text(sport, color = Color.Black) },
                                    onClick = {
                                        selectedSport = sport
                                        selectedLevel = "Abierto a todos"
                                        expandedSport = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // --- SECCIÓN 2: FECHA Y HORA CON DEGRADADO PREMIUM ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color.Transparent,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .background(premiumGradient)
                        .padding(16.dp), 
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("¿Cuándo es el partido?", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                val cal = Calendar.getInstance()
                                DatePickerDialog(context, { _, y, m, d -> dateText = "$d/${m + 1}/$y" }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.DateRange, null, modifier = Modifier.size(18.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(dateText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
    
                        OutlinedButton(
                            onClick = {
                                val cal = Calendar.getInstance()
                                TimePickerDialog(context, { _, h, min -> timeText = String.format("%02d:%02d", h, min) }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Schedule, null, modifier = Modifier.size(18.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(timeText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // --- SECCIÓN 3: UBICACIÓN CON DEGRADADO PREMIUM ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color.Transparent,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .background(premiumGradient)
                        .padding(16.dp), 
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("¿Dónde quedamos?", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedButton(
                        onClick = { navController.navigate(Screen.LocationPicker.route) },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(
                            if (locationSelected) Icons.Default.CheckCircle else Icons.Default.LocationOn, 
                            null, 
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(selectedAddress, fontSize = 14.sp, maxLines = 1, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // --- SECCIÓN: MODO DE CREACIÓN ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color.Transparent,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .background(premiumGradient)
                        .padding(16.dp), 
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("¿Quién viene contigo?", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Solo yo", "Con amigos").forEach { mode ->
                            val isSelected = creationMode == mode
                            Surface(
                                onClick = { creationMode = mode },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.1f),
                                border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)) else null
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = mode,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (creationMode == "Con amigos") {
                // --- SECCIÓN: NÚMERO DE AMIGOS ---
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Transparent,
                    shadowElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .background(premiumGradient)
                            .padding(16.dp), 
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("¿Cuántos amigos traes?", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Text("Indica el número de acompañantes", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        SlotsSelector(
                            value = friendCount,
                            onValueChange = { friendCount = it },
                            range = 1..10
                        )
                    }
                }
            }

            // --- SECCIÓN 4: PLAZAS CON DEGRADADO PREMIUM ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color.Transparent,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .background(premiumGradient)
                        .padding(16.dp), 
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Plazas disponibles", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Text("Ajusta el número de plazas", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Selector de Plazas Profesional
                    SlotsSelector(
                        value = plazasLibres,
                        onValueChange = { plazasLibres = it },
                        range = 1..22
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val totalOccupied = 1 + (if (creationMode == "Con amigos") friendCount else 0)
                    val totalCapacity = totalOccupied + plazasLibres
                    
                    Surface(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Resumen: $totalCapacity plazas totales ($totalOccupied ocupadas, $plazasLibres libres)",
                            fontSize = 13.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(12.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            // --- SECCIÓN 5: NIVEL Y NOTAS CON DEGRADADO PREMIUM ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color.Transparent,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .background(premiumGradient)
                        .padding(16.dp), 
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Detalles adicionales", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    ExposedDropdownMenuBox(
                        expanded = expandedLevel,
                        onExpandedChange = { expandedLevel = !expandedLevel },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedLevel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Nivel del partido", color = Color.White.copy(alpha = 0.7f)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedLevel) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color.White.copy(alpha = 0.1f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.1f),
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                                focusedLabelColor = Color.White,
                                unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                                focusedTrailingIconColor = Color.White,
                                unfocusedTrailingIconColor = Color.White
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = expandedLevel, 
                            onDismissRequest = { expandedLevel = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            levelsOptions.forEach { level ->
                                DropdownMenuItem(
                                    text = { Text(level, color = Color.Black) },
                                    onClick = {
                                        selectedLevel = level
                                        expandedLevel = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = notas,
                        onValueChange = { notas = it },
                        label = { Text("Notas (Opcional)", color = Color.White.copy(alpha = 0.7f)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.White.copy(alpha = 0.1f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.1f),
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                            focusedLabelColor = Color.White,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
                        )
                    )
                }
            }

            // --- BOTÓN FINAL EXTRA PREMIUM ---
            Surface(
                onClick = {
                    if (eventTitle.isBlank() || dateText == "Fecha" || !locationSelected) {
                        Toast.makeText(context, "Ponle un título, fecha y ubicación", Toast.LENGTH_SHORT).show()
                        return@Surface
                    }
                    if (currentUser == null) return@Surface
                    isSaving = true
                    val totalOccupied = 1 + (if (creationMode == "Con amigos") friendCount else 0)
                    val totalCapacity = totalOccupied + plazasLibres

                    val eventMap = hashMapOf(
                        "title" to eventTitle,
                        "sport" to selectedSport,
                        "level" to selectedLevel,
                        "date" to dateText,
                        "time" to timeText,
                        "location" to GeoPoint(selectedLatLng.latitude, selectedLatLng.longitude),
                        "address" to selectedAddress,
                        "creationMode" to creationMode,
                        "friendCount" to (if (creationMode == "Con amigos") friendCount else 0),
                        "availableSlots" to plazasLibres,
                        "occupiedSlots" to totalOccupied,
                        "totalSlots" to totalCapacity,
                        "notes" to notas,
                        "creatorId" to currentUser.uid,
                        "participants" to if (creationMode == "Con amigos") {
                            listOf(currentUser.uid) + List(friendCount) { "friend_${currentUser.uid}_${System.currentTimeMillis()}_$it" }
                        } else {
                            listOf(currentUser.uid)
                        },
                        "status" to "open",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    db.collection("events").add(eventMap)
                        .addOnSuccessListener { docRef ->
                            val eventId = docRef.id
                            // Creamos el chat temporal vinculado al evento
                            val chatMap = hashMapOf(
                                "eventId" to eventId,
                                "isGroup" to true,
                                "isEventChat" to true,
                                "sport" to selectedSport,
                                "eventDate" to dateText,
                                "eventTime" to timeText,
                                "participants" to listOf(currentUser.uid),
                                "groupName" to (eventTitle.ifBlank { selectedSport }),
                                "creatorId" to currentUser.uid,
                                "lastMessage" to "Chat del evento creado",
                                "createdAt" to System.currentTimeMillis(),
                                "updatedAt" to System.currentTimeMillis()
                            )
                            db.collection("chats").document(eventId).set(chatMap).addOnSuccessListener {
                                Toast.makeText(context, "🏆 ¡Evento publicado!", Toast.LENGTH_LONG).show()
                                navController.popBackStack()
                            }
                        }
                        .addOnFailureListener { isSaving = false }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp),
                shape = RoundedCornerShape(24.dp),
                enabled = !isSaving,
                shadowElevation = 12.dp,
                color = Color.Transparent,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (isSaving) SolidColor(Color.Gray.copy(alpha = 0.5f)) else inputFieldGradient),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "PUBLICAR EVENTO", 
                                fontSize = 18.sp, 
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SlotsSelector(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Botón Menos
        IconButton(
            onClick = { if (value > range.first) onValueChange(value - 1) },
            modifier = Modifier
                .size(48.dp)
                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
        ) {
            Icon(
                Icons.Default.Remove, 
                contentDescription = "Menos", 
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(32.dp))

        // Card Central con el Número (Estilo Crystal)
        Surface(
            modifier = Modifier.size(80.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color.White.copy(alpha = 0.1f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
            shadowElevation = 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = value.toString(),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "CANTIDAD",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.7f),
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(32.dp))

        // Botón Más
        IconButton(
            onClick = { if (value < range.last) onValueChange(value + 1) },
            modifier = Modifier
                .size(48.dp)
                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
        ) {
            Icon(
                Icons.Default.Add, 
                contentDescription = "Más", 
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
    
    // Barra de progreso sutil debajo
    Spacer(modifier = Modifier.height(16.dp))
    val progress = (value - range.first).toFloat() / (range.last - range.first).toFloat()
    Box(
        modifier = Modifier
            .width(200.dp)
            .height(4.dp)
            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .fillMaxHeight()
                .background(Color.White, RoundedCornerShape(2.dp))
        )
    }
}