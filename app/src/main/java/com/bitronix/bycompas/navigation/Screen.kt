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
    object IndividualChat : Screen("chat/{chatId}/{otherName}", "Chat", Icons.Filled.Chat)
    object LocationPicker : Screen("location_picker", "Ubicación", Icons.Filled.Map)
    object UserEvents : Screen("user_events", "Mis Eventos", Icons.Filled.EventAvailable)
    object RequestManagement : Screen("request_management", "Solicitudes", Icons.Filled.PendingActions)
    object EventHistory : Screen("event_history", "Historial", Icons.Filled.History)
    object Rating : Screen("rating/{chatId}/{eventName}", "Valorar", Icons.Filled.Star)
}