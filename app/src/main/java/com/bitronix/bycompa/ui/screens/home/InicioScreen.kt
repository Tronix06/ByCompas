package com.bitronix.bycompa.ui.screens.home

import com.bitronix.bycompa.R
import com.bitronix.bycompa.models.EventData
import com.bitronix.bycompa.models.UserData
import coil.compose.AsyncImage
import androidx.navigation.NavController
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import com.bitronix.bycompa.navigation.Screen
import androidx.compose.ui.platform.LocalContext
import com.bitronix.bycompa.database.AppDatabase
import com.bitronix.bycompa.repository.HomeRepository
import com.bitronix.bycompa.utils.LevelManager
import com.bitronix.bycompa.utils.TimeUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.bitronix.bycompa.ui.components.EventDetailSheet
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InicioScreen(navController: NavController, onNavigateToTab: (String) -> Unit) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser
    
    val repository = remember {
        val eventDao = AppDatabase.getDatabase(context).eventDao()
        HomeRepository(eventDao, db)
    }

    var userData by remember { mutableStateOf<UserData?>(null) }
    var upcomingEvents by remember { mutableStateOf<List<EventData>>(emptyList()) }
    var recommendedEvents by remember { mutableStateOf<List<EventData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Estados para BottomSheet y Detail
    var selectedEvent by remember { mutableStateOf<EventData?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showLevelInfo by remember { mutableStateOf(false) }
    var showChallengeInfo by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while(true) {
            delay(1000)
            currentTime = System.currentTimeMillis()
        }
    }

    LaunchedEffect(currentUser) {
        currentUser?.uid?.let { uid ->
            // Marcar como conectado al entrar a la app
            db.collection("users").document(uid).update("isOnline", true)
            
            db.collection("users").document(uid).addSnapshotListener { snap, _ ->
                userData = snap?.toObject(UserData::class.java)
            }
        }
    }

    DisposableEffect(currentUser, userData?.sports) {
        val uid = currentUser?.uid ?: return@DisposableEffect onDispose {}
        val favSports = userData?.sports ?: emptyList()
        
        isLoading = true

        // Listener 1: Próxima cita (Eventos inscritos)
        val joinedListener = db.collection("events")
            .whereArrayContains("participants", uid)
            .addSnapshotListener { snap, _ ->
                val events = snap?.documents?.mapNotNull { 
                    it.toObject(EventData::class.java)?.copy(id = it.id)
                } ?: emptyList()
                upcomingEvents = events.sortedBy { TimeUtils.getMillisFromDate(it.date, it.time) }
                isLoading = false
            }

        // Listener 2: Recomendados
        val recommendedListener = if (favSports.isNotEmpty()) {
            db.collection("events")
                .whereIn("sport", favSports)
                .limit(10)
                .addSnapshotListener { snap, _ ->
                    val events = snap?.documents?.mapNotNull { 
                        it.toObject(EventData::class.java)?.copy(id = it.id)
                    } ?: emptyList()
                    
                    recommendedEvents = events
                        .filter { it.participants.size < it.availableSlots }
                        .filter { TimeUtils.getMillisFromDate(it.date, it.time) > currentTime }
                        .sortedBy { TimeUtils.getMillisFromDate(it.date, it.time) }
                }
        } else null

        onDispose {
            joinedListener.remove()
            recommendedListener?.remove()
        }
    }

    // Filtrar eventos futuros usando el tiempo actual reactivo
    val nextEvent = upcomingEvents.filter { 
        TimeUtils.getMillisFromDate(it.date, it.time) > currentTime 
    }.firstOrNull()

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding(), bottom = paddingValues.calculateBottomPadding())
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // --- CABECERA PREMIUM ---
                item {
                    HeaderSection(userData)
                }

                // --- PRÓXIMA CITA ---
                item {
                    SectionHeader("Tu próxima cita", onAction = { navController.navigate(Screen.UserEvents.route) })
                    Spacer(Modifier.height(12.dp))
                    if (nextEvent != null) {
                        NextEventTicket(
                            event = nextEvent, 
                            currentTime = currentTime, 
                            navController = navController,
                            onDetailClick = {
                                selectedEvent = it
                                showBottomSheet = true
                            }
                        )
                    } else {
                        EmptyEventCard { onNavigateToTab(Screen.Radar.route) }
                    }
                }

                // --- EXPLORAR RECOMENDADOS ---
                item {
                    SectionHeader("Recomendados para ti", actionText = "Ver mapa", onAction = { onNavigateToTab(Screen.Radar.route) })
                    Spacer(Modifier.height(12.dp))
                    if (recommendedEvents.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp)
                        ) {
                            items(recommendedEvents) { event ->
                                PremiumRecommendationCard(event) {
                                    selectedEvent = event
                                    showBottomSheet = true
                                }
                            }
                        }
                    } else {
                        EmptyRecommendationCard { onNavigateToTab(Screen.Radar.route) }
                    }
                }

                // --- ACCIONES RÁPIDAS (Bento Style) ---
                item {
                    BentoActionsSection(navController)
                }
            }
        }
    }

    if (showLevelInfo) {
        LevelInfoDialog(onDismiss = { showLevelInfo = false })
    }

    if (showChallengeInfo) {
        ChallengeInfoDialog(onDismiss = { showChallengeInfo = false })
    }

    if (showBottomSheet && selectedEvent != null) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = Color.Transparent,
            dragHandle = {},
            scrimColor = Color.Black.copy(alpha = 0.5f)
        ) {
            EventDetailSheet(
                event = selectedEvent!!,
                myLocation = null,
                navController = navController,
                snackbarHostState = snackbarHostState
            )
        }
    }
}

