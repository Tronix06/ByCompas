package com.bitronix.bycompas.ui.screens.events

import android.Manifest
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.bitronix.bycompas.utils.AppConstants
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var searchQuery by remember { mutableStateOf("") }
    var mapType by remember { mutableStateOf(MapType.NORMAL) }
    var expandedMapType by remember { mutableStateOf(false) }
    var myLocation by remember { mutableStateOf<LatLng?>(null) }
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(40.4168, -3.7038), 5f) // Inicial en España, se actualizará a la real
    }

    // Gestionar ubicación del usuario
    LaunchedEffect(Unit) {
        try {
            LocationServices.getFusedLocationProviderClient(context).lastLocation.addOnSuccessListener { loc ->
                loc?.let {
                    val latLng = LatLng(it.latitude, it.longitude)
                    myLocation = latLng
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 16f)
                }
            }
        } catch (e: SecurityException) {
            // Sin permisos, se queda en la vista general
        }
    }

    // Geocoder para buscar direcciones de forma gratuita
    val geocoder = remember { Geocoder(context, Locale.getDefault()) }

    fun searchAddress() {
        if (searchQuery.isBlank()) return
        scope.launch {
            try {
                val addresses = withContext(Dispatchers.IO) {
                    geocoder.getFromLocationName(searchQuery, 1)
                }
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val target = LatLng(address.latitude, address.longitude)
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(target, 16f)
                    )
                }
            } catch (e: Exception) {
                // Manejar error de red o geocodificación
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seleccionar Ubicación", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // MAPA
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = true,
                    mapType = mapType
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false
                )
            )

            // --- COLUMNA DE CONTROLES DEL MAPA (Derecha) ---
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.End
            ) {
                // 1. Botón Mi Ubicación
                MapControlButton(Icons.Default.MyLocation) {
                    myLocation?.let {
                        scope.launch {
                            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(it, 16f))
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

                // 3. Botón Zoom In
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

            // CHINCHETA CENTRAL ESTILO UBER
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Posición central",
                tint = Color.Red,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp)
                    .padding(bottom = 24.dp)
            )

            // BARRA DE BÚSQUEDA SUPERIOR
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 10.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar calle, ciudad o sitio...", fontSize = 14.sp, color = Color.Gray) },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Borrar", modifier = Modifier.size(18.dp), tint = Color.Gray)
                        }
                    }
                    IconButton(onClick = { searchAddress() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward, 
                            contentDescription = "Ir", 
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // BOTÓN DE CONFIRMACIÓN INFERIOR (Estilo Azul Premium)
            Surface(
                onClick = {
                    val target = cameraPositionState.position.target
                    navController.previousBackStackEntry?.savedStateHandle?.set("lat", target.latitude)
                    navController.previousBackStackEntry?.savedStateHandle?.set("lng", target.longitude)
                    navController.popBackStack()
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(60.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color.Transparent,
                shadowElevation = 12.dp
            ) {
                val premiumGradient = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1565C0),
                        Color(0xFF1E88E5)
                    )
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(premiumGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color.White)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "CONFIRMAR UBICACIÓN", 
                            fontSize = 16.sp, 
                            fontWeight = FontWeight.Black, 
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
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
