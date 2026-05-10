package com.bitronix.bycompa.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.background
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.bitronix.bycompa.models.EventData
import com.bitronix.bycompa.models.UserData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestManagementScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid ?: ""

    var receivedEvents by remember { mutableStateOf<List<EventData>>(emptyList()) }
    var sentEvents by remember { mutableStateOf<List<EventData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(1) } // Default to Received

    val premiumGradient = androidx.compose.ui.graphics.Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            Color(0xFF1565C0)
        )
    )

    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { 2 }, initialPage = 1)

    LaunchedEffect(pagerState.currentPage) {
        selectedTab = pagerState.currentPage
    }

    LaunchedEffect(selectedTab) {
        if (pagerState.currentPage != selectedTab) {
            pagerState.animateScrollToPage(selectedTab)
        }
    }

    LaunchedEffect(Unit) {
        isLoading = true
        // Recibidas: Eventos creados por mí con solicitudes pendientes
        db.collection("events")
            .whereEqualTo("creatorId", uid)
            .addSnapshotListener { snap, _ ->
                val allEvents = snap?.documents?.mapNotNull { 
                    it.toObject(EventData::class.java)?.copy(id = it.id)
                } ?: emptyList()
                receivedEvents = allEvents.filter { it.pendingRequests.isNotEmpty() }
            }

        // Enviadas: Eventos donde mi ID está en pendingRequests
        db.collection("events")
            .whereArrayContains("pendingRequests", uid)
            .addSnapshotListener { snap, _ ->
                sentEvents = snap?.documents?.mapNotNull { 
                    it.toObject(EventData::class.java)?.copy(id = it.id)
                } ?: emptyList()
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Transparent
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(premiumGradient)
                        .padding(top = 40.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                        }
                        Text(
                            "Solicitudes", 
                            color = Color.White, 
                            fontWeight = FontWeight.Black, 
                            fontSize = 22.sp
                        )
                    }

                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                height = 4.dp,
                                color = Color.White
                            )
                        },
                        divider = {}
                    ) {
                        Tab(
                            selected = selectedTab == 0, 
                            onClick = { selectedTab = 0 }
                        ) {
                            Text(
                                "ENVIADAS", 
                                modifier = Modifier.padding(16.dp), 
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = if (selectedTab == 0) Color.White else Color.White.copy(alpha = 0.6f)
                            )
                        }
                        Tab(
                            selected = selectedTab == 1, 
                            onClick = { selectedTab = 1 }
                        ) {
                            Text(
                                "RECIBIDAS", 
                                modifier = Modifier.padding(16.dp), 
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = if (selectedTab == 1) Color.White else Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                androidx.compose.foundation.pager.HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    val currentList = if (pageIndex == 0) sentEvents else receivedEvents
                    
                    if (currentList.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(32.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.size(80.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                ) {
                                    Icon(
                                        if (pageIndex == 0) Icons.Default.Person else Icons.Default.Check, 
                                        null, 
                                        modifier = Modifier.padding(20.dp), 
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(Modifier.height(24.dp))
                                Text(
                                    if (pageIndex == 0) "Sin solicitudes enviadas" else "¡Todo al día!", 
                                    fontWeight = FontWeight.Black, 
                                    fontSize = 22.sp,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    if (pageIndex == 0) "No has solicitado unirte a ningún evento aún." else "No tienes solicitudes pendientes en tus eventos.",
                                    color = Color.Gray, 
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(), 
                            contentPadding = PaddingValues(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (pageIndex == 1) { // RECIBIDAS
                                receivedEvents.forEach { event ->
                                    item {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.padding(top = 8.dp)
                                        ) {
                                            Text(
                                                text = "En: ${if (event.title.isNotBlank()) "${event.title} • " else ""}${event.sport} (${event.date})",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    items(event.pendingRequests) { requesterId ->
                                        RequestItem(event.id, requesterId, db, premiumGradient)
                                    }
                                }
                            } else { // ENVIADAS
                                items(sentEvents) { event ->
                                    SentRequestItem(event, uid, db)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SentRequestItem(event: EventData, myUid: String, db: FirebaseFirestore) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(24.dp), ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F4F9))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                val icon = when {
                    event.sport.contains("Fútbol", ignoreCase = true) || event.sport.contains("⚽") -> "⚽"
                    event.sport.contains("Pádel", ignoreCase = true) || event.sport.contains("🎾") -> "🎾"
                    else -> "🏆"
                }
                Text(icon, fontSize = 24.sp)
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(event.title.ifBlank { event.sport }, fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.Black)
                Text("${event.date} • ${event.time}", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                Text("Esperando confirmación...", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }

            Surface(
                onClick = {
                    db.collection("events").document(event.id).update(
                        "pendingRequests", FieldValue.arrayRemove(myUid)
                    )
                },
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = Color.Red.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Close, "Cancelar", tint = Color.Red, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun RequestItem(eventId: String, requesterId: String, db: FirebaseFirestore, premiumGradient: androidx.compose.ui.graphics.Brush) {
    var requesterData by remember { mutableStateOf<UserData?>(null) }
    
    LaunchedEffect(requesterId) {
        db.collection("users").document(requesterId).get().addOnSuccessListener {
            requesterData = it.toObject(UserData::class.java)
        }
    }

    if (requesterData == null) return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f)),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                AsyncImage(
                    model = requesterData?.profileImageUrl ?: com.bitronix.bycompa.R.drawable.perfilpordefecto,
                    contentDescription = null,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
                Surface(
                    modifier = Modifier.align(Alignment.BottomEnd).size(16.dp),
                    shape = CircleShape,
                    color = Color(0xFF4CAF50),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color.White)
                ) {}
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    requesterData?.name ?: "Usuario", 
                    fontWeight = FontWeight.Black, 
                    fontSize = 16.sp
                )
                Text(
                    "Nivel ${requesterData?.level ?: 1} • ★ ${requesterData?.rating ?: 5.0}", 
                    fontSize = 13.sp, 
                    color = Color.Gray
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Botón Rechazar
                Surface(
                    onClick = {
                        db.collection("events").document(eventId).update(
                            "pendingRequests", FieldValue.arrayRemove(requesterId)
                        )
                    },
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Red.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Close, null, tint = Color.Red, modifier = Modifier.size(20.dp))
                    }
                }
                
                // Botón Aceptar
                Surface(
                    onClick = {
                        db.collection("events").document(eventId).update(
                            "pendingRequests", FieldValue.arrayRemove(requesterId),
                            "participants", FieldValue.arrayUnion(requesterId),
                            "occupiedSlots", FieldValue.increment(1),
                            "availableSlots", FieldValue.increment(-1)
                        ).addOnSuccessListener {
                            db.collection("chats").document(eventId)
                                .update("participants", FieldValue.arrayUnion(requesterId))
                        }
                    },
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(premiumGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
