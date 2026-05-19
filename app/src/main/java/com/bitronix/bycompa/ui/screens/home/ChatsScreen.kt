package com.bitronix.bycompa.ui.screens.home

import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.navigation.NavController
import androidx.compose.ui.window.Dialog
import com.google.firebase.firestore.FieldValue

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.border
import androidx.compose.foundation.pager.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.bitronix.bycompa.models.UserData
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.launch

// --- MODELOS DE DATOS ---
data class ChatRequest(
    val id: String = "",
    val from: String = "",
    val to: String = "",
    val status: String = "pending",
    val senderName: String = "Cargando...",
    val senderImage: String? = null
)

data class ChatSummary(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: String = "",
    val otherUserName: String = "Compas",
    val otherUserImage: String? = null,
    val unreadCount: Int = 0,
    val isGroup: Boolean = false,
    val groupName: String? = null,
    val groupImageUrl: String? = null,
    val sport: String? = null,
    val eventDate: String? = null,
    val eventTime: String? = null,
    val warningSent: Boolean = false,
    val isExpired: Boolean = false,
    val alreadyRated: Boolean = false,
    val isPinned: Boolean = false,
    val isHidden: Boolean = false,
    val updatedAt: Long = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(navController: NavController, initialPage: Int = 0) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    var currentUser by remember { mutableStateOf(auth.currentUser) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener {
            currentUser = it.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }
    
    val scope = rememberCoroutineScope()
    val tabs = listOf("Individual", "Grupos", "Temporales")
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { tabs.size })
    val selectedTab = pagerState.currentPage
    var enlargedImageUrl by remember { mutableStateOf<String?>(null) }
    var showIndividualDialog by remember { mutableStateOf(false) }
    var showGroupDialog by remember { mutableStateOf(false) }
    
    // Estados para nuevas funciones
    var selectedChatId by remember { mutableStateOf<String?>(null) }
    var selectedChatIsEvent by remember { mutableStateOf(false) }
    var selectedChatIsExpired by remember { mutableStateOf(false) }
    var showDeleteWarning by remember { mutableStateOf(false) }
    var showFabMenu by remember { mutableStateOf(false) }
    var showAllChatsSheet by remember { mutableStateOf(false) }

    val premiumGradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            Color(0xFF1565C0)
        )
    )

    Scaffold(
        floatingActionButtonPosition = FabPosition.Center,
        topBar = {
            if (selectedChatId != null) {
                // Barra Contextual simplificada
                TopAppBar(
                    title = { Text("Seleccionado", fontSize = 16.sp) },
                    navigationIcon = {
                        IconButton(onClick = { selectedChatId = null }) {
                            Icon(Icons.Default.Close, null)
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            val chatId = selectedChatId ?: return@IconButton
                            val myUid = currentUser?.uid ?: return@IconButton
                            
                            db.collection("chats").document(chatId).get().addOnSuccessListener { chatDoc ->
                                val pBy = chatDoc.get("pinnedBy") as? Map<String, Any> ?: emptyMap()
                                val isCurrentlyPinned = pBy[myUid] == true
                                
                                if (isCurrentlyPinned) {
                                    db.collection("chats").document(chatId).update("pinnedBy.$myUid", false)
                                    selectedChatId = null
                                } else {
                                    db.collection("chats").whereArrayContains("participants", myUid).get().addOnSuccessListener { snap ->
                                        val pinnedCount = snap.documents.count { doc -> 
                                            val docPby = doc.get("pinnedBy") as? Map<String, Any> ?: emptyMap()
                                            docPby[myUid] == true 
                                        }
                                        if (pinnedCount >= 3) {
                                            Toast.makeText(context, "Puedes fijar un máximo de 3 chats", Toast.LENGTH_SHORT).show()
                                        } else {
                                            db.collection("chats").document(chatId).update("pinnedBy.$myUid", true)
                                            selectedChatId = null
                                        }
                                    }
                                }
                            }
                        }) {
                            Icon(Icons.Default.PushPin, null, tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = {
                            if (selectedChatIsEvent && !selectedChatIsExpired) {
                                showDeleteWarning = true
                            } else {
                                selectedChatId?.let { id ->
                                    db.collection("chats").document(id).update("hiddenBy.${currentUser?.uid}", true)
                                    selectedChatId = null
                                }
                            }
                        }) {
                            Icon(Icons.Default.Delete, null, tint = Color.Red)
                        }
                    }
                )
            } else {
                // --- CABECERA PREMIUM CON TABS ---
                Surface(
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    color = Color.Transparent,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(premiumGradient)
                            .padding(top = 16.dp)
                    ) {
                        // Título de la sección
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Mis Chats", 
                                fontSize = 22.sp, 
                                fontWeight = FontWeight.Black, 
                                color = Color.White
                            )
                        }

                        // Selector de pestañas (Tabs) integrado
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = Color.Transparent,
                            contentColor = Color.White,
                            divider = {},
                            indicator = { tabPositions ->
                                if (selectedTab < tabPositions.size) {
                                    TabRowDefaults.SecondaryIndicator(
                                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                        color = Color.White,
                                        height = 3.dp
                                    )
                                }
                            }
                        ) {
                            tabs.forEachIndexed { index, title ->
                                val icon = when(index) {
                                    0 -> Icons.Default.Person
                                    1 -> Icons.Default.Groups
                                    else -> Icons.Default.Timer
                                }
                                Tab(
                                    selected = selectedTab == index,
                                    onClick = { 
                                        scope.launch { pagerState.animateScrollToPage(index) }
                                    },
                                    text = { 
                                        Text(
                                            title, 
                                            fontSize = 12.sp, 
                                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                                        ) 
                                    },
                                    icon = { Icon(icon, null, modifier = Modifier.size(20.dp)) },
                                    selectedContentColor = Color.White,
                                    unselectedContentColor = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (selectedTab != 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 90.dp) // Elevado para librar la barra flotante
                ) {
                    // --- BOTÓN INVITAR (Abajo a la Izquierda) - Visible en Individuales (0) y Grupos (1) ---
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .size(56.dp)
                            .shadow(12.dp, CircleShape, ambientColor = MaterialTheme.colorScheme.primary, spotColor = MaterialTheme.colorScheme.primary)
                            .clip(CircleShape)
                            .background(premiumGradient)
                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                            .clickable { 
                                if (selectedTab == 0) showIndividualDialog = true
                                else showGroupDialog = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Invitar",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // --- BOTÓN AGENDA (Abajo a la Derecha) - SOLO en Individuales (0) ---
                    if (selectedTab == 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(56.dp)
                                .shadow(12.dp, CircleShape, ambientColor = MaterialTheme.colorScheme.primary, spotColor = MaterialTheme.colorScheme.primary)
                                .clip(CircleShape)
                                .background(premiumGradient)
                                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                .clickable { showAllChatsSheet = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = "Agenda",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.Top
            ) { page ->
                when(page) {
                    0 -> ChatListIndividual(
                        navController = navController, 
                        onImageClick = { url: String -> enlargedImageUrl = url },
                        selectedChatId = selectedChatId,
                        onChatLongClick = { id, isEv, isExp -> 
                            selectedChatId = id 
                            selectedChatIsEvent = isEv
                            selectedChatIsExpired = isExp
                        },
                        currentUser = currentUser
                    )
                    1 -> ChatListGrupos(
                        navController = navController, 
                        onImageClick = { url: String -> enlargedImageUrl = url },
                        currentUser = currentUser
                    )
                    2 -> ChatListTemporales(
                        navController = navController, 
                        onImageClick = { url: String -> enlargedImageUrl = url },
                        selectedChatId = selectedChatId,
                        onChatLongClick = { id, isEv, isExp -> 
                            selectedChatId = id 
                            selectedChatIsEvent = isEv
                            selectedChatIsExpired = isExp
                        },
                        currentUser = currentUser
                    )
                }
            }
        }
    }

    if (showIndividualDialog) NewChatRequestDialog(onDismiss = { showIndividualDialog = false })
    if (showGroupDialog) NewGroupDialog(onDismiss = { showGroupDialog = false })
    
    if (showDeleteWarning) {
        AlertDialog(
            onDismissRequest = { showDeleteWarning = false },
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = Color(0xFFFFB300))
                    Spacer(Modifier.width(8.dp))
                    Text("Evento en curso", fontWeight = FontWeight.Black)
                }
            },
            text = { Text("No puedes eliminar este evento todavía porque sigue en curso. En cuanto pasen 24h después de la fecha y hora del evento podrás eliminarlo.") },
            confirmButton = {
                TextButton(onClick = { showDeleteWarning = false; selectedChatId = null }) {
                    Text("Entendido", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showAllChatsSheet) {
        AllChatsSheet(
            onDismiss = { showAllChatsSheet = false }, 
            navController = navController,
            currentUser = currentUser
        )
    }
    
    // Visor de Imagen Ampliada
    enlargedImageUrl?.let { url ->
        FullScreenImageDialog(imageUrl = url, onDismiss = { enlargedImageUrl = null })
    }
}

// --- SECCIÓN INDIVIDUAL ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListIndividual(
    navController: NavController, 
    onImageClick: (String) -> Unit,
    selectedChatId: String?,
    onChatLongClick: (String, Boolean, Boolean) -> Unit,
    currentUser: FirebaseUser?
) {
    val db = FirebaseFirestore.getInstance()
    // Ya no lo pedimos aquí, lo recibimos por parámetro

    var pendingRequests by remember { mutableStateOf<List<ChatRequest>>(emptyList()) }
    var sentRequests by remember { mutableStateOf<List<ChatRequest>>(emptyList()) }
    var activeChats by remember { mutableStateOf<List<ChatSummary>>(emptyList()) }
    
    // Almacén reactivo de perfiles (Nombre, Imagen, EnLinea)
    val userProfiles = remember { mutableStateMapOf<String, Triple<String, String?, Boolean>>() }
    
    var showSentSheet by remember { mutableStateOf(false) }
    var showPendingSheet by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser) {
        val user = currentUser ?: return@LaunchedEffect
        
        fun loadProfile(uid: String) {
            if (uid.isEmpty() || userProfiles.containsKey(uid)) return
            
            // Ponemos un placeholder para no lanzar el listener 2 veces por error
            userProfiles[uid] = Triple("Compas", null, false)
            
            db.collection("users").document(uid).addSnapshotListener { d, _ ->
                if (d != null && d.exists()) {
                    val name = d.getString("name") ?: "Compas"
                    val img = d.getString("profileImageUrl")
                    val isOnline = d.getBoolean("isOnline") ?: false
                    userProfiles[uid] = Triple(name, img, isOnline)
                }
            }
        }

        // 1. Solicitudes Recibidas
        db.collection("chat_requests").whereEqualTo("to", user.uid).whereEqualTo("status", "pending")
            .addSnapshotListener { s, _ ->
                pendingRequests = s?.documents?.mapNotNull { d ->
                    val req = d.toObject(ChatRequest::class.java)?.copy(id = d.id)
                    req?.let { loadProfile(it.from) }
                    req
                } ?: emptyList()
            }

        // 2. Solicitudes Enviadas
                db.collection("chat_requests").whereEqualTo("from", user.uid).whereEqualTo("status", "pending")
            .addSnapshotListener { s, _ ->
                sentRequests = s?.documents?.mapNotNull { d ->
                    val req = d.toObject(ChatRequest::class.java)?.copy(id = d.id)
                    req?.let { loadProfile(it.to) }
                    req
                } ?: emptyList()
            }

        // 3. Chats Individuales
        db.collection("chats").whereArrayContains("participants", user.uid)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    android.util.Log.e("CHATS", "Error en listener: ${error.message}")
                    return@addSnapshotListener
                }
                
                val fetched = snap?.documents?.mapNotNull { doc ->
                    val isGrp = doc.getBoolean("isGroup") ?: false
                    val isEvent = doc.getBoolean("isEventChat") ?: false
                    if (isGrp || isEvent) return@mapNotNull null
                    
                    val participants = doc.get("participants") as? List<String> ?: emptyList()
                    val otherId = participants.firstOrNull { it != user.uid } ?: ""
                    if (otherId.isNotEmpty()) loadProfile(otherId)

                    // Parseo ultra-flexible de números y fechas
                    val unread = (doc.get("unreadCounts") as? Map<String, Any>)?.get(user.uid)
                    val myUnread = (unread as? Number)?.toInt() ?: 0
                    
                    val pBy = doc.get("pinnedBy") as? Map<String, Any> ?: emptyMap()
                    val hBy = doc.get("hiddenBy") as? Map<String, Any> ?: emptyMap()
                    
                    // Soporte para Timestamp o Long (Milisegundos)
                    val time: Long = when(val rawTs = doc.get("updatedAt") ?: doc.get("createdAt")) {
                        is com.google.firebase.Timestamp -> rawTs.toDate().time
                        is Number -> rawTs.toLong()
                        else -> 0L
                    }

                    ChatSummary(
                        id = doc.id,
                        participants = participants,
                        lastMessage = doc.getString("lastMessage") ?: "Chat activo",
                        unreadCount = myUnread,
                        isPinned = pBy[user.uid] == true,
                        isHidden = hBy[user.uid] == true,
                        updatedAt = time
                    )
                } ?: emptyList()
                
                activeChats = fetched.filter { !it.isHidden }
                    .sortedWith(compareByDescending<ChatSummary> { it.isPinned }.thenByDescending { it.updatedAt })
            }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        if (pendingRequests.isNotEmpty()) {
            item {
                Surface(
                    onClick = { showPendingSheet = true },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                ) {
                    Row(Modifier.padding(16.dp, 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Inbox, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Solicitudes recibidas",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.weight(1f))
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        ) {
                            Text(
                                "${pendingRequests.size}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        if (sentRequests.isNotEmpty()) {
            item {
                Surface(
                    onClick = { showSentSheet = true },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                ) {
                    Row(Modifier.padding(16.dp, 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Outbox, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Solicitudes enviadas",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.weight(1f))
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            shape = CircleShape
                        ) {
                            Text(
                                "${sentRequests.size}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        if (pendingRequests.isNotEmpty() || sentRequests.isNotEmpty()) {
            item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
        }

        item { 
            Text(
                "Tus Chats", 
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), 
                fontWeight = FontWeight.Bold, 
                fontSize = 15.sp, 
                color = Color.Gray
            ) 
        }

        items(items = activeChats, key = { it.id }) { chat ->
            val otherId = chat.participants.firstOrNull { it != currentUser?.uid } ?: ""
            val profile = userProfiles[otherId]
            
            ChatItem(
                name = profile?.first ?: "Compas", 
                subtext = chat.lastMessage, 
                imageUrl = profile?.second,
                unreadCount = chat.unreadCount,
                isGroup = false,
                isPinned = chat.isPinned,
                isSelected = selectedChatId == chat.id,
                isOnline = profile?.third == true,
                onClick = {
                    if (selectedChatId != null) {
                        onChatLongClick(chat.id, false, false)
                    } else {
                        navController.navigate("chat/${chat.id}/${profile?.first ?: "Compas"}")
                    }
                },
                onLongClick = { onChatLongClick(chat.id, false, false) },
                onImageClick = { url -> onImageClick(url) }
            )
        }
    }

    if (showSentSheet) {
        SentRequestsSheet(requests = sentRequests, userProfiles = userProfiles, onDismiss = { showSentSheet = false })
    }

    if (showPendingSheet) {
        PendingRequestsSheet(requests = pendingRequests, userProfiles = userProfiles, onDismiss = { showPendingSheet = false })
    }
}

// --- SECCIÓN GRUPOS ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListGrupos(
    navController: NavController, 
    onImageClick: (String) -> Unit,
    currentUser: FirebaseUser?
) {
    val db = FirebaseFirestore.getInstance()
    var groupChats by remember { mutableStateOf<List<ChatSummary>>(emptyList()) }
    var pendingGroupInvites by remember { mutableStateOf<List<Triple<String, String, String?>>>(emptyList()) }
    var showInvitesSheet by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            // Solicitudes de Grupo Pendientes
            db.collection("chats")
                .whereEqualTo("isGroup", true)
                .whereArrayContains("pendingParticipants", user.uid)
                .addSnapshotListener { s, _ ->
                    pendingGroupInvites = s?.documents?.mapNotNull { d ->
                        val name = d.getString("groupName") ?: "Nuevo Grupo"
                        val img = d.getString("groupImageUrl")
                        Triple(d.id, name, img)
                    } ?: emptyList()
                }

            db.collection("chats")
                .whereArrayContains("participants", user.uid)
                .addSnapshotListener { snap, error ->
                    if (error != null) return@addSnapshotListener
                    
                    groupChats = snap?.documents?.mapNotNull { doc ->
                        val isGrp = doc.getBoolean("isGroup") ?: false
                        val isEvent = doc.getBoolean("isEventChat") ?: false
                        if (!isGrp || isEvent) return@mapNotNull null
                        
                        val time: Long = when(val rawTs = doc.get("updatedAt") ?: doc.get("createdAt")) {
                            is com.google.firebase.Timestamp -> rawTs.toDate().time
                            is Number -> rawTs.toLong()
                            else -> 0L
                        }

                        ChatSummary(
                            id = doc.id,
                            groupName = doc.getString("groupName"),
                            groupImageUrl = doc.getString("groupImageUrl"),
                            lastMessage = doc.getString("lastMessage") ?: "",
                            isGroup = true,
                            updatedAt = time
                        )
                    }?.sortedByDescending { it.updatedAt } ?: emptyList()
                }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        if (pendingGroupInvites.isNotEmpty()) {
            item {
                Surface(
                    onClick = { showInvitesSheet = true },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                ) {
                    Row(Modifier.padding(16.dp, 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Groups, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Invitaciones de grupo",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.weight(1f))
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        ) {
                            Text(
                                "${pendingGroupInvites.size}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                }
            }
            item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
        }

        item { 
            Text(
                "Tus Grupos", 
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), 
                fontWeight = FontWeight.Bold, 
                fontSize = 15.sp, 
                color = Color.Gray
            ) 
        }

        if (groupChats.isEmpty()) {
            item { Box(Modifier.fillParentMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { Text("No hay grupos activos", color = Color.Gray) } }
        }

        items(items = groupChats, key = { it.id }) { group ->
            ChatItem(
                name = group.groupName ?: "Grupo", 
                subtext = group.lastMessage, 
                imageUrl = group.groupImageUrl, 
                isGroup = true,
                onClick = {
                    navController.navigate("chat/${group.id}/${group.groupName ?: "Grupo"}")
                },
                onImageClick = { url -> onImageClick(url ?: "") }
            )
        }
    }

    if (showInvitesSheet) {
        ModalBottomSheet(
            onDismissRequest = { showInvitesSheet = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            dragHandle = {}
        ) {
            PendingGroupInvitesSheet(
                invites = pendingGroupInvites,
                onAccept = { id ->
                    currentUser?.let { user ->
                        db.collection("chats").document(id).update(
                            "pendingParticipants", FieldValue.arrayRemove(user.uid),
                            "participants", FieldValue.arrayUnion(user.uid)
                        )
                    }
                },
                onReject = { id ->
                    currentUser?.let { user ->
                        db.collection("chats").document(id).update(
                            "pendingParticipants", FieldValue.arrayRemove(user.uid)
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun PendingGroupInvitesSheet(
    invites: List<Triple<String, String, String?>>,
    onAccept: (String) -> Unit,
    onReject: (String) -> Unit
) {
    val premiumGradient = Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, Color(0xFF1565C0)))
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        // Cabecera Premium
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .background(premiumGradient)
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .align(Alignment.TopCenter)
                    .width(40.dp)
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.3f), CircleShape)
            )
            
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "Invitaciones de Grupo",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp
                )
                Text(
                    "Decide a qué equipos quieres unirte",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        if (invites.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Text("No hay invitaciones pendientes", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(invites) { (id, name, img) ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(6.dp, RoundedCornerShape(20.dp), ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFF1F4F9))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(52.dp),
                                shape = CircleShape,
                                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            ) {
                                if (img != null) {
                                    AsyncImage(model = img, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                } else {
                                    AsyncImage(
                                        model = com.bitronix.bycompa.R.drawable.grupopordefecto,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                }
                            }
                            
                            Spacer(Modifier.width(16.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(name, fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.Black)
                                Text("Te han invitado a unirte", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                            }
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                // Rechazar
                                Surface(
                                    onClick = { onReject(id) },
                                    modifier = Modifier.size(38.dp),
                                    shape = CircleShape,
                                    color = Color.Red.copy(alpha = 0.1f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Close, null, tint = Color.Red, modifier = Modifier.size(18.dp))
                                    }
                                }
                                
                                // Aceptar
                                Surface(
                                    onClick = { onAccept(id) },
                                    modifier = Modifier.size(38.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize().background(premiumGradient),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Done, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
fun ChatListTemporales(
    navController: NavController, 
    onImageClick: (String) -> Unit,
    selectedChatId: String?,
    onChatLongClick: (String, Boolean, Boolean) -> Unit,
    currentUser: FirebaseUser?
) {
    val db = FirebaseFirestore.getInstance()
    var eventChats by remember { mutableStateOf<List<ChatSummary>>(emptyList()) }

    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            val triggeredWarnings = mutableSetOf<String>()
            db.collection("chats")
                .whereArrayContains("participants", user.uid)
                .addSnapshotListener { snap, error ->
                    if (error != null) return@addSnapshotListener
                    
                    eventChats = snap?.documents?.mapNotNull { doc ->
                        val isEvent = doc.getBoolean("isEventChat") ?: false
                        if (!isEvent) return@mapNotNull null
                        
                        val eventDate = doc.getString("eventDate")
                        val eventTime = doc.getString("eventTime")
                        val warningSent = doc.getBoolean("warningSent") ?: false
                        
                        // Lógica de expiración
                        val eventMillis = parseDateTime(eventDate, eventTime)
                        val isExpired = doc.getBoolean("isExpired") ?: false
                        val ratedBy = doc.get("ratedBy") as? Map<String, Any> ?: emptyMap()
                        val alreadyRated = ratedBy[user.uid] == true
                        
                        if (eventMillis > 0) {
                            val now = System.currentTimeMillis()
                            val oneHourBeforeClosure = eventMillis + (23 * 60 * 60 * 1000)
                            val closureTime = eventMillis + (24 * 60 * 60 * 1000)

                            if (now > closureTime && !isExpired) {
                                db.collection("chats").document(doc.id).update("isExpired", true)
                            } else if (now > oneHourBeforeClosure && !warningSent && !isExpired && !triggeredWarnings.contains(doc.id)) {
                                triggeredWarnings.add(doc.id)
                                db.collection("chats").document(doc.id).update("warningSent", true)
                                db.collection("chats").document(doc.id).collection("messages").document("warning_1h").set(hashMapOf(
                                    "senderId" to "system",
                                    "text" to "⚠️ Este chat se cerrará automáticamente en 1h.",
                                    "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                                ))
                            }
                        }

                        val time: Long = when(val rawTs = doc.get("updatedAt") ?: doc.get("createdAt")) {
                            is com.google.firebase.Timestamp -> rawTs.toDate().time
                            is Number -> rawTs.toLong()
                            else -> 0L
                        }

                        val unread = (doc.get("unreadCounts") as? Map<String, Any>)?.get(user.uid)
                        val myUnread = (unread as? Number)?.toInt() ?: 0
                        val pBy = doc.get("pinnedBy") as? Map<String, Any> ?: emptyMap()
                        val hBy = doc.get("hiddenBy") as? Map<String, Any> ?: emptyMap()

                        ChatSummary(
                            id = doc.id,
                            groupName = doc.getString("groupName"),
                            groupImageUrl = doc.getString("groupImageUrl"),
                            sport = doc.getString("sport"),
                            eventDate = eventDate,
                            eventTime = eventTime,
                            warningSent = warningSent,
                            isExpired = isExpired,
                            unreadCount = myUnread,
                            isPinned = pBy[user.uid] == true,
                            isHidden = hBy[user.uid] == true,
                            alreadyRated = alreadyRated,
                            lastMessage = doc.getString("lastMessage") ?: "",
                            isGroup = true,
                            updatedAt = time
                        )
                    } ?: emptyList()
                    
                    eventChats = eventChats.filter { !it.isHidden }
                        .sortedWith(compareByDescending<ChatSummary> { it.isPinned }.thenByDescending { it.updatedAt })
                }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item { 
            Text(
                "Tus Chats de Eventos", 
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), 
                fontWeight = FontWeight.Bold, 
                fontSize = 15.sp, 
                color = Color.Gray
            ) 
        }

        if (eventChats.isEmpty()) {
            item { Box(Modifier.fillParentMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { Text("No tienes chats de eventos", color = Color.Gray) } }
        }

        items(items = eventChats, key = { it.id }) { chat ->
            val sportIcon = when {
                chat.sport?.contains("Fútbol", ignoreCase = true) == true -> "⚽"
                chat.sport?.contains("Pádel", ignoreCase = true) == true -> "🎾"
                chat.sport?.contains("Baloncesto", ignoreCase = true) == true -> "🏀"
                chat.sport?.contains("Tenis", ignoreCase = true) == true -> "🎾"
                chat.sport?.contains("Running", ignoreCase = true) == true -> "🏃"
                chat.sport?.contains("Gym", ignoreCase = true) == true -> "🏋️"
                else -> "🏆"
            }
            
            ChatItem(
                name = chat.groupName ?: "Evento", 
                subtext = if (chat.isExpired) "Evento finalizado" else chat.lastMessage, 
                imageUrl = chat.groupImageUrl, 
                isGroup = true,
                isPinned = chat.isPinned,
                isSelected = selectedChatId == chat.id,
                unreadCount = chat.unreadCount,
                iconText = sportIcon,
                isExpired = chat.isExpired,
                onClick = {
                    if (selectedChatId != null) {
                        onChatLongClick(chat.id, true, chat.isExpired)
                    } else {
                        if (chat.isExpired && !chat.alreadyRated) {
                            navController.navigate("rating/${chat.id}/${chat.groupName ?: "Evento"}")
                        } else {
                            navController.navigate("chat/${chat.id}/${chat.groupName ?: "Evento"}")
                        }
                    }
                },
                onLongClick = { onChatLongClick(chat.id, true, chat.isExpired) },
                onImageClick = { url -> onImageClick(url) }
            )
        }
    }
}

// --- DIÁLOGOS ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewGroupDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    var groupName by remember { mutableStateOf("") }
    var userSearch by remember { mutableStateOf("") }
    var foundUserPreview by remember { mutableStateOf<Triple<String, String, String?>?>(null) } // ID, Name, Image
    var isSearching by remember { mutableStateOf(false) }
    // Triple: ID, Name, ImageUrl
    val selectedUsers = remember { mutableStateListOf<Triple<String, String, String?>>() }
    var isCreating by remember { mutableStateOf(false) }

    val premiumGradient = Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, Color(0xFF1565C0)))

    // Búsqueda en tiempo real
    LaunchedEffect(userSearch) {
        if (userSearch.length >= 3) {
            isSearching = true
            db.collection("users").whereEqualTo("nameLower", userSearch.lowercase()).get().addOnSuccessListener { query ->
                if (!query.isEmpty) {
                    val doc = query.documents[0]
                    foundUserPreview = Triple(doc.id, doc.getString("name") ?: "Compas", doc.getString("profileImageUrl"))
                } else {
                    foundUserPreview = null
                }
                isSearching = false
            }
        } else {
            foundUserPreview = null
            isSearching = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column {
                // Cabecera con degradado
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(premiumGradient)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Crear Nuevo Grupo", 
                        color = Color.White, 
                        fontSize = 20.sp, 
                        fontWeight = FontWeight.Black
                    )
                }

                Column(modifier = Modifier.padding(24.dp)) {
                    OutlinedTextField(
                        value = groupName, 
                        onValueChange = { groupName = it }, 
                        label = { Text("Nombre del grupo") }, 
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Spacer(Modifier.height(20.dp))
                    
                    OutlinedTextField(
                        value = userSearch, 
                        onValueChange = { userSearch = it }, 
                        label = { Text("Buscar usuario") }, 
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) },
                        trailingIcon = {
                            if (isSearching) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        }
                    )
                    
                    Spacer(Modifier.height(12.dp))

                    // --- PREVISUALIZACIÓN DE BÚSQUEDA ---
                    AnimatedVisibility(visible = foundUserPreview != null) {
                        foundUserPreview?.let { (id, name, imageUrl) ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(Modifier.size(40.dp).clip(CircleShape).background(Color.LightGray)) {
                                        if (imageUrl != null) AsyncImage(model = imageUrl, contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                        else Icon(Icons.Default.Person, null, modifier = Modifier.align(Alignment.Center))
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Text(name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    IconButton(
                                        onClick = {
                                            if (!selectedUsers.any { it.first == id }) {
                                                selectedUsers.add(Triple(id, name, imageUrl))
                                            }
                                            userSearch = ""
                                            foundUserPreview = null
                                        },
                                        modifier = Modifier.background(premiumGradient, CircleShape).size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    if (selectedUsers.isNotEmpty()) {
                        Text("Integrantes seleccionados:", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
                        LazyRow {
                            items(selectedUsers) { (id, name, imageUrl) ->
                                AssistChip(
                                    onClick = { selectedUsers.removeIf { it.first == id } },
                                    label = { Text(name) },
                                    leadingIcon = {
                                        Box(Modifier.size(24.dp).clip(CircleShape).background(Color.LightGray)) {
                                            if (imageUrl != null) AsyncImage(model = imageUrl, contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                            else Icon(Icons.Default.Person, null, modifier = Modifier.size(14.dp).align(Alignment.Center))
                                        }
                                    },
                                    trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(14.dp)) },
                                    modifier = Modifier.padding(end = 6.dp),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancelar", fontWeight = FontWeight.Bold)
                        }
                        
                        Button(
                            enabled = groupName.isNotEmpty() && selectedUsers.isNotEmpty() && !isCreating,
                            onClick = {
                                isCreating = true
                                val creatorUid = auth.currentUser?.uid ?: ""
                                val invitedUids = selectedUsers.map { it.first }
                                
                                val data = hashMapOf(
                                    "groupName" to groupName,
                                    "participants" to listOf(creatorUid),
                                    "pendingParticipants" to invitedUids,
                                    "creatorId" to creatorUid,
                                    "isGroup" to true,
                                    "isEventChat" to false,
                                    "lastMessage" to "Invitaciones enviadas",
                                    "createdAt" to System.currentTimeMillis(),
                                    "updatedAt" to System.currentTimeMillis()
                                )
                                db.collection("chats").add(data).addOnSuccessListener { onDismiss() }
                            },
                            modifier = Modifier.weight(1.2f).height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(if (groupName.isNotEmpty() && selectedUsers.isNotEmpty()) premiumGradient else Brush.linearGradient(listOf(Color.Gray, Color.Gray))),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isCreating) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White)
                                else Text("Crear Grupo", fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChatRequestDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    var targetUsername by remember { mutableStateOf("") }
    var foundUser by remember { mutableStateOf<Pair<String, String?>?>(null) }
    var isSending by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }

    val premiumGradient = Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, Color(0xFF1565C0)))

    // Búsqueda en tiempo real (debounced)
    LaunchedEffect(targetUsername) {
        if (targetUsername.length >= 3) {
            isSearching = true
            db.collection("users").whereEqualTo("nameLower", targetUsername.lowercase()).get().addOnSuccessListener { query ->
                if (!query.isEmpty) {
                    val doc = query.documents[0]
                    foundUser = (doc.getString("name") ?: "Compas") to doc.getString("profileImageUrl")
                } else {
                    foundUser = null
                }
                isSearching = false
            }
        } else {
            foundUser = null
            isSearching = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column {
                // Cabecera con degradado
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(premiumGradient)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Nueva Invitación", 
                        color = Color.White, 
                        fontSize = 20.sp, 
                        fontWeight = FontWeight.Black
                    )
                }

                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        "Escribe el nombre de usuario exacto para buscar a tu compa.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = targetUsername, 
                        onValueChange = { targetUsername = it }, 
                        label = { Text("Nombre de usuario") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) },
                        trailingIcon = {
                            if (isSearching) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        }
                    )

                    Spacer(Modifier.height(16.dp))

                    // --- PREVISUALIZACIÓN DEL USUARIO ---
                    AnimatedVisibility(visible = foundUser != null) {
                        foundUser?.let { (name, imageUrl) ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (imageUrl != null) {
                                            AsyncImage(
                                                model = imageUrl,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                            )
                                        } else {
                                            Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(name, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancelar", fontWeight = FontWeight.Bold)
                        }
                        
                        Button(
                            enabled = foundUser != null && !isSending,
                            onClick = {
                                isSending = true
                                val myUid = auth.currentUser?.uid ?: ""
                                db.collection("users").whereEqualTo("nameLower", targetUsername.lowercase()).get().addOnSuccessListener { query ->
                                    if (!query.isEmpty) {
                                        val targetUid = query.documents[0].id
                                        
                                        if (targetUid == myUid) {
                                            Toast.makeText(context, "No puedes invitarte a ti mismo", Toast.LENGTH_SHORT).show()
                                            isSending = false
                                        } else {
                                            // 1. Verificar si ya existe un chat activo (ya son amigos)
                                            db.collection("chats")
                                                .whereArrayContains("participants", myUid)
                                                .whereEqualTo("isGroup", false)
                                                .get().addOnSuccessListener { chatSnap ->
                                                    val alreadyFriends = chatSnap.documents.any { doc ->
                                                        (doc.get("participants") as? List<String>)?.contains(targetUid) == true
                                                    }
                                                    
                                                    if (alreadyFriends) {
                                                        Toast.makeText(context, "Ya tienes un chat activo con este usuario", Toast.LENGTH_SHORT).show()
                                                        isSending = false
                                                    } else {
                                                        // 2. Verificar solicitudes pendientes (en ambas direcciones)
                                                        db.collection("chat_requests")
                                                            .whereIn("from", listOf(myUid, targetUid))
                                                            .get().addOnSuccessListener { reqSnap ->
                                                                val pendingExists = reqSnap.documents.any { doc ->
                                                                    val to = doc.getString("to")
                                                                    val status = doc.getString("status")
                                                                    (to == myUid || to == targetUid) && status == "pending"
                                                                }
                                                                
                                                                if (pendingExists) {
                                                                    Toast.makeText(context, "Ya hay una solicitud pendiente entre vosotros", Toast.LENGTH_SHORT).show()
                                                                    isSending = false
                                                                } else {
                                                                    // 3. Enviar nueva solicitud
                                                                    val req = hashMapOf(
                                                                        "from" to myUid,
                                                                        "to" to targetUid,
                                                                        "status" to "pending",
                                                                        "timestamp" to System.currentTimeMillis()
                                                                    )
                                                                    db.collection("chat_requests").add(req).addOnSuccessListener {
                                                                        Toast.makeText(context, "Solicitud enviada", Toast.LENGTH_SHORT).show()
                                                                        onDismiss()
                                                                    }.addOnFailureListener { isSending = false }
                                                                }
                                                            }.addOnFailureListener { isSending = false }
                                                    }
                                                }.addOnFailureListener { isSending = false }
                                        }
                                    } else {
                                        Toast.makeText(context, "Usuario no encontrado", Toast.LENGTH_SHORT).show()
                                        isSending = false
                                    }
                                }.addOnFailureListener { isSending = false }
                            },
                            modifier = Modifier.weight(1.2f).height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(if (foundUser != null) premiumGradient else Brush.linearGradient(listOf(Color.Gray, Color.Gray))),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSending) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White)
                                else Text("Enviar Invitación", fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingRequestsSheet(
    requests: List<ChatRequest>, 
    userProfiles: Map<String, Triple<String, String?, Boolean>>, 
    onDismiss: () -> Unit
) {
    val premiumGradient = Brush.linearGradient(
        colors = listOf(MaterialTheme.colorScheme.primary, Color(0xFF1565C0))
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {},
        scrimColor = Color.Black.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // --- CABECERA PREMIUM ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(premiumGradient)
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .align(Alignment.TopCenter)
                        .width(40.dp)
                        .height(4.dp)
                        .background(Color.White.copy(alpha = 0.3f), CircleShape)
                )

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Solicitudes Recibidas", 
                        fontSize = 20.sp, 
                        fontWeight = FontWeight.Black, 
                        color = Color.White
                    )
                    Text(
                        "Usuarios que quieren chatear contigo",
                        fontSize = 12.sp, 
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (requests.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.HourglassEmpty, null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("No hay nuevas solicitudes", color = Color.Gray, fontWeight = FontWeight.Medium)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(requests) { request ->
                        val profile = userProfiles[request.from]
                        RequestItem(
                            request = request,
                            name = profile?.first ?: "Compas",
                            imageUrl = profile?.second
                        )
                    }
                    item { Spacer(Modifier.height(32.dp)) }
                }
            }
        }
    }
}

@Composable
fun RequestItem(request: ChatRequest, name: String, imageUrl: String?) {
    val db = FirebaseFirestore.getInstance()
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(
                elevation = 6.dp, 
                shape = RoundedCornerShape(20.dp), 
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFF1F4F9))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Imagen con borde sutil
            Surface(
                modifier = Modifier.size(52.dp),
                shape = CircleShape,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            ) {
                if (!imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    AsyncImage(
                        model = com.bitronix.bycompa.R.drawable.perfilpordefecto,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.Black)
                Text("Quiere empezar un chat", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            }
            
            Spacer(Modifier.width(8.dp))

            // Acciones Modernas
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Aceptar
                Surface(
                    onClick = {
                        db.collection("chat_requests").document(request.id).update("status", "accepted")
                        db.collection("chats").add(hashMapOf(
                            "participants" to listOf(request.from, request.to),
                            "isGroup" to false,
                            "isEventChat" to false,
                            "lastMessage" to "¡Solicitud aceptada!",
                            "updatedAt" to System.currentTimeMillis()
                        ))
                    },
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    color = Color(0xFF4CAF50).copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Check, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(24.dp))
                    }
                }

                Spacer(Modifier.width(12.dp))

                // Rechazar
                Surface(
                    onClick = {
                        db.collection("chat_requests").document(request.id).update("status", "rejected")
                    },
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    color = Color.Red.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Close, null, tint = Color.Red, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ChatItem(
    name: String, 
    subtext: String, 
    imageUrl: String?, 
    unreadCount: Int = 0,
    isGroup: Boolean, 
    isPinned: Boolean = false,
    isSelected: Boolean = false,
    isOnline: Boolean = false,
    iconText: String? = null,
    isExpired: Boolean = false,
    onClick: () -> Unit, 
    onLongClick: () -> Unit = {},
    onImageClick: (String) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    isExpired -> Color(0xFFE3F2FD) // Azulito claro
                    unreadCount > 0 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                    else -> Color.Transparent
                }
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            }
            .padding(horizontal = 16.dp, vertical = 12.dp), 
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(50.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier.fillMaxSize().clip(CircleShape)
                    .background(if (isGroup) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer)
                    .clickable(enabled = !imageUrl.isNullOrEmpty()) { imageUrl?.let { onImageClick(it) } },
                contentAlignment = Alignment.Center
            ) {
                if (!imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else if (!iconText.isNullOrEmpty()) {
                    Text(iconText, fontSize = 24.sp)
                } else {
                    if (isGroup) {
                        AsyncImage(
                            model = com.bitronix.bycompa.R.drawable.grupopordefecto,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        AsyncImage(
                            model = com.bitronix.bycompa.R.drawable.perfilpordefecto,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }
                }
            }
            
            if (isOnline && !isGroup) {
                Box(
                    modifier = Modifier
                        .size(15.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = (-2).dp, y = (-2).dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50)) // Verde en línea
                        .border(2.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = name, 
                fontWeight = if (unreadCount > 0) FontWeight.ExtraBold else FontWeight.Bold,
                color = if (unreadCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtext, 
                fontSize = 14.sp, 
                color = if (unreadCount > 0) MaterialTheme.colorScheme.onSurface else Color.Gray, 
                maxLines = 1, 
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (unreadCount > 0) FontWeight.Medium else FontWeight.Normal
            )
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isPinned) {
                    Icon(
                        Icons.Default.PushPin, 
                        null, 
                        modifier = Modifier.size(14.dp).padding(end = 4.dp),
                        tint = Color.Gray
                    )
                }
                Text("Hoy", fontSize = 11.sp, color = Color.Gray)
            }
            
            if (unreadCount > 0) {
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(Color(0xFF007AFF), CircleShape), // Azul iOS vibrante
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = unreadCount.toString(),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun FullScreenImageDialog(imageUrl: String, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Imagen ampliada",
                modifier = Modifier
                    .fillMaxSize(0.9f)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
            
            // Botón cerrar opcional
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).statusBarsPadding()
            ) {
                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SentRequestsSheet(
    requests: List<ChatRequest>, 
    userProfiles: Map<String, Triple<String, String?, Boolean>>, 
    onDismiss: () -> Unit
) {
    val premiumGradient = Brush.linearGradient(
        colors = listOf(MaterialTheme.colorScheme.primary, Color(0xFF1565C0))
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {},
        scrimColor = Color.Black.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // --- CABECERA PREMIUM ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(premiumGradient)
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .align(Alignment.TopCenter)
                        .width(40.dp)
                        .height(4.dp)
                        .background(Color.White.copy(alpha = 0.3f), CircleShape)
                )

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Solicitudes Enviadas", 
                        fontSize = 20.sp, 
                        fontWeight = FontWeight.Black, 
                        color = Color.White
                    )
                    Text(
                        "Esperando respuesta de otros usuarios",
                        fontSize = 12.sp, 
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (requests.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.MailOutline, null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("No tienes solicitudes pendientes", color = Color.Gray, fontWeight = FontWeight.Medium)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(requests) { request ->
                        val profile = userProfiles[request.to]
                        SentRequestItem(
                            request = request,
                            name = profile?.first ?: "Compas",
                            imageUrl = profile?.second
                        )
                    }
                    item { Spacer(Modifier.height(32.dp)) }
                }
            }
        }
    }
}

@Composable
fun SentRequestItem(request: ChatRequest, name: String, imageUrl: String?) {
    val db = FirebaseFirestore.getInstance()
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(
                elevation = 6.dp, 
                shape = RoundedCornerShape(20.dp), 
                ambientColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
            ),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFF1F4F9))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = CircleShape,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
            ) {
                if (!imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    AsyncImage(
                        model = com.bitronix.bycompa.R.drawable.perfilpordefecto,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.Black)
                Text("Esperando respuesta...", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Medium)
            }
            
            Spacer(Modifier.width(8.dp))

            // Botón Cancelar Premium
            Surface(
                onClick = {
                    db.collection("chat_requests").document(request.id).delete()
                },
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = Color.Red.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Close, null, tint = Color.Red, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

// --- TODOS LOS CHATS (CONTACTOS) ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllChatsSheet(onDismiss: () -> Unit, navController: NavController, currentUser: FirebaseUser?) {
    val db = FirebaseFirestore.getInstance()
    var contacts by remember { mutableStateOf<List<UserData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val premiumGradient = Brush.linearGradient(
        colors = listOf(MaterialTheme.colorScheme.primary, Color(0xFF1565C0))
    )

    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            db.collection("chats")
                .whereArrayContains("participants", user.uid)
                .whereEqualTo("isGroup", false)
                .get()
                .addOnSuccessListener { snap ->
                    val otherIds = snap.documents.map { doc ->
                        val parts = doc.get("participants") as? List<String> ?: emptyList()
                        parts.firstOrNull { it != user.uid } ?: ""
                    }.filter { it.isNotEmpty() }
                    
                    if (otherIds.isEmpty()) {
                        isLoading = false
                        return@addOnSuccessListener
                    }

                    db.collection("users").whereIn("uid", otherIds.take(20)).get().addOnSuccessListener { uSnap ->
                        contacts = uSnap.documents.mapNotNull { it.toObject(UserData::class.java)?.copy(uid = it.id) }
                        isLoading = false
                    }.addOnFailureListener {
                         isLoading = false
                    }
                }.addOnFailureListener {
                    isLoading = false
                }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {},
        scrimColor = Color.Black.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // --- CABECERA PREMIUM ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(premiumGradient)
            ) {
                // Indicador de arrastre personalizado
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .align(Alignment.TopCenter)
                        .width(40.dp)
                        .height(4.dp)
                        .background(Color.White.copy(alpha = 0.3f), CircleShape)
                )

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.RecentActors, null, tint = Color.White, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Agenda",
                            fontSize = 20.sp, 
                            fontWeight = FontWeight.Black, 
                            color = Color.White
                        )
                    }
                    Text(
                        "Tus contactos recientes", 
                        fontSize = 12.sp, 
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            
            if (isLoading) {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (contacts.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PersonOff, null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("No tienes contactos activos", color = Color.Gray, fontWeight = FontWeight.Medium)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f, false),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(contacts) { contact ->
                        ChatItem(
                            name = contact.name,
                            subtext = "Toca para abrir el chat",
                            imageUrl = contact.profileImageUrl,
                            isGroup = false,
                            onClick = {
                                db.collection("chats")
                                    .whereArrayContains("participants", currentUser?.uid ?: "")
                                    .whereEqualTo("isGroup", false)
                                    .get()
                                    .addOnSuccessListener { snap ->
                                        val chatDoc = snap.documents.find { doc ->
                                            (doc.get("participants") as? List<String>)?.contains(contact.uid) == true
                                        }
                                        if (chatDoc != null) {
                                            chatDoc.reference.update("hiddenBy.${currentUser?.uid}", false)
                                            navController.navigate("chat/${chatDoc.id}/${contact.name}")
                                            onDismiss()
                                        }
                                    }
                            },
                            onImageClick = {}
                        )
                    }
                    item { Spacer(Modifier.height(32.dp)) }
                }
            }
        }
    }
}

fun parseDateTime(date: String?, time: String?): Long {
    if (date == null || time == null) return 0L
    return try {
        // Formato esperado: d/M/yyyy HH:mm
        val sdf = SimpleDateFormat("d/M/yyyy HH:mm", Locale.getDefault())
        sdf.parse("$date $time")?.time ?: 0L
    } catch (e: Exception) {
        0L
    }
}