@Composable
fun HeaderSection(userData: UserData?) {
    val premiumGradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            Color(0xFF1565C0)
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(premiumGradient, shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .padding(top = 64.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Contenedor de perfil (ajustado al tamaño de la foto para un centrado perfecto)
        Box(
            modifier = Modifier.size(108.dp),
            contentAlignment = Alignment.Center
        ) {
            // Foto de Perfil (Centro absoluto)
            Surface(
                modifier = Modifier.size(108.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.15f),
                shadowElevation = 16.dp,
                border = BorderStroke(3.dp, Color.White.copy(alpha = 0.6f))
            ) {
                AsyncImage(
                    model = userData?.profileImageUrl ?: R.drawable.perfilpordefecto,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }
        
        Spacer(Modifier.height(12.dp))
        
        // [Nombre de Perfil]
        Text(
            text = userData?.name ?: "Usuario",
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            letterSpacing = (-0.5).sp
        )
        
        // [Edad]
        userData?.birthDate?.let { dateStr ->
            val age = calculateAge(dateStr)
            if (age > 0) {
                Text(
                    text = "$age años",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun LevelHub(userData: UserData?, onClick: () -> Unit) {
    val progress = LevelManager.getLevelProgress(userData?.xp ?: 0)
    
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(
                elevation = 8.dp, 
                shape = RoundedCornerShape(24.dp), 
                ambientColor = if (androidx.compose.foundation.isSystemInDarkTheme()) Color.Black else Color.Black.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = if (androidx.compose.foundation.isSystemInDarkTheme()) {
            BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        } else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            Row(
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            colors = if (androidx.compose.foundation.isSystemInDarkTheme()) {
                                listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            } else {
                                listOf(Color.White, Color(0xFFF3F7FF))
                            }
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .alpha(0.2f), // Más sombreado para indicar interactividad
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Nivel ${userData?.level ?: 1}", 
                        fontSize = 16.sp, 
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.fillMaxWidth(0.8f)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress.coerceAtLeast(0.05f))
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(MaterialTheme.colorScheme.primary, Color(0xFF03DAC6))
                                    )
                                )
                        )
                    }
                }
                
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "${userData?.xp ?: 0} XP", 
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Black,
                        color = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFFBB86FC) else MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp
                    )
                }
            }
            
            // Etiqueta de Próximamente (más pequeña)
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text(
                    "Próximamente", 
                    color = Color.White, 
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
        }
    }
}



