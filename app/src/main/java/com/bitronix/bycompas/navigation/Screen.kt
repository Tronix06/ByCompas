package com.bitronix.bycompas.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Inicio : Screen("inicio", "Inicio", Icons.Filled.Home)
    object Buscar : Screen("buscar", "Buscar", Icons.Filled.Search)
    object Radar : Screen("radar", "Radar", Icons.Filled.LocationOn)
    object Chats : Screen("chats", "Chats", Icons.AutoMirrored.Filled.Send) // Cambiado aquí
    object Perfil : Screen("perfil", "Perfil", Icons.Filled.Person)
    object CreateEvent : Screen("create_event", "Crear", Icons.Filled.Add)
    object Login : Screen("login", "Login", Icons.Filled.Lock)
}