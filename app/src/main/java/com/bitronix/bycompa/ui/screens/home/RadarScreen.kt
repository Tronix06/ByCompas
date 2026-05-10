package com.bitronix.bycompa.ui.screens.home

import com.bitronix.bycompa.models.EventData
import com.bitronix.bycompa.utils.AppConstants


import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List // Cambiado aquí
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.bitronix.bycompa.navigation.Screen
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.compose.*
import java.util.Calendar
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.*
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.*
import com.bitronix.bycompa.ui.components.EventDetailSheet
import com.google.android.gms.maps.CameraUpdateFactory
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadarScreen(navController: NavController) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    var isMapView by remember { mutableStateOf(true) }
    var selectedSport by remember { mutableStateOf("Todos") }
    var selectedLevel by remember { mutableStateOf("Todos") }
    var eventsList by remember { mutableStateOf<List<EventData>>(emptyList()) }
    var myLocation by remember { mutableStateOf<LatLng?>(null) }
    var selectedEvent by remember { mutableStateOf<EventData?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var locationPermissionGranted by remember { 
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var expandedSportFilter by remember { mutableStateOf(false) }
    var expandedLevelFilter by remember { mutableStateOf(false) }
    var userRadius by remember { mutableStateOf(15.0) }
    var onlyToday by remember { mutableStateOf(false) }
    var permissionRequested by rememberSaveable { mutableStateOf(false) }
    var mapType by remember { mutableStateOf(MapType.NORMAL) }
    var expandedMapType by remember { mutableStateOf(false) }
    var useInfoMarkers by remember { mutableStateOf(false) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    val cameraPositionState = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(AppConstants.DEFAULT_LAT_LNG, 5f) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        locationPermissionGranted = result.values.all { it }
    }

    // Disparo automático del permiso al entrar
    LaunchedEffect(Unit) {
        if (!locationPermissionGranted && !permissionRequested) {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            permissionRequested = true
        }
    }

    LaunchedEffect(locationPermissionGranted) {
        if (locationPermissionGranted) {
            try {
                LocationServices.getFusedLocationProviderClient(context).lastLocation.addOnSuccessListener { loc ->
                    loc?.let {
                        val latLng = LatLng(it.latitude, it.longitude)
                        myLocation = latLng
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, AppConstants.RADAR_MAP_ZOOM)
                    }
                }
            } catch (e: SecurityException) {
                // Manejar error de seguridad si el permiso fue revocado repentinamente
            }
        }
    }

    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid).addSnapshotListener { snap, _ ->
                userRadius = snap?.getDouble("radarRadius") ?: 15.0
            }
        }
    }

    LaunchedEffect(Unit) {
        db.collection("events").whereEqualTo("status", "open").addSnapshotListener { snap, error ->
            if (error != null) {
                isLoading = false
                return@addSnapshotListener
            }
            eventsList = snap?.documents?.mapNotNull { it.toObject(EventData::class.java)?.copy(id = it.id) } ?: emptyList()
            isLoading = false
        }
    }

    val today = Calendar.getInstance()
    val todayStr = "${today.get(Calendar.DAY_OF_MONTH)}/${today.get(Calendar.MONTH) + 1}/${today.get(Calendar.YEAR)}"

    val filteredEvents = eventsList.filter {
        val matchesSport = if (selectedSport == "Todos") true else it.sport == selectedSport
        val matchesLevel = if (selectedLevel == "Todos") true else it.level == selectedLevel
        val matchesToday = if (onlyToday) it.date == todayStr else true
        
        // Filtrado por distancia (en kms)
        val matchesDistance = if (myLocation != null) {
            val results = FloatArray(1)
            android.location.Location.distanceBetween(
                myLocation!!.latitude, myLocation!!.longitude,
                it.location.latitude, it.location.longitude,
                results
            )
            val distanceInKm = results[0] / 1000
            distanceInKm <= userRadius
        } else true

        val matchesSearch = if (searchQuery.isBlank()) true else {
            it.sport.contains(searchQuery, ignoreCase = true) || 
            it.level.contains(searchQuery, ignoreCase = true) ||
            (it.title.contains(searchQuery, ignoreCase = true))
        }
        matchesSport && matchesLevel && matchesDistance && matchesSearch && matchesToday
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
        if (!locationPermissionGranted) {
            // Caso 1: No hay permisos - Bloqueamos la vista hasta tenerlos
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                Spacer(Modifier.height(16.dp))
                Text("Buscando gente cerca...", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("Para mostrarte eventos en tu zona y conectar con otros compas, necesitamos activar el GPS.", 
                    textAlign = TextAlign.Center, 
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = {
                        permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(0.7f).height(50.dp)
                ) {
                    Text("Activar Ubicación")
                }
            }
        } else if (isLoading) {
            // Caso 2: Cargando datos de Firestore
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (isMapView) {
            // Caso 3: Mapa interactivo
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = true,
                    mapType = mapType
                ),
                uiSettings = MapUiSettings(
                    myLocationButtonEnabled = false,
                    zoomControlsEnabled = false
                ),
                contentPadding = PaddingValues(top = 220.dp, bottom = 80.dp) 
            ) {
                filteredEvents.forEach { event ->
                    if (useInfoMarkers) {
                        // MODO SMART: Todos los bocadillos visibles a la vez (AZUL)
                        val markerIcon = remember(event.id, event.sport, event.title, event.level, event.date, event.occupiedSlots, event.totalSlots) {
                            val slots = "${event.occupiedSlots}/${event.totalSlots}"
                            createCustomMarkerBitmap(
                                context = context,
                                emoji = getSportEmoji(event.sport),
                                title = event.title.ifBlank { event.sport },
                                date = event.date,
                                level = event.level,
                                slots = slots
                            )
                        }
                        
                        Marker(
                            state = MarkerState(LatLng(event.location.latitude, event.location.longitude)),
                            icon = markerIcon,
                            anchor = androidx.compose.ui.geometry.Offset(0.5f, 1.0f),
                            onClick = { 
                                selectedEvent = event
                                showBottomSheet = true
                                true 
                            }
                        )
                    } else {
                        // MODO NORMAL: Chinchetas AZULES estándar
                        Marker(
                            state = MarkerState(LatLng(event.location.latitude, event.location.longitude)),
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                            title = event.sport,
                            onClick = { 
                                selectedEvent = event
                                showBottomSheet = true
                                true 
                            }
                        )
                    }
                }
            }
        } else {
            // Caso 4: Listado de eventos con permisos OK
            val hasFilters = selectedSport != "Todos" || selectedLevel != "Todos" || onlyToday
            val dynamicTopPadding = if (hasFilters) 230.dp else 180.dp

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(top = dynamicTopPadding), 
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 180.dp), 
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (filteredEvents.isEmpty()) {
                    item {
                        Box(Modifier.fillParentMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                            Text("No hay eventos disponibles", color = Color.Gray)
                        }
                    }
                }
                items(filteredEvents) { event ->
                    com.bitronix.bycompa.ui.components.EventSummaryCard(
                        event = event,
                        onClick = {
                            selectedEvent = event
                            showBottomSheet = true
                        }
                    )
                }
            }
        }

        // --- COLUMNA DE CONTROLES DEL MAPA (Derecha) ---
        if (isMapView && locationPermissionGranted) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 220.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.End
            ) {
                // 1. Botón Mi Ubicación
                MapControlButton(Icons.Default.MyLocation) {
                    myLocation?.let {
                        scope.launch {
                            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(it, AppConstants.RADAR_MAP_ZOOM))
                        }
                    }
                }

                // 2. Control de Capas Horizontal
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = expandedMapType,
                        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInHorizontally(initialOffsetX = { it }),
                        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutHorizontally(targetOffsetX = { it })
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Botones secundarios más pequeños, sutiles e invertidos
                            MapControlButton(
                                icon = Icons.Default.Map,
                                isSelected = mapType == MapType.NORMAL,
                                inverted = true,
                                size = 34.dp,
                                iconSize = 16.dp
                            ) { mapType = MapType.NORMAL; expandedMapType = false }
                            
                            MapControlButton(
                                icon = Icons.Default.Satellite,
                                isSelected = mapType == MapType.SATELLITE,
                                inverted = true,
                                size = 34.dp,
                                iconSize = 16.dp
                            ) { mapType = MapType.SATELLITE; expandedMapType = false }
                            
                            MapControlButton(
                                icon = Icons.Default.Terrain,
                                isSelected = mapType == MapType.TERRAIN,
                                inverted = true,
                                size = 34.dp,
                                iconSize = 16.dp
                            ) { mapType = MapType.TERRAIN; expandedMapType = false }
                        }
                    }
                    
                    MapControlButton(Icons.Default.Layers, isSelected = expandedMapType) { expandedMapType = !expandedMapType }
                }

                // 3. Toggle de Estilo de Marcadores (NUEVO)
                MapControlButton(
                    icon = if (useInfoMarkers) Icons.Default.SpeakerNotes else Icons.Default.LocationOn,
                    isSelected = useInfoMarkers
                ) { useInfoMarkers = !useInfoMarkers }

                // 4. Botón Zoom In
                MapControlButton(Icons.Default.Add) {
                    scope.launch {
                        cameraPositionState.animate(CameraUpdateFactory.zoomIn())
                    }
                }

                // 4. Botón Zoom Out
                MapControlButton(Icons.Default.Remove) {
                    scope.launch {
                        cameraPositionState.animate(CameraUpdateFactory.zoomOut())
                    }
                }
            }
        }

        val headerGradient = Brush.linearGradient(
            colors = listOf(
                Color(0xFF1565C0),
                Color(0xFF1E88E5)
            )
        )

        // --- CABECERA PREMIUM (FLOTANTE) ---
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(12.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = Color.Transparent,
            shadowElevation = 12.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier
                    .background(headerGradient)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Fila 1: Buscador Estilo Premium
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Buscar por evento o deporte", fontSize = 14.sp, color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, null, modifier = Modifier.size(20.dp))
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.4f),
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
                        unfocusedLeadingIconColor = Color.Gray
                    )
                )

                Spacer(Modifier.height(16.dp))

                // Fila 2: Acciones Rápidas (Chips Rectangulares)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Alternar Vista
                    ActionItem(
                        label = "VISTA", 
                        icon = if (isMapView) Icons.AutoMirrored.Filled.List else Icons.Default.Map, 
                        isActive = !isMapView,
                        isOnBlue = true,
                        modifier = Modifier.weight(1f)
                    ) { isMapView = !isMapView }
                    
                    // 2. Deporte
                    Box(modifier = Modifier.weight(1f)) {
                        ActionItem(
                            label = "DEPORTE", 
                            icon = Icons.Default.SportsScore, 
                            isActive = selectedSport != "Todos",
                            isOnBlue = true,
                            modifier = Modifier.fillMaxWidth()
                        ) { expandedSportFilter = true }
                        
                        DropdownMenu(
                            expanded = expandedSportFilter,
                            onDismissRequest = { expandedSportFilter = false },
                            modifier = Modifier.heightIn(max = 400.dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Todos los deportes", fontWeight = FontWeight.Bold) },
                                onClick = { selectedSport = "Todos"; expandedSportFilter = false }
                            )
                            HorizontalDivider()
                            AppConstants.SPORTS_LIST.forEach { sport ->
                                DropdownMenuItem(
                                    text = { Text(sport) },
                                    onClick = { selectedSport = sport; expandedSportFilter = false }
                                )
                            }
                        }
                    }

                    // 3. Nivel
                    Box(modifier = Modifier.weight(1f)) {
                        ActionItem(
                            label = "NIVEL", 
                            icon = Icons.Default.FilterList, 
                            isActive = selectedLevel != "Todos",
                            isOnBlue = true,
                            modifier = Modifier.fillMaxWidth()
                        ) { expandedLevelFilter = true }
                        
                        DropdownMenu(
                            expanded = expandedLevelFilter,
                            onDismissRequest = { expandedLevelFilter = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Todos los niveles", fontWeight = FontWeight.Bold) },
                                onClick = { selectedLevel = "Todos"; expandedLevelFilter = false }
                            )
                            HorizontalDivider()
                            listOf("Abierto a todos", "Principiante", "Intermedio", "Avanzado").forEach { level ->
                                DropdownMenuItem(
                                    text = { Text(level) },
                                    onClick = { selectedLevel = level; expandedLevelFilter = false }
                                )
                            }
                        }
                    }

                    // 4. Hoy
                    ActionItem(
                        label = "HOY", 
                        icon = Icons.Default.Event, 
                        isActive = onlyToday,
                        isOnBlue = true,
                        modifier = Modifier.weight(1f)
                    ) { onlyToday = !onlyToday }
                }

                // Fila 3: Badges de filtros activos
                if (selectedSport != "Todos" || selectedLevel != "Todos" || onlyToday) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedSport != "Todos") FilterBadge(selectedSport) { selectedSport = "Todos" }
                        if (selectedLevel != "Todos") {
                            Spacer(Modifier.width(6.dp))
                            FilterBadge(selectedLevel) { selectedLevel = "Todos" }
                        }
                        if (onlyToday) {
                            Spacer(Modifier.width(6.dp))
                            FilterBadge("Hoy") { onlyToday = false }
                        }
                    }
                }
            }
        }

        val premiumGradient = Brush.linearGradient(
            colors = listOf(
                Color(0xFF1565C0),
                Color(0xFF1E88E5)
            )
        )

        Surface(
            onClick = { navController.navigate(Screen.CreateEvent.route) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp)
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color.Transparent,
            shadowElevation = 8.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .background(premiumGradient)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Crear Evento", 
                        color = Color.White, 
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

    if (showBottomSheet && selectedEvent != null) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = Color.Transparent,
            dragHandle = null,
            scrimColor = Color.Black.copy(alpha = 0.4f)
        ) {
            EventDetailSheet(
                event = selectedEvent!!,
                myLocation = myLocation,
                navController = navController,
                snackbarHostState = snackbarHostState
            )
        }
    }
}

