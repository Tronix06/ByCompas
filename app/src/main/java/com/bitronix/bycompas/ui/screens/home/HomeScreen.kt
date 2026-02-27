package com.bitronix.bycompas.ui.screens.home

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
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
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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
                1 -> RadarScreenContent()
                2 -> ProfileScreenContent(navController)
            }
        }
    }
}

@Composable
fun RadarScreenContent() {
    val context = LocalContext.current

    // 1. Comprobamos si el usuario nos dio el permiso en la pantalla anterior
    val hasLocationPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    // 2. Posición por defecto (España general) por si aún no tenemos ubicación
    val defaultLocation = LatLng(40.4167, -3.7032)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 5f) // Zoom lejano
    }

    // 3. Magia: Si hay permiso, pedimos la ubicación real y centramos el mapa
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val myLocation = LatLng(location.latitude, location.longitude)
                    // Centramos la cámara en tu ubicación con un zoom cercano (14f)
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(myLocation, 14f)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            // 4. ¡Encendemos el puntito azul de Google Maps y el botón de centrar!
            properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
            uiSettings = MapUiSettings(myLocationButtonEnabled = hasLocationPermission, zoomControlsEnabled = true)
        )

        ExtendedFloatingActionButton(
            onClick = { /* Abrir filtros */ },
            icon = { Icon(Icons.Filled.LocationOn, "Filtro") },
            text = { Text("Filtrar Deporte") },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
        )
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
                        if (sportsFromDb != null) {
                            userSports = sportsFromDb
                        }
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
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Person, contentDescription = "Foto", tint = Color.White, modifier = Modifier.size(60.dp))
        }
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
                Button(onClick = { /* Abrir ventana de añadir deportes */ }) {
                    Text("Gestionar mis deportes")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Distancia del Radar: ${radarRadius.toInt()} km", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Recibir notificaciones de eventos deportivos en este radio.", fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))

                Slider(
                    value = radarRadius,
                    onValueChange = { radarRadius = it },
                    valueRange = 1f..50f,
                    steps = 49,
                    onValueChangeFinished = {
                        if (currentUser != null) {
                            db.collection("users").document(currentUser.uid)
                                .update("radarRadius", radarRadius.toDouble())
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Distancia guardada", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedButton(
            onClick = {
                auth.signOut()
                navController.navigate("login") {
                    popUpTo(0)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cerrar Sesión", color = Color.Gray)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { showDeleteDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Borrar mi cuenta")
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("¿Estás seguro?") },
            text = { Text("Esta acción es irreversible. Se borrarán todas tus actividades y estadísticas de ByCompas.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (currentUser != null) {
                            db.collection("users").document(currentUser.uid).delete()
                                .addOnSuccessListener {
                                    currentUser.delete().addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            Toast.makeText(context, "Cuenta borrada", Toast.LENGTH_SHORT).show()
                                            navController.navigate("login") { popUpTo(0) }
                                        }
                                    }
                                }
                        }
                        showDeleteDialog = false
                    }
                ) {
                    Text("Sí, borrar cuenta", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}