package com.bitronix.bycompas.ui.screens.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.bitronix.bycompas.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    // Estado para la sección activa. Empezamos en Radar por defecto.
    var selectedTab by remember { mutableStateOf(Screen.Radar.route) }

    val bottomMenuItems = listOf(
        Screen.Inicio, Screen.Buscar, Screen.Radar, Screen.Chats, Screen.Perfil
    )

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
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                bottomMenuItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title, fontSize = 10.sp) },
                        selected = selectedTab == screen.route,
                        onClick = { selectedTab = screen.route }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                Screen.Inicio.route -> InicioScreen()
                Screen.Buscar.route -> BuscarScreen()
                Screen.Radar.route -> RadarScreen(navController)
                Screen.Chats.route -> ChatsScreen()
                Screen.Perfil.route -> ProfileScreen(navController)
            }
        }
    }
}