@Composable
fun ActionItem(
    label: String,
    icon: ImageVector,
    isActive: Boolean,
    isOnBlue: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val premiumGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF1565C0),
            Color(0xFF1E88E5)
        )
    )

    Surface(
        onClick = onClick,
        modifier = modifier.height(42.dp),
        shape = RoundedCornerShape(12.dp),
        color = when {
            isOnBlue && isActive -> Color(0xFF0D47A1) // Azul más oscuro y sólido para resaltar sobre el degradado
            isOnBlue -> Color.White
            else -> Color.Transparent
        },
        border = when {
            isOnBlue && isActive -> androidx.compose.foundation.BorderStroke(1.dp, Color.White)
            isOnBlue && !isActive -> null
            !isOnBlue && !isActive -> androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E88E5).copy(alpha = 0.2f))
            else -> null
        },
        shadowElevation = if (isActive) 8.dp else 4.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (!isOnBlue && isActive) Modifier.background(premiumGradient) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Icon(
                    icon, 
                    null, 
                    modifier = Modifier.size(14.dp),
                    tint = when {
                        isOnBlue && isActive -> Color.White
                        isOnBlue -> MaterialTheme.colorScheme.primary
                        isActive -> Color.White
                        else -> Color.Black
                    }
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    label, 
                    fontSize = 10.sp, 
                    fontWeight = FontWeight.Black, 
                    maxLines = 1,
                    color = when {
                        isOnBlue && isActive -> Color.White
                        isOnBlue -> MaterialTheme.colorScheme.primary
                        isActive -> Color.White
                        else -> Color.Black
                    },
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun FilterBadge(text: String, onDismiss: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Default.Close,
                null,
                modifier = Modifier
                    .size(12.dp)
                    .clickable { onDismiss() },
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun MapControlButton(
    icon: ImageVector, 
    isSelected: Boolean = false,
    inverted: Boolean = false,
    size: androidx.compose.ui.unit.Dp = 40.dp,
    iconSize: androidx.compose.ui.unit.Dp = 20.dp,
    onClick: () -> Unit
) {
    val premiumGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF1565C0),
            Color(0xFF1E88E5)
        )
    )

    Surface(
        onClick = onClick,
        modifier = Modifier.size(size),
        shape = RoundedCornerShape(10.dp),
        color = if (inverted) Color.White else Color.Transparent,
        shadowElevation = if (isSelected) 8.dp else 4.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            if (isSelected) (if (inverted) MaterialTheme.colorScheme.primary else Color.White) 
            else (if (inverted) Color.LightGray.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.2f))
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (!inverted) Modifier.background(premiumGradient) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon, 
                null, 
                modifier = Modifier.size(iconSize),
                tint = if (inverted) MaterialTheme.colorScheme.primary else Color.White
            )
        }
    }
}

