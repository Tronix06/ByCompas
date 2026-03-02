package com.bitronix.bycompas.ui.screens.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List // Cambiado aquí
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.bitronix.bycompas.navigation.Screen
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.maps.android.compose.*

data class EventData(
    val id: String = "",
    val sport: String = "",
    val level: String = "",
    val date: String = "",
    val time: String = "",
    val location: GeoPoint = GeoPoint(0.0, 0.0),
    val availableSlots: Int = 0,
    val notes: String = "",
    val creatorId: String = "",
    val participants: List<String> = emptyList(),
    val pendingRequests: List<String> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadarScreen(navController: NavController) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    var isMapView by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf("Todos") }
    var eventsList by remember { mutableStateOf<List<EventData>>(emptyList()) }
    var myLocation by remember { mutableStateOf<LatLng?>(null) }
    var selectedEvent by remember { mutableStateOf<EventData?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }

    val hasLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val cameraPositionState = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(LatLng(40.41, -3.70), 5f) }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            LocationServices.getFusedLocationProviderClient(context).lastLocation.addOnSuccessListener { loc ->
                loc?.let {
                    val latLng = LatLng(it.latitude, it.longitude)
                    myLocation = latLng
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 14f)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        db.collection("events").whereEqualTo("status", "open").addSnapshotListener { snap, _ ->
            eventsList = snap?.documents?.mapNotNull { it.toObject(EventData::class.java)?.copy(id = it.id) } ?: emptyList()
        }
    }

    val filteredEvents = eventsList.filter {
        if (selectedFilter == "Todos") true else it.sport.contains(selectedFilter.substringAfter(" "))
    }

    Box(Modifier.fillMaxSize()) {
        if (isMapView) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
                uiSettings = MapUiSettings(myLocationButtonEnabled = hasLocationPermission),
                contentPadding = PaddingValues(top = 70.dp)
            ) {
                filteredEvents.forEach { event ->
                    Marker(
                        state = MarkerState(LatLng(event.location.latitude, event.location.longitude)),
                        title = event.sport,
                        onClick = { selectedEvent = event; showBottomSheet = true; true }
                    )
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(top = 80.dp), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(filteredEvents) { event ->
                    Card(Modifier.fillMaxWidth().clickable { selectedEvent = event; showBottomSheet = true }) {
                        Column(Modifier.padding(16.dp)) {
                            Text(event.sport, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("Nivel: ${event.level}")
                            Text("${event.date} • ${event.time}", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(8.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ElevatedFilterChip(
                selected = !isMapView,
                onClick = { isMapView = !isMapView },
                label = { Text(if (isMapView) "Lista" else "Mapa") },
                leadingIcon = { Icon(if (isMapView) Icons.AutoMirrored.Filled.List else Icons.Default.LocationOn, null) } // Cambiado aquí
            )
            listOf("Todos", "⚽ Fútbol", "🎾 Pádel", "🏃‍♂️ Running").forEach {
                ElevatedFilterChip(selected = selectedFilter == it, onClick = { selectedFilter = it }, label = { Text(it) })
            }
        }

        ExtendedFloatingActionButton(
            onClick = { navController.navigate(Screen.CreateEvent.route) },
            icon = { Icon(Icons.Default.Add, null) },
            text = { Text("Crear Evento") },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
        )
    }

    if (showBottomSheet && selectedEvent != null) {
        ModalBottomSheet(onDismissRequest = { showBottomSheet = false }) {
            EventDetailSheet(selectedEvent!!, myLocation)
        }
    }
}

@Composable
fun EventDetailSheet(event: EventData, myLocation: LatLng?) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser
    var creatorName by remember { mutableStateOf("Cargando...") }

    LaunchedEffect(event) {
        db.collection("users").document(event.creatorId).get().addOnSuccessListener { creatorName = it.getString("name") ?: "Anon" }
    }

    Column(Modifier.fillMaxWidth().padding(24.dp).verticalScroll(rememberScrollState())) {
        Text(event.sport, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("Organiza: $creatorName", color = Color.Gray)
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            db.collection("events").document(event.id).update("pendingRequests", FieldValue.arrayUnion(user?.uid))
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Solicitar Unirse")
        }
    }
}