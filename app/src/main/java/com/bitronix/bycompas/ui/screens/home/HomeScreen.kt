package com.bitronix.bycompas.ui.screens.home

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
import androidx.navigation.NavController
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    var selectedItem by remember { mutableIntStateOf(1) } // 1 es el Radar (Centro)
    val items = listOf("Eventos", "Radar", "Perfil") // Cambiado de Partidos a Eventos
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
    val ciudadEjemplo = LatLng(40.416775, -3.703790)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(ciudadEjemplo, 12f)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            Marker(
                state = MarkerState(position = ciudadEjemplo),
                title = "Tu ubicación",
                snippet = "Buscando deportistas cerca"
            )
        }

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
        // Textos adaptados para ser inclusivos con cualquier deporte
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
        // --- 1. CABECERA DE PERFIL ---
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

        // --- 2. GESTIÓN DE DEPORTES (Enfoque Multideporte) ---
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Mis Deportes", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                // Ejemplos visuales de que soporta múltiples disciplinas y métricas
                Text("- Pádel (Nivel Intermedio)")
                Text("- Running (Ritmo 5:30 min/km)")
                Text("- Baloncesto (Avanzado)")
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { /* Abrir ventana de añadir deportes */ }) {
                    Text("Gestionar mis deportes")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- 3. CONFIGURACIÓN DEL RADAR ---
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Distancia del Radar: ${radarRadius.toInt()} km", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                // CORRECCIÓN APLICADA AQUÍ:
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

        // --- 4. ZONA DE PELIGRO ---
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