fun createCustomMarkerBitmap(
    context: android.content.Context,
    emoji: String,
    title: String,
    date: String,
    level: String,
    slots: String
): BitmapDescriptor {
    val density = context.resources.displayMetrics.density
    
    // Configuración de dimensiones (en píxeles)
    val padding = 8f * density
    val emojiSize = 24f * density
    val titleTextSize = 14f * density
    val secondaryTextSize = 10f * density
    val pointerHeight = 8f * density
    val pinRadius = 8f * density
    
    // Paints Premium Overhaul
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { 
        color = android.graphics.Color.WHITE 
        // Sombra más difusa y profesional (estilo Apple/Google)
        setShadowLayer(10f * density, 0f, 5f * density, android.graphics.Color.parseColor("#1F000000"))
    }
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { 
        style = Paint.Style.STROKE
        strokeWidth = 1.8f * density // Grosor elegante, no tan gordo
        // Gradiente Indigo -> Azul Real
        shader = android.graphics.LinearGradient(
            0f, 0f, 0f, 100f * density,
            android.graphics.Color.parseColor("#1A237E"), // Indigo
            android.graphics.Color.parseColor("#1565C0"), // Azul Royal
            android.graphics.Shader.TileMode.CLAMP
        )
        alpha = 230 // Un pelín de transparencia para suavizar
    }
    val innerTintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#051565C0") // Tinte azul casi invisible para profundidad
    }
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#1565C0")
        textSize = titleTextSize
        isFakeBoldText = true
    }
    val secondaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#455A64")
        textSize = secondaryTextSize
        isFakeBoldText = true
    }
    val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = emojiSize
    }
    val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.parseColor("#1565C0") }
    val pinBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * density
    }
    val pointerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#1565C0")
    }

    // Medir textos
    val titleBounds = Rect()
    titlePaint.getTextBounds(title, 0, title.length, titleBounds)
    val dateText = "📅 $date"
    val levelText = "⭐ $level"
    val slotsText = "👥 $slots"
    
    val dateBounds = Rect()
    secondaryPaint.getTextBounds(dateText, 0, dateText.length, dateBounds)
    val levelBounds = Rect()
    secondaryPaint.getTextBounds(levelText, 0, levelText.length, levelBounds)
    val slotsBounds = Rect()
    secondaryPaint.getTextBounds(slotsText, 0, slotsText.length, slotsBounds)
    
    val maxTextWidth = maxOf(titleBounds.width(), maxOf(dateBounds.width(), maxOf(levelBounds.width(), slotsBounds.width()))).toFloat()
    val bubbleWidth = maxOf(emojiSize, maxTextWidth) + (padding * 4f)
    
    val contentHeight = emojiSize + titleBounds.height() + dateBounds.height() + levelBounds.height() + slotsBounds.height() + (padding * 4f)
    val bubbleHeight = contentHeight + (padding * 2f)
    
    val width = bubbleWidth.toInt()
    val height = (bubbleHeight + pointerHeight + pinRadius * 2f + (10f * density)).toInt()
    
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    // 1. Aplicar gradiente dinámico al borde
    borderPaint.shader = android.graphics.LinearGradient(
        0f, 0f, 0f, bubbleHeight,
        android.graphics.Color.parseColor("#1A237E"),
        android.graphics.Color.parseColor("#1565C0"),
        android.graphics.Shader.TileMode.CLAMP
    )

    // 2. Dibujar burbuja con capas de profundidad
    val bubbleRect = RectF(0f, 0f, bubbleWidth, bubbleHeight)
    canvas.drawRoundRect(bubbleRect, 22f * density, 22f * density, bgPaint)
    canvas.drawRoundRect(bubbleRect, 22f * density, 22f * density, innerTintPaint)
    canvas.drawRoundRect(bubbleRect, 22f * density, 22f * density, borderPaint)
    
    // 3. Dibujar contenido CENTRADO VERTICALMENTE
    val centerX = bubbleWidth / 2f
    var currentY = padding * 2f
    
    // Emoji
    val emojiX = centerX - (emojiSize / 2f)
    canvas.drawText(emoji, emojiX, currentY + emojiSize, emojiPaint)
    currentY += emojiSize + padding
    
    // Title
    titlePaint.textAlign = Paint.Align.CENTER
    canvas.drawText(title, centerX, currentY + titleBounds.height(), titlePaint)
    currentY += titleBounds.height() + padding
    
    // Date
    secondaryPaint.textAlign = Paint.Align.CENTER
    canvas.drawText(dateText, centerX, currentY + dateBounds.height(), secondaryPaint)
    currentY += dateBounds.height() + (padding * 0.5f)
    
    // Level
    canvas.drawText(levelText, centerX, currentY + levelBounds.height(), secondaryPaint)
    currentY += levelBounds.height() + (padding * 0.5f)
    
    // Slots
    canvas.drawText(slotsText, centerX, currentY + slotsBounds.height(), secondaryPaint)
    
    // 3. Dibujar puntero
    val path = android.graphics.Path().apply {
        moveTo(bubbleWidth / 2f - 7f * density, bubbleHeight)
        lineTo(bubbleWidth / 2f + 7f * density, bubbleHeight)
        lineTo(bubbleWidth / 2f, bubbleHeight + pointerHeight)
        close()
    }
    canvas.drawPath(path, pointerPaint)
    
    // 4. Dibujar Chincheta
    canvas.drawCircle(bubbleWidth / 2f, bubbleHeight + pointerHeight + pinRadius, pinRadius, pinPaint)
    canvas.drawCircle(bubbleWidth / 2f, bubbleHeight + pointerHeight + pinRadius, pinRadius, pinBorderPaint)
    
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

class TriangleShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width / 2f, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}