@Composable
fun WeeklyChallengesWidget(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Row(
                modifier = Modifier
                    .padding(20.dp)
                    .alpha(0.3f), // Un poco más sombreado para indicar interactividad
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("RETO SEMANAL", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("Desbloquea recompensas", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text("Gana XP extra cada semana", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 13.sp)
                }
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { 0f },
                        modifier = Modifier.size(50.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                        strokeWidth = 4.dp
                    )
                }
            }
            
            // Etiqueta de Próximamente
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text(
                    "Próximamente", 
                    color = Color.White, 
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun NextEventTicket(
    event: EventData, 
    currentTime: Long, 
    navController: NavController,
    onDetailClick: (EventData) -> Unit
) {
    val context = LocalContext.current
    val targetMillis = TimeUtils.getMillisFromDate(event.date, event.time)
    val countdown = TimeUtils.formatCountdown(targetMillis)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(
                elevation = 16.dp, 
                shape = RoundedCornerShape(32.dp), 
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), 
                spotColor = MaterialTheme.colorScheme.primary
            )
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(32.dp)),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            Color(0xFF1565C0)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(getSportEmoji(event.sport), fontSize = 38.sp)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = event.title.ifBlank { event.sport }, 
                            color = Color.White, 
                            fontWeight = FontWeight.Black, 
                            fontSize = 18.sp,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = event.sport.uppercase(), 
                            color = Color.White.copy(alpha = 0.7f), 
                            fontSize = 13.sp, 
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    Surface(
                        color = Color(0xFFFFD700),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            countdown, 
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = Color.Black
                        )
                    }
                }
                
                Spacer(Modifier.height(24.dp))
                
                HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                
                Spacer(Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(), 
                    horizontalArrangement = Arrangement.SpaceBetween, 
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Fecha",
                            color = Color.White.copy(alpha = 0.6f), 
                            fontSize = 10.sp, 
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(14.dp), tint = Color.White)
                            Spacer(Modifier.width(6.dp))
                            Text("${event.date} • ${event.time}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                        }
                    }
                    
                    Row {
                        IconButton(
                            onClick = {
                                val uri = "geo:${event.location.latitude},${event.location.longitude}?q=${event.location.latitude},${event.location.longitude}(Evento)"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                                context.startActivity(intent)
                            },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.15f))
                        ) {
                            Icon(Icons.Default.Navigation, null, tint = Color.White)
                        }
                        Spacer(Modifier.width(10.dp))
                        IconButton(
                            onClick = { onDetailClick(event) },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumRecommendationCard(event: EventData, onClick: () -> Unit) {
    var creatorImageUrl by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(event.creatorId) {
        if (event.creatorId.isNotEmpty()) {
            FirebaseFirestore.getInstance().collection("users").document(event.creatorId)
                .get()
                .addOnSuccessListener { doc ->
                    creatorImageUrl = doc.getString("profileImageUrl")
                }
        }
    }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(180.dp)
            .shadow(
                elevation = 12.dp, 
                shape = RoundedCornerShape(32.dp), 
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                spotColor = MaterialTheme.colorScheme.primary
            )
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(32.dp)),
        shape = RoundedCornerShape(32.dp),
        color = Color.Transparent
    ) {
        Box {
            Column(
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                Color(0xFF1565C0)
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(getSportEmoji(event.sport), fontSize = 48.sp)
                }
                Spacer(Modifier.height(14.dp))
                
                // Título del Evento
                Text(
                    text = event.title.ifBlank { event.sport }, 
                    fontWeight = FontWeight.Black, 
                    fontSize = 15.sp, 
                    color = Color.White, 
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                
                // Deporte
                Text(
                    text = event.sport,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                
                Spacer(Modifier.height(14.dp))
                
                // Plazas Disponibles
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Icon(Icons.Default.Groups, null, modifier = Modifier.size(13.dp), tint = Color.White.copy(alpha = 0.8f))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${event.occupiedSlots}/${event.totalSlots} miembros", 
                        color = Color.White.copy(alpha = 0.8f), 
                        fontSize = 11.sp, 
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(Modifier.height(8.dp))
                
                // Fecha y Hora
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Schedule, null, modifier = Modifier.size(12.dp), tint = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${event.date} • ${event.time}", 
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }

            // Imagen del creador abajo a la derecha
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (creatorImageUrl != null) {
                    AsyncImage(
                        model = creatorImageUrl,
                        contentDescription = "Creador",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(14.dp), tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun BentoActionsSection(navController: NavController) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        SectionHeader("Gestión rápida")
        Spacer(Modifier.height(16.dp))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val premiumGradient = Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, Color(0xFF1565C0)))
            
            BentoItem(
                title = "Mis Eventos",
                subtitle = "Gestiona tus próximos eventos",
                icon = Icons.Default.Event,
                gradient = premiumGradient,
                isSmall = true,
                modifier = Modifier.fillMaxWidth()
            ) { navController.navigate(Screen.UserEvents.route) }
            
            BentoItem(
                title = "Solicitudes",
                subtitle = "Peticiones de otros compas",
                icon = Icons.Default.PersonAdd,
                gradient = premiumGradient,
                isSmall = true,
                modifier = Modifier.fillMaxWidth()
            ) { navController.navigate(Screen.RequestManagement.route) }
            
            BentoItem(
                title = "Historial",
                subtitle = "Tus eventos finalizados",
                icon = Icons.Default.History,
                gradient = premiumGradient,
                isSmall = true,
                modifier = Modifier.fillMaxWidth()
            ) { navController.navigate(Screen.EventHistory.route) }
        }
    }
}

