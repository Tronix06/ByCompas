package com.bitronix.bycompas.ui.screens.home

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

// --- MODELO DE DATOS ---
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
fun HomeScreen(navController: NavController) {
    var selectedItem by remember { mutableIntStateOf(1) }
    val items = listOf("Eventos", "Radar", "Perfil")
    val icons = listOf(Icons.Filled.DateRange, Icons.Filled.LocationOn, Icons.Filled.Person)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("ByCompas", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(icons[index], contentDescription = item) },
                        label = { Text(item) },
                        selected = selectedItem == index,
                        onClick = { selectedItem = index }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedItem) {
                0 -> MatchesScreenContent()
                1 -> RadarScreenContent(navController)
                2 -> ProfileScreenContent(navController)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadarScreenContent(navController: NavController) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    var isMapView by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf("Todos") }

    var eventsList by remember { mutableStateOf(emptyList<EventData>()) }
    var myLocation by remember { mutableStateOf<LatLng?>(null) }
    var selectedEvent by remember { mutableStateOf<EventData?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }

    val hasLocationPermission = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val defaultLocation = LatLng(40.4167, -3.7032)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 5f)
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val loc = LatLng(location.latitude, location.longitude)
                    myLocation = loc
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(loc, 14f)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        db.collection("events").whereEqualTo("status", "open")
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        val ev = doc.toObject(EventData::class.java)
                        ev?.copy(id = doc.id)
                    }
                    eventsList = list
                }
            }
    }

    // --- LA MAGIA DEL FILTRADO ---
    // Creamos una lista filtrada en tiempo real según el botón superior que esté pulsado
    val filteredEvents = eventsList.filter { event ->
        when (selectedFilter) {
            "Todos" -> true
            "⚽ Fútbol" -> event.sport == "Fútbol"
            "🎾 Pádel" -> event.sport == "Pádel"
            "🏃‍♂️ Running" -> event.sport == "Running"
            // Por ahora "Hoy" y "Mañana" muestran todo hasta que configuremos la lógica de fechas
            "Hoy" -> true
            "Mañana" -> true
            else -> true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // --- RENDERIZAMOS MAPA O LISTA ---
        if (isMapView) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
                uiSettings = MapUiSettings(myLocationButtonEnabled = hasLocationPermission, zoomControlsEnabled = true),
                contentPadding = PaddingValues(top = 72.dp, bottom = 80.dp)
            ) {
                // Usamos la lista FILTRADA para pintar las chinchetas
                filteredEvents.forEach { event ->
                    Marker(
                        state = MarkerState(position = LatLng(event.location.latitude, event.location.longitude)),
                        title = "${event.sport} - ${event.time}",
                        snippet = "Nivel: ${event.level}",
                        onClick = {
                            selectedEvent = event
                            showBottomSheet = true
                            true
                        }
                    )
                }
            }
        } else {
            // ¡NUEVO! LA VISTA DE LISTA REAL
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 80.dp, bottom = 80.dp), // Espacio para botones
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (filteredEvents.isEmpty()) {
                    item {
                        Text(
                            text = "No hay eventos para este filtro.",
                            color = Color.Gray,
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    items(filteredEvents) { event ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // Al pulsar la tarjeta, se abre la misma ficha que en el mapa
                                    selectedEvent = event
                                    showBottomSheet = true
                                },
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(event.sport, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                                    Text("Quedan ${event.availableSlots} plazas", color = Color.Red, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Nivel: ${event.level}", fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.DateRange, contentDescription = "Fecha", modifier = Modifier.size(16.dp), tint = Color.Gray)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${event.date} a las ${event.time}", color = Color.Gray, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- BARRA SUPERIOR DE FILTROS ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 8.dp, start = 8.dp, end = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ElevatedFilterChip(
                selected = !isMapView,
                onClick = { isMapView = !isMapView },
                label = { Text(if (isMapView) "Ver Lista" else "Ver Mapa") },
                leadingIcon = { Icon(if (isMapView) Icons.Filled.List else Icons.Filled.LocationOn, contentDescription = "Cambiar vista") }
            )
            HorizontalDivider(modifier = Modifier.height(24.dp).width(1.dp), color = Color.Gray)

            val filtros = listOf("Todos", "⚽ Fútbol", "🎾 Pádel", "🏃‍♂️ Running", "Hoy", "Mañana")
            filtros.forEach { filtro ->
                ElevatedFilterChip(
                    selected = selectedFilter == filtro,
                    onClick = { selectedFilter = filtro },
                    label = { Text(filtro) }
                )
            }
        }

        // --- BOTÓN CREAR EVENTO ---
        ExtendedFloatingActionButton(
            onClick = { navController.navigate("create_event") },
            icon = { Icon(Icons.Filled.Add, "Crear Evento") },
            text = { Text("Crear Evento", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    }

    if (showBottomSheet && selectedEvent != null) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            EventDetailSheet(event = selectedEvent!!, myLocation = myLocation)
        }
    }
}

@Composable
fun EventDetailSheet(event: EventData, myLocation: LatLng?) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    var creatorName by remember { mutableStateOf("Cargando...") }
    var creatorRating by remember { mutableDoubleStateOf(0.0) }

    var isRequesting by remember { mutableStateOf(false) }
    val amIOrganizer = event.creatorId == currentUser?.uid
    val amIJoined = event.participants.contains(currentUser?.uid)
    val amIPending = event.pendingRequests.contains(currentUser?.uid)

    var distanciaStr by remember { mutableStateOf("Calculando distancia...") }

    LaunchedEffect(event) {
        db.collection("users").document(event.creatorId).get().addOnSuccessListener { doc ->
            creatorName = doc.getString("name") ?: "Deportista"
            creatorRating = doc.getDouble("rating") ?: 5.0
        }

        if (myLocation != null) {
            val results = FloatArray(1)
            Location.distanceBetween(
                myLocation.latitude, myLocation.longitude,
                event.location.latitude, event.location.longitude,
                results
            )
            val km = results[0] / 1000
            distanciaStr = if (km < 1) "A menos de 1 km de ti" else "A %.1f km de distancia".format(km)
        } else {
            distanciaStr = "Ubicación desconocida"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(50.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Person, contentDescription = "Foto", tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(creatorName, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("⭐ $creatorRating • Organizador", color = Color.Gray, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(12.dp))) {
            GoogleMap(
                cameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(LatLng(event.location.latitude, event.location.longitude), 14f)
                },
                uiSettings = MapUiSettings(scrollGesturesEnabled = false, zoomGesturesEnabled = false, rotationGesturesEnabled = false)
            ) {
                Marker(state = MarkerState(position = LatLng(event.location.latitude, event.location.longitude)))
            }
        }
        Text(distanciaStr, fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(event.sport, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("${event.date} • ${event.time}", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Text(event.level, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        val ocupadas = event.participants.size
        val totales = ocupadas + event.availableSlots
        val progreso = ocupadas.toFloat() / totales.toFloat()

        Text("Plazas ocupadas: $ocupadas de $totales", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progreso },
            modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )

        if (event.notes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Notas del organizador:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(event.notes, color = Color.Gray, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (currentUser != null && !amIOrganizer && !amIJoined && !amIPending) {
                    isRequesting = true
                    db.collection("events").document(event.id)
                        .update("pendingRequests", FieldValue.arrayUnion(currentUser.uid))
                        .addOnSuccessListener { isRequesting = false }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = !isRequesting && !amIOrganizer && !amIJoined && !amIPending && event.availableSlots > 0,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (amIPending) Color.Gray else MaterialTheme.colorScheme.primary
            )
        ) {
            if (isRequesting) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text(
                    text = when {
                        amIOrganizer -> "Es tu evento"
                        amIJoined -> "Ya estás dentro"
                        amIPending -> "Solicitud Pendiente ⏳"
                        event.availableSlots == 0 -> "Evento Lleno"
                        else -> "Solicitar Unirse"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun MatchesScreenContent() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Tus Actividades", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Aquí verás los eventos deportivos en los que estás inscrito.", fontSize = 16.sp)
    }
}

@Composable
fun ProfileScreenContent(navController: NavController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser

    var userName by remember { mutableStateOf("Cargando...") }
    var userEmail by remember { mutableStateOf(currentUser?.email ?: "") }
    var userRating by remember { mutableStateOf(0.0) }
    var radarRadius by remember { mutableFloatStateOf(15f) }
    var userSports by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        userName = document.getString("name") ?: "Deportista Anónimo"
                        userRating = document.getDouble("rating") ?: 5.0
                        radarRadius = document.getDouble("radarRadius")?.toFloat() ?: 15f
                        val sportsFromDb = document.get("sports") as? List<String>
                        if (sportsFromDb != null) { userSports = sportsFromDb }
                    }
                    isLoading = false
                }
                .addOnFailureListener {
                    userName = "Error al cargar"
                    isLoading = false
                }
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(100.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.Person, contentDescription = "Foto", tint = Color.White, modifier = Modifier.size(60.dp)) }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = userName, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(text = userEmail, fontSize = 16.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "⭐ $userRating", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(16.dp))
            AssistChip(onClick = { }, label = { Text("Buen rollo") })
            Spacer(modifier = Modifier.width(8.dp))
            AssistChip(onClick = { }, label = { Text("Puntual") })
        }
        Spacer(modifier = Modifier.height(32.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Mis Deportes", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                if (userSports.isEmpty()) {
                    Text("Aún no has configurado ningún deporte.", color = Color.Gray)
                } else {
                    userSports.forEach { deporte ->
                        Text("- $deporte", fontSize = 16.sp, modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { }) { Text("Gestionar mis deportes") }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Distancia del Radar: ${radarRadius.toInt()} km", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Recibir notificaciones de eventos deportivos en este radio.", fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = radarRadius, onValueChange = { radarRadius = it }, valueRange = 1f..50f, steps = 49,
                    onValueChangeFinished = {
                        if (currentUser != null) {
                            db.collection("users").document(currentUser.uid).update("radarRadius", radarRadius.toDouble())
                                .addOnSuccessListener { Toast.makeText(context, "Distancia guardada", Toast.LENGTH_SHORT).show() }
                        }
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedButton(
            onClick = { auth.signOut(); navController.navigate("login") { popUpTo(0) } },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Cerrar Sesión", color = Color.Gray) }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { showDeleteDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Borrar mi cuenta") }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("¿Estás seguro?") },
            text = { Text("Esta acción es irreversible.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (currentUser != null) {
                            db.collection("users").document(currentUser.uid).delete()
                                .addOnSuccessListener {
                                    currentUser.delete().addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            navController.navigate("login") { popUpTo(0) }
                                        }
                                    }
                                }
                        }
                        showDeleteDialog = false
                    }
                ) { Text("Sí, borrar cuenta", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") } }
        )
    }
}