package com.bitronix.bycompa.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.animation.togetherWith
import androidx.navigation.NavController
import com.bitronix.bycompa.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, initialTab: String? = null, initialSubTab: Int = 0) {
    // Estado para la sección activa. Si viene una pestaña por parámetro la usamos, si no Inicio.
    var selectedTab by remember { mutableStateOf(initialTab ?: Screen.Inicio.route) }
    // Estado para ocultar la barra (usado por Perfil -> Ajustes)
    var isBottomBarVisible by remember { mutableStateOf(true) }

    val bottomMenuItems = listOf(
        Screen.Inicio, Screen.Buscar, Screen.Radar, Screen.Chats, Screen.Perfil
    )

    Scaffold(
        // Dejamos el bottomBar del Scaffold vacío para que la barra flote de verdad sobre el contenido
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Contenido de las pestañas
            androidx.compose.animation.AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) togetherWith
                    androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(300))
                },
                label = "TabTransition"
            ) { targetTab ->
                Box(modifier = Modifier.padding(bottom = 0.dp)) { // Sin padding inferior para que el contenido pase por debajo
                    when (targetTab) {
                        Screen.Inicio.route -> InicioScreen(navController) { selectedTab = it }
                        Screen.Buscar.route -> BuscarScreen(navController)
                        Screen.Radar.route -> RadarScreen(navController)
                        Screen.Chats.route -> ChatsScreen(navController, initialPage = initialSubTab)
                        Screen.Perfil.route -> ProfileScreen(navController) { isBottomBarVisible = !it }
                    }
                }
            }

            // BARRA FLOTANTE (Con animación de ocultar/mostrar)
            androidx.compose.animation.AnimatedVisibility(
                visible = isBottomBarVisible,
                enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }) + androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }) + androidx.compose.animation.fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Barra Base (Blanca Sólida Premium)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        tonalElevation = 0.dp,
                        shadowElevation = 12.dp,
                        color = Color.White,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                            Color.Transparent
                                        ),
                                        center = Offset.Infinite
                                    )
                                )
                        )
                    }

                    // Contenido Interactivo
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        bottomMenuItems.forEach { screen ->
                            val selected = selectedTab == screen.route
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(
                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                        indication = null
                                    ) { selectedTab = screen.route },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Box(
                                            modifier = Modifier
                                                .size(80.dp)
                                                .background(
                                                    Brush.radialGradient(
                                                        colors = listOf(
                                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                                            Color.Transparent
                                                        )
                                                    )
                                                )
                                        )
                                        
                                        Box(
                                            modifier = Modifier
                                                .size(72.dp)
                                                .shadow(12.dp, CircleShape, ambientColor = MaterialTheme.colorScheme.primary, spotColor = MaterialTheme.colorScheme.primary)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.linearGradient(
                                                        colors = listOf(
                                                            MaterialTheme.colorScheme.primary,
                                                            Color(0xFF1565C0)
                                                        )
                                                    )
                                                )
                                                .border(1.5.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    imageVector = screen.icon,
                                                    contentDescription = screen.title,
                                                    modifier = Modifier.size(26.dp),
                                                    tint = Color.White
                                                )
                                                Text(
                                                    text = screen.title,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = screen.title,
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}