package com.bitronix.bycompas.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    // CAMBIO: Empezamos en el índice 1 (que ahora es el Radar)
    var selectedItem by remember { mutableIntStateOf(1) }

    // CAMBIO: Reordenamos las pestañas para que Radar esté en el centro
    val items = listOf("Partidos", "Radar", "Perfil")
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
            // CAMBIO: Actualizamos a qué número corresponde cada ventana
            when (selectedItem) {
                0 -> MatchesScreenContent() // Partidos (Izquierda)
                1 -> RadarScreenContent()   // Radar (Centro)
                2 -> ProfileScreenContent() // Perfil (Derecha)
            }
        }
    }
}

@Composable
fun RadarScreenContent() {
    // Posición inicial de la cámara
    val ciudadEjemplo = LatLng(40.416775, -3.703790) // Madrid
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

        // CAMBIO: Botón movido a la parte de arriba, al centro, para no tapar el zoom
        ExtendedFloatingActionButton(
            onClick = { /* Abrir filtros */ },
            icon = { Icon(Icons.Filled.LocationOn, "Filtro") },
            text = { Text("Filtrar Deporte") },
            modifier = Modifier
                .align(Alignment.TopCenter) // Lo ponemos arriba
                .padding(top = 16.dp)       // Con un poco de margen
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
        Text("Tus Partidos", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Aquí verás los partidos en los que estás inscrito.", fontSize = 16.sp)
    }
}

@Composable
fun ProfileScreenContent() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Text("Mi Perfil de Deportista", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Nivel: ⭐⭐⭐⭐⭐", fontSize = 20.sp)
    }
}