@Composable
fun BentoItem(
    title: String, 
    subtitle: String? = null,
    icon: ImageVector, 
    gradient: Brush, 
    isSmall: Boolean = false, 
    modifier: Modifier = Modifier, 
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(if (isSmall) 80.dp else 196.dp)
            .shadow(
                elevation = 8.dp, 
                shape = RoundedCornerShape(24.dp),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                spotColor = MaterialTheme.colorScheme.primary
            )
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                Modifier.fillMaxSize(), 
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(30.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        title, 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 16.sp, 
                        color = Color.White
                    )
                    if (subtitle != null) {
                        Text(
                            subtitle, 
                            fontSize = 12.sp, 
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.ChevronRight, null, tint = Color.White.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
fun LevelInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoGraph, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text("Sistema de Niveles")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "¡Muy pronto podrás ganar experiencia (XP) participando en eventos!",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                
                InfoRow(Icons.Default.Sports, "Juega eventos para subir de rango.")
                InfoRow(Icons.Default.MilitaryTech, "Desbloquea insignias exclusivas.")
                InfoRow(Icons.Default.CardGiftcard, "Obtén recompensas de nuestros colaboradores.")
                
                Text(
                    "Estamos terminando de pulir los algoritmos para que tu esfuerzo se vea recompensado como merece.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("¡Genial!")
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
fun ChallengeInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EmojiEvents, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text("Retos Semanales")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "¡Prepárate para competir y ganar premios increíbles cada semana!",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                
                InfoRow(Icons.Default.Flag, "Completa objetivos variados (eventos, victorias...).")
                InfoRow(Icons.Default.FlashOn, "Suma bonificaciones de XP masivas.")
                InfoRow(Icons.Default.EmojiEvents, "Consigue trofeos para tu perfil público.")
                
                Text(
                    "Los retos se renovarán cada lunes. ¿Serás capaz de completarlos todos?",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("¡Acepto el reto!")
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
fun InfoRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
        Spacer(Modifier.width(12.dp))
        Text(text, fontSize = 13.sp)
    }
}

fun getSportEmoji(sport: String): String {
    return when {
        sport.contains("Fútbol", ignoreCase = true) -> "⚽"
        sport.contains("Pádel", ignoreCase = true) -> "🎾"
        sport.contains("Tenis", ignoreCase = true) -> "🥎"
        sport.contains("Baloncesto", ignoreCase = true) -> "🏀"
        sport.contains("Running", ignoreCase = true) -> "🏃"
        sport.contains("Ciclismo", ignoreCase = true) -> "🚴"
        sport.contains("Natación", ignoreCase = true) -> "🏊"
        sport.contains("Voleibol", ignoreCase = true) -> "🏐"
        sport.contains("Gym", ignoreCase = true) -> "🏋️"
        sport.contains("Fitness", ignoreCase = true) -> "💪"
        sport.contains("Crossfit", ignoreCase = true) -> "⛓️"
        sport.contains("Yoga", ignoreCase = true) -> "🧘"
        sport.contains("Pilates", ignoreCase = true) -> "🤸"
        sport.contains("Senderismo", ignoreCase = true) -> "🥾"
        sport.contains("Golf", ignoreCase = true) -> "🏌️"
        sport.contains("Boxeo", ignoreCase = true) -> "🥊"
        sport.contains("Skate", ignoreCase = true) -> "🛹"
        sport.contains("Ajedrez", ignoreCase = true) -> "♟️"
        else -> "🏆"
    }
}

private fun calculateAge(birthDate: String?): Int {
    if (birthDate.isNullOrEmpty()) return 0
    return try {
        val parts = birthDate.split("/")
        if (parts.size < 3) return 0
        val day = parts[0].toInt()
        val month = parts[1].toInt()
        val year = parts[2].toInt()
        
        val dob = Calendar.getInstance()
        dob.set(year, month - 1, day)
        
        val today = Calendar.getInstance()
        var age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
        
        // Comprobación precisa de mes y día para asegurar que el cambio ocurre el mismo día del cumple
        if (today.get(Calendar.MONTH) < dob.get(Calendar.MONTH) || 
            (today.get(Calendar.MONTH) == dob.get(Calendar.MONTH) && today.get(Calendar.DAY_OF_MONTH) < dob.get(Calendar.DAY_OF_MONTH))) {
            age--
        }
        age
    } catch (e: Exception) {
        0
    }
}

@Composable
fun SectionHeader(title: String, actionText: String = "Ver todo", onAction: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Black)
        if (onAction != null) {
            Text(
                actionText, 
                modifier = Modifier.clickable { onAction() },
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun EmptyEventCard(onExplore: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.SportsTennis, null, modifier = Modifier.size(48.dp), tint = Color.LightGray)
            Spacer(Modifier.height(16.dp))
            Text("Estás libre de eventos", fontWeight = FontWeight.Black, fontSize = 18.sp)
            Text("¡Explora el mapa y únete a un compa!", color = Color.Gray, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            Button(onClick = onExplore, shape = RoundedCornerShape(12.dp)) {
                Text("Explorar Radar")
            }
        }
    }
}

@Composable
fun EmptyRecommendationCard(onExplore: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(40.dp), tint = Color.LightGray)
            Spacer(Modifier.height(12.dp))
            Text("Sin recomendaciones nuevas", fontWeight = FontWeight.Black, fontSize = 16.sp)
            Text("Añade tus deportes favoritos en tu perfil para recibir sugerencias personalizadas", color = Color.Gray, textAlign = TextAlign.Center, fontSize = 13.sp)
        }
    }
}