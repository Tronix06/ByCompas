package com.bitronix.bycompas.ui.screens.home

import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.bitronix.bycompas.models.Message
import com.bitronix.bycompas.models.UserData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.filled.Image

sealed class ChatUiItem {
    data class DateSeparatorItem(val label: String) : ChatUiItem()
    data class MessageUiItem(val message: Message) : ChatUiItem()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndividualChatScreen(navController: NavController, chatId: String, otherName: String) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var textInput by remember { mutableStateOf("") }
    var otherUserImage by remember { mutableStateOf<String?>(null) }
    var otherUserData by remember { mutableStateOf<UserData?>(null) }
    var showFullImage by remember { mutableStateOf(false) }
    var showProfileSheet by remember { mutableStateOf(false) }
    var otherUserId by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    
    var selectedMessage by remember { mutableStateOf<Message?>(null) }
    var editingMessage by remember { mutableStateOf<Message?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var newEditText by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showOnlyFavorites by remember { mutableStateOf(false) }    
    var isEventChat by remember { mutableStateOf(false) }
    var isGroup by remember { mutableStateOf(false) }
    var chatSport by remember { mutableStateOf<String?>(null) }
    var onlineParticipantsCount by remember { mutableIntStateOf(1) }
    // Estados para Fondo de Pantalla
    var chatWallpaperUrl by remember { mutableStateOf<String?>(null) }
    var showFramerDialog by remember { mutableStateOf(false) }
    var showWallpaperOptions by remember { mutableStateOf(false) }
    var selectedWallpaperUri by remember { mutableStateOf<Uri?>(null) }
    val scope = rememberCoroutineScope()
    val storage = FirebaseStorage.getInstance()
    var groupImageUrl by remember { mutableStateOf<String?>(null) }
    var groupNameState by remember { mutableStateOf("") }
    var creatorId by remember { mutableStateOf<String?>(null) }
    var showGroupInfoSheet by remember { mutableStateOf(false) }
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var groupParticipants by remember { mutableStateOf<List<UserData>>(emptyList()) }
    var isLoadingParticipants by remember { mutableStateOf(false) }

    val groupImagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val ref = storage.reference.child("group_images/$chatId.jpg")
                    ref.putFile(uri).await()
                    val downloadUrl = ref.downloadUrl.await().toString()
                    
                    val userDoc = db.collection("users").document(currentUser!!.uid).get().await()
                    val userName = userDoc.getString("name") ?: "Un compa"
                    
                    db.collection("chats").document(chatId).update(
                        "groupImageUrl", downloadUrl,
                        "lastMessage", "$userName cambió la imagen del grupo"
                    ).await()
                    
                    val systemMsg = hashMapOf(
                        "senderId" to "system",
                        "text" to "$userName cambió la imagen del grupo",
                        "timestamp" to FieldValue.serverTimestamp()
                    )
                    db.collection("chats").document(chatId).collection("messages").add(systemMsg).await()
                    
                    groupImageUrl = downloadUrl
                    Toast.makeText(context, "Imagen de grupo actualizada", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Error al subir imagen: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedWallpaperUri = uri
            showFramerDialog = true
        }
    }

    val uiItems = remember(messages, showOnlyFavorites) {
        val items = mutableListOf<ChatUiItem>()
        var lastDateLabel = ""
        val myUid = currentUser?.uid ?: ""
        
        val displayedMessages = if (showOnlyFavorites) {
            messages.filter { it.favorites.contains(myUid) && !it.deletedForEveryone }
        } else {
            messages
        }
        
        displayedMessages.forEach { msg ->
            val dateLabel = getDateLabel(msg.timestamp)
            if (dateLabel != lastDateLabel) {
                items.add(ChatUiItem.DateSeparatorItem(dateLabel))
                lastDateLabel = dateLabel
            }
            items.add(ChatUiItem.MessageUiItem(msg))
        }
        items
    }

    LaunchedEffect(chatId) {
        db.collection("chats").document(chatId).addSnapshotListener { chatDoc, _ ->
            if (chatDoc != null && chatDoc.exists()) {
                isEventChat = chatDoc.getBoolean("isEventChat") ?: false
                isGroup = chatDoc.getBoolean("isGroup") ?: false
                chatSport = chatDoc.getString("sport")
                groupImageUrl = chatDoc.getString("groupImageUrl")
                groupNameState = chatDoc.getString("groupName") ?: otherName
                creatorId = chatDoc.getString("creatorId") ?: chatDoc.getString("creator")
                
                val participants = chatDoc.get("participants") as? List<String> ?: emptyList()
                
                // Cargar datos de participantes para grupos en tiempo real
                if (isGroup && participants.isNotEmpty()) {
                    db.collection("users").whereIn("uid", participants.take(30)).addSnapshotListener { usersSnap, _ ->
                        groupParticipants = usersSnap?.toObjects(UserData::class.java) ?: emptyList()
                    }
                }

                if (isEventChat) {
                    // Contar cuántos participantes están online
                    if (participants.isNotEmpty()) {
                        db.collection("users")
                            .whereIn("uid", participants.take(30)) 
                            .addSnapshotListener { usersSnap, _ ->
                                val count = usersSnap?.documents?.count { it.getBoolean("isOnline") == true } ?: 0
                                onlineParticipantsCount = if (count > 0) count else 1
                            }
                    }
                } else if (!isGroup) {
                    val otherId = participants.firstOrNull { it != (currentUser?.uid ?: "") } ?: ""
                    otherUserId = otherId
                    if (otherId.isNotEmpty()) {
                        db.collection("users").document(otherId).addSnapshotListener { uDoc, _ ->
                            if (uDoc != null && uDoc.exists()) {
                                val isOnlineVal = uDoc.getBoolean("isOnline") ?: false
                                val lastSeenVal = (uDoc.get("lastSeen") as? Number)?.toLong() ?: 0L
                                
                                otherUserData = uDoc.toObject(UserData::class.java)?.copy(
                                    uid = uDoc.id,
                                    isOnline = isOnlineVal,
                                    lastSeen = lastSeenVal
                                )
                                otherUserImage = otherUserData?.profileImageUrl
                            }
                        }
                    }
                }
            }
        }
        
        // Cargar Fondo de Pantalla
        if (currentUser != null) {
            db.collection("users").document(currentUser.uid).get().addOnSuccessListener { doc ->
                val userData = doc.toObject(UserData::class.java)
                chatWallpaperUrl = userData?.chatWallpapers?.get(chatId)
            }
        }
        
        // 2. Lógica de "Visto" inicial: Marcar mensajes antiguos como leídos
        if (currentUser != null) {
            db.collection("chats").document(chatId).update("unreadCounts.${currentUser.uid}", 0)
            db.collection("chats").document(chatId).collection("messages")
                .whereNotEqualTo("senderId", currentUser.uid)
                .get()
                .addOnSuccessListener { snap ->
                    snap.documents.forEach { doc ->
                        if (doc.get("readAt") == null) {
                            doc.reference.update("readAt", FieldValue.serverTimestamp())
                        }
                    }
                }
        }

        // 3. Escuchar mensajes en tiempo real (Sin orderBy para ver mensajes locales al instante)
        db.collection("chats").document(chatId).collection("messages")
            .addSnapshotListener { snap, error ->
                if (error != null) return@addSnapshotListener
                
                val msgs = snap?.documents?.mapNotNull { doc ->
                    val m = doc.toObject(Message::class.java)?.copy(id = doc.id)
                    val myUid = currentUser?.uid ?: ""
                    
                    when {
                        m == null -> null
                        m.deletedFor.contains(myUid) -> null 
                        m.deletedForEveryone -> m.copy(text = "❌ Este mensaje fue eliminado", deletedForEveryone = true)
                        else -> m
                    }
                } ?: emptyList()
                
                // Ordenar en cliente para evitar que desaparezcan mensajes con timestamp null local
                messages = msgs.sortedBy { it.timestamp?.time ?: Long.MAX_VALUE }
            }
    }

    // Lógica de "Visto" controlada para evitar bucles infinitos
    LaunchedEffect(messages) {
        if (currentUser != null) {
            val unreadMessages = messages.filter { it.senderId != currentUser.uid && it.readAt == null }
            if (unreadMessages.isNotEmpty()) {
                unreadMessages.forEach { msg ->
                    db.collection("chats").document(chatId).collection("messages").document(msg.id)
                        .update("readAt", FieldValue.serverTimestamp())
                }
                // Limpiar aviso en el resumen del chat
                db.collection("chats").document(chatId).update("unreadCounts.${currentUser.uid}", 0)
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    BackHandler {
        if (selectedMessage != null) {
            selectedMessage = null
        } else {
            val subTab = if (isEventChat) 2 else 0
            navController.navigate("home_main?startTab=chats&subTab=$subTab") {
                popUpTo("home_main") { inclusive = true }
            }
        }
    }

    Scaffold(
        topBar = {
            val premiumGradient = Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, Color(0xFF1565C0)))
            
            if (selectedMessage == null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 4.dp,
                    shadowElevation = 4.dp
                ) {
                    Box(modifier = Modifier.background(premiumGradient)) {
                        TopAppBar(
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically, 
                                    modifier = Modifier.clickable { 
                                        if (isGroup) {
                                            showGroupInfoSheet = true
                                        } else {
                                            showProfileSheet = true 
                                        }
                                    }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.2f)), 
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isGroup && groupImageUrl == null) {
                                            AsyncImage(
                                                model = com.bitronix.bycompas.R.drawable.grupopordefecto,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else if (isGroup && groupImageUrl != null) {
                                            AsyncImage(model = groupImageUrl, contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                                        } else if (!otherUserImage.isNullOrEmpty()) {
                                            AsyncImage(model = otherUserImage, contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                                        } else {
                                            AsyncImage(
                                                model = com.bitronix.bycompas.R.drawable.perfilpordefecto,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        val statusText = if (isEventChat) {
                                            "$onlineParticipantsCount en línea"
                                        } else {
                                            val isOnlineMillis = otherUserData?.lastSeen ?: 0L
                                            val isOnline = otherUserData?.isOnline ?: false
                                            val now = System.currentTimeMillis()
                                            val sevenDaysMillis = 7L * 24 * 60 * 60 * 1000
                                            
                                            when {
                                                isOnline -> "En línea"
                                                isOnlineMillis == 0L || (now - isOnlineMillis) > sevenDaysMillis -> "Ausente"
                                                else -> "Desconectado"
                                            }
                                        }
                                        
                                        val statusColor = if (isEventChat || otherUserData?.isOnline == true) {
                                            Color(0xFF81C784) // Verde clarito para el degradado
                                        } else {
                                            Color.White.copy(alpha = 0.7f)
                                        }

                                        Text(
                                            groupNameState, 
                                            style = MaterialTheme.typography.titleMedium, 
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                        if (!isGroup) {
                                            Text(
                                                statusText, 
                                                style = MaterialTheme.typography.bodySmall, 
                                                color = statusColor, 
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = { 
                                    val subTab = if (isEventChat) 2 else 0
                                    navController.navigate("home_main?startTab=chats&subTab=$subTab") {
                                        popUpTo("home_main") { inclusive = true }
                                    }
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                                }
                            },
                            actions = {
                                Box {
                                    IconButton(onClick = { showMoreMenu = true }) { Icon(Icons.Default.MoreVert, null, tint = Color.White) }
                                    DropdownMenu(
                                        expanded = showMoreMenu,
                                        onDismissRequest = { showMoreMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(if (showOnlyFavorites) "Mostrar todos" else "Ver Favoritos") },
                                            leadingIcon = { Icon(if (showOnlyFavorites) Icons.Default.StarBorder else Icons.Default.Star, null, tint = Color(0xFFFFD700)) },
                                            onClick = {
                                                showMoreMenu = false
                                                showOnlyFavorites = !showOnlyFavorites
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Fondo de chat") },
                                            leadingIcon = { Icon(Icons.Default.Image, null, tint = MaterialTheme.colorScheme.primary) },
                                            onClick = {
                                                showMoreMenu = false
                                                showWallpaperOptions = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Vaciar chat") },
                                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) },
                                            onClick = {
                                                showMoreMenu = false
                                                if (currentUser != null) {
                                                    db.collection("chats").document(chatId).collection("messages").get()
                                                        .addOnSuccessListener { snap ->
                                                            val batch = db.batch()
                                                            snap.documents.forEach { doc ->
                                                                batch.update(doc.reference, "deletedFor", FieldValue.arrayUnion(currentUser.uid))
                                                            }
                                                            batch.commit().addOnSuccessListener {
                                                                Toast.makeText(context, "Chat vaciado", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                }
                                            }
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                        )
                    }
                }
            } else {
                TopAppBar(
                    title = { Text("") },
                    navigationIcon = {
                        IconButton(onClick = { selectedMessage = null }) {
                            Icon(Icons.Default.Close, null)
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            val msg = selectedMessage!!
                            val myUid = currentUser?.uid ?: ""
                            val newFavorites = if (msg.favorites.contains(myUid)) msg.favorites - myUid else msg.favorites + myUid
                            db.collection("chats").document(chatId).collection("messages").document(msg.id).update("favorites", newFavorites)
                            selectedMessage = null
                            Toast.makeText(context, if (msg.favorites.contains(myUid)) "Eliminado de favoritos" else "Añadido a favoritos", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(if (selectedMessage?.favorites?.contains(currentUser?.uid ?: "") == true) Icons.Default.Star else Icons.Default.StarBorder, null, tint = Color.Yellow)
                        }
                        IconButton(onClick = {
                            val msg = selectedMessage!!
                            val myUid = currentUser?.uid ?: ""
                            val newLikes = if (msg.likes.contains(myUid)) msg.likes - myUid else msg.likes + myUid
                            db.collection("chats").document(chatId).collection("messages").document(msg.id).update("likes", newLikes)
                            selectedMessage = null
                        }) {
                            Icon(if (selectedMessage?.likes?.contains(currentUser?.uid ?: "") == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = Color.Red)
                        }
                        IconButton(onClick = { showInfoDialog = true }) {
                            Icon(Icons.Default.Info, null)
                        }
                        if (selectedMessage?.senderId == currentUser?.uid) {
                            val now = System.currentTimeMillis()
                            val sentTime = selectedMessage?.timestamp?.time ?: now
                            if ((now - sentTime) < 15 * 60 * 1000) {
                                IconButton(onClick = {
                                    editingMessage = selectedMessage
                                    newEditText = selectedMessage?.text ?: ""
                                    showEditDialog = true
                                    selectedMessage = null
                                }) { Icon(Icons.Default.Edit, null) }
                            }
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, null)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        },
        bottomBar = {
            val premiumGradient = Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, Color(0xFF1565C0)))
            Surface(
                tonalElevation = 12.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.background(premiumGradient)) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .navigationBarsPadding()
                            .imePadding(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Campo de entrada premium
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Escribe un mensaje...", color = Color.Gray) },
                            shape = RoundedCornerShape(28.dp),
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White.copy(alpha = 0.9f),
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        
                        // Botón de enviar premium
                        FloatingActionButton(
                            onClick = {
                                if (textInput.isNotBlank() && currentUser != null) {
                                    val messageText = textInput.trim()
                                    textInput = ""
                                    
                                    val msg = hashMapOf(
                                        "senderId" to currentUser.uid,
                                        "text" to messageText,
                                        "timestamp" to FieldValue.serverTimestamp()
                                    )
                                    
                                    val chatRef = db.collection("chats").document(chatId)
                                    chatRef.collection("messages").add(msg)
                                        .addOnSuccessListener {
                                            val updates = hashMapOf<String, Any>("lastMessage" to messageText)
                                            if (otherUserId.isNotEmpty()) {
                                                updates["unreadCounts.$otherUserId"] = FieldValue.increment(1)
                                            }
                                            chatRef.update(updates)
                                        }
                                }
                            },
                            modifier = Modifier.size(50.dp),
                            shape = CircleShape,
                            containerColor = Color.White,
                            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, "Enviar", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // FONDO CON PARALAJE
            if (chatWallpaperUrl != null) {
                val parallaxOffset = listState.firstVisibleItemScrollOffset.toFloat() / 15f
                AsyncImage(
                    model = chatWallpaperUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationY = -parallaxOffset
                            scaleX = 1.15f
                            scaleY = 1.15f
                        },
                    contentScale = ContentScale.Crop
                )
                // Capa de lectura (Oscurecimiento más suave)
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
            } else {
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F7FB)))
            }

            Column(modifier = Modifier.fillMaxSize()) {
                if (showOnlyFavorites) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                        modifier = Modifier.fillMaxWidth().clickable { showOnlyFavorites = false }
                    ) {
                        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, null, tint = Color.Yellow, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Mostrando solo favoritos (toca para quitar)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(
                    items = uiItems,
                    key = { item ->
                        when (item) {
                            is ChatUiItem.DateSeparatorItem -> "date_${item.label}"
                            is ChatUiItem.MessageUiItem -> item.message.id
                        }
                    }
                ) { item ->
                    when (item) {
                        is ChatUiItem.DateSeparatorItem -> DateSeparator(item.label)
                        is ChatUiItem.MessageUiItem -> {
                            val msg = item.message
                            val isSelected = selectedMessage?.id == msg.id
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                            ) {
                                MessageBubble(
                                    message = msg,
                                    isMine = msg.senderId == (currentUser?.uid ?: ""),
                                    isSelected = isSelected,
                                    myId = currentUser?.uid ?: "",
                                    onLongClick = {
                                        if (!msg.deletedForEveryone) {
                                            selectedMessage = msg
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            } // Cierra LazyColumn
            } // Cierra Column del filtro
        }
    }

    // Visor de Imagen Ampliada
    if (showFullImage && !otherUserImage.isNullOrEmpty()) {
        FullScreenImageDialog(imageUrl = otherUserImage!!, onDismiss = { showFullImage = false })
    }

    // Panel de Perfil Público
    if (showProfileSheet && otherUserData != null) {
        ModalBottomSheet(
            onDismissRequest = { showProfileSheet = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            dragHandle = {},
            scrimColor = Color.Black.copy(alpha = 0.5f)
        ) {
            PublicProfileSheet(userData = otherUserData!!)
        }
    }

    // Panel de Información del Grupo
    if (showGroupInfoSheet) {
        ModalBottomSheet(
            onDismissRequest = { showGroupInfoSheet = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            dragHandle = {},
            scrimColor = Color.Black.copy(alpha = 0.5f)
        ) {
            GroupInfoSheet(
                groupName = groupNameState,
                groupImageUrl = groupImageUrl,
                participants = groupParticipants,
                isLoading = isLoadingParticipants,
                onImageClick = { 
                    if (groupImageUrl != null) {
                        otherUserImage = groupImageUrl
                        showFullImage = true
                    }
                },
                onEditClick = { groupImagePickerLauncher.launch("image/*") },
                onParticipantClick = { p ->
                    otherUserData = p
                    showProfileSheet = true
                },
                onAddMemberClick = { showAddMemberDialog = true },
                onLeaveGroup = {
                    if (currentUser != null) {
                        scope.launch {
                            try {
                                // 1. Obtener nombre del usuario antes de salir
                                val userDoc = db.collection("users").document(currentUser.uid).get().await()
                                val userName = userDoc.getString("name") ?: "Un compa"
                                
                                // 2. Añadir mensaje de sistema
                                val systemMsg = hashMapOf(
                                    "senderId" to "system",
                                    "text" to "$userName salió del chat",
                                    "timestamp" to FieldValue.serverTimestamp()
                                )
                                db.collection("chats").document(chatId).collection("messages").add(systemMsg).await()
                                
                                // 3. Salir del grupo
                                db.collection("chats").document(chatId)
                                    .update(
                                        "participants", FieldValue.arrayRemove(currentUser.uid),
                                        "lastMessage", "$userName salió del chat"
                                    )
                                    .await()
                                
                                showGroupInfoSheet = false
                                Toast.makeText(context, "Has salido del grupo", Toast.LENGTH_SHORT).show()
                                
                                val subTab = if (isEventChat) 2 else 1
                                navController.navigate("home_main?startTab=chats&subTab=$subTab") {
                                    popUpTo("home_main") { inclusive = true }
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error al salir: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                isEventChat = isEventChat,
                canEditImage = !isEventChat || (currentUser?.uid == creatorId)
            )
        }
    }

    // Diálogo para Añadir Miembro
    if (showAddMemberDialog) {
        AddMemberDialog(
            onDismiss = { showAddMemberDialog = false },
            onAddMember = { uid, name ->
                if (currentUser != null) {
                    scope.launch {
                        try {
                            val myDoc = db.collection("users").document(currentUser.uid).get().await()
                            val myName = myDoc.getString("name") ?: "Un compa"
                            
                            val chatRef = db.collection("chats").document(chatId)
                            chatRef.update(
                                "participants", FieldValue.arrayUnion(uid),
                                "lastMessage", "$myName añadió a $name"
                            ).await()
                            
                            val systemMsg = hashMapOf(
                                "senderId" to "system",
                                "text" to "$myName añadió a $name",
                                "timestamp" to FieldValue.serverTimestamp()
                            )
                            chatRef.collection("messages").add(systemMsg).await()
                            
                            showAddMemberDialog = false
                            Toast.makeText(context, "$name ha sido añadido", Toast.LENGTH_SHORT).show()
                            
                            // Refrescar lista visual de participantes
                            db.collection("users").whereIn("uid", (groupParticipants.map { it.uid } + uid).take(30)).get().addOnSuccessListener { s ->
                                groupParticipants = s.toObjects(UserData::class.java)
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )
    }

    // Diálogo de Información
    if (showInfoDialog && selectedMessage != null) {
        val msg = selectedMessage!!
        val dayFormat = remember { SimpleDateFormat("d 'de' MMMM, HH:mm", Locale("es", "ES")) }
        AlertDialog(
            onDismissRequest = { showInfoDialog = false; selectedMessage = null },
            title = { Text("Información del mensaje") },
            text = {
                Column {
                    Text("Enviado: ${msg.timestamp?.let { dayFormat.format(it) } ?: "..."}")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Leído: ${msg.readAt?.let { dayFormat.format(it) } ?: "Pendiente"}",
                        color = if (msg.readAt != null) Color(0xFF007AFF) else Color.Gray
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false; selectedMessage = null }) { Text("Cerrar") }
            }
        )
    }

    // Diálogo de Eliminar
    if (showDeleteDialog && selectedMessage != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false; selectedMessage = null },
            title = { Text("¿Eliminar mensaje?") },
            text = { Text("Esta acción no se puede deshacer.") },
            confirmButton = {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                    if (selectedMessage?.senderId == currentUser?.uid) {
                        TextButton(onClick = {
                            val msgId = selectedMessage?.id ?: ""
                            db.collection("chats").document(chatId).collection("messages").document(msgId).update("deletedForEveryone", true)
                                .addOnSuccessListener {
                                    showDeleteDialog = false
                                    selectedMessage = null
                                }
                        }) { Text("Eliminar para todos", color = Color.Red, fontWeight = FontWeight.Bold) }
                    }
                    TextButton(onClick = {
                        val msgId = selectedMessage?.id ?: ""
                        val myUid = currentUser?.uid ?: ""
                        db.collection("chats").document(chatId).collection("messages").document(msgId).update("deletedFor", FieldValue.arrayUnion(myUid))
                            .addOnSuccessListener {
                                showDeleteDialog = false
                                selectedMessage = null
                            }
                    }) { Text("Eliminar para mí", color = Color.Red) }
                    TextButton(onClick = { showDeleteDialog = false; selectedMessage = null }) { Text("Cancelar") }
                }
            }
        )
    }

    if (showEditDialog && editingMessage != null) {
        val context = LocalContext.current
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Editar Mensaje")
                }
            },
            text = {
                OutlinedTextField(
                    value = newEditText,
                    onValueChange = { newEditText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Escribe el nuevo mensaje...") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val msgId = editingMessage?.id ?: ""
                        if (msgId.isNotEmpty()) {
                            val chatRef = db.collection("chats").document(chatId)
                            val updates = hashMapOf<String, Any>(
                                "text" to newEditText.trim(),
                                "edited" to true
                            )
                            chatRef.collection("messages").document(msgId).update(updates)
                                .addOnSuccessListener {
                                    chatRef.update("lastMessage", newEditText.trim())
                                    showEditDialog = false
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Error al editar", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // OPCIONES DE FONDO (SIN FONDO / CON FONDO)
    if (showWallpaperOptions) {
        AlertDialog(
            onDismissRequest = { showWallpaperOptions = false },
            title = { Text("Fondo de chat") },
            text = { Text("¿Cómo quieres configurar el fondo de este chat?") },
            confirmButton = {
                Button(onClick = {
                    showWallpaperOptions = false
                    galleryLauncher.launch("image/*")
                }) {
                    Text("Con fondo (Galería)")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showWallpaperOptions = false
                    if (currentUser != null) {
                        scope.launch {
                            try {
                                // Eliminar físicamente del Storage
                                try {
                                    storage.reference.child("wallpapers/${currentUser.uid}/$chatId.jpg")
                                        .delete().await()
                                } catch (e: Exception) {
                                    // Ignoramos si el archivo no existe en Storage, solo queremos limpiar Firestore
                                }

                                db.collection("users").document(currentUser.uid)
                                    .update("chatWallpapers.$chatId", FieldValue.delete()).await()
                                
                                chatWallpaperUrl = null
                                Toast.makeText(context, "Fondo eliminado por completo", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }) {
                    Text("Sin fondo", color = Color.Red)
                }
            }
        )
    }

    // DIÁLOGO DE ENCUADRE DE FONDO
    if (showFramerDialog && selectedWallpaperUri != null) {
        var scale by remember { mutableStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        val state = rememberTransformableState { zoomChange, offsetChange, _ ->
            scale *= zoomChange
            offset += offsetChange
        }

        AlertDialog(
            onDismissRequest = { showFramerDialog = false },
            title = { Text("Ajustar fondo") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Mueve y pellizca para encuadrar la imagen", fontSize = 12.sp, color = Color.Gray)
                    Spacer(Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black)
                    ) {
                        AsyncImage(
                            model = selectedWallpaperUri,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x,
                                    translationY = offset.y
                                )
                                .transformable(state = state),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        try {
                            if (currentUser != null) {
                                val imageRef = storage.reference.child("wallpapers/${currentUser.uid}/$chatId.jpg")
                                imageRef.putFile(selectedWallpaperUri!!).await()
                                val downloadUrl = imageRef.downloadUrl.await().toString()
                                
                                db.collection("users").document(currentUser.uid)
                                    .update("chatWallpapers.$chatId", downloadUrl).await()
                                
                                chatWallpaperUrl = downloadUrl
                                showFramerDialog = false
                                Toast.makeText(context, "Fondo aplicado", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error al subir: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) { Text("Aplicar") }
            },
            dismissButton = {
                TextButton(onClick = { showFramerDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun DateSeparator(label: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.05f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )
        }
    }
}

fun getDateLabel(date: Date?): String {
    if (date == null) return "..."
    val now = Calendar.getInstance()
    val msgDate = Calendar.getInstance().apply { time = date }
    
    return when {
        isSameDay(now, msgDate) -> "Hoy"
        isYesterday(now, msgDate) -> "Ayer"
        else -> SimpleDateFormat("d 'de' MMMM", Locale("es", "ES")).format(date)
    }
}

fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

fun isYesterday(now: Calendar, msgDate: Calendar): Boolean {
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    return isSameDay(yesterday, msgDate)
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message, 
    isMine: Boolean,
    isSelected: Boolean = false,
    myId: String = "",
    onLongClick: () -> Unit = {}
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeString = remember(message.timestamp) { 
        if (message.timestamp != null) timeFormat.format(message.timestamp) else "..." 
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (message.senderId == "system") 8.dp else 1.dp),
        horizontalAlignment = when {
            message.senderId == "system" -> Alignment.CenterHorizontally
            isMine -> Alignment.End
            else -> Alignment.Start
        }
    ) {
        if (message.senderId == "system") {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = CircleShape,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    text = message.text,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            val bubbleColor = when {
                message.deletedForEveryone -> Brush.linearGradient(colors = listOf(Color.LightGray.copy(alpha = 0.2f), Color.LightGray.copy(alpha = 0.2f)))
                isMine -> Brush.linearGradient(colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)))
                else -> Brush.linearGradient(colors = listOf(Color.White, Color.White))
            }

            Box(contentAlignment = Alignment.BottomEnd) {
                Surface(
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .combinedClickable(
                            onClick = { /* Navegación si aplica */ },
                            onLongClick = onLongClick
                        ),
                    shape = RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isMine) 18.dp else 4.dp,
                        bottomEnd = if (isMine) 4.dp else 18.dp
                    ),
                    color = Color.Transparent,
                    shadowElevation = if (message.deletedForEveryone) 0.dp else 2.dp,
                    tonalElevation = 1.dp
                ) {
                    Box(modifier = Modifier.background(bubbleColor).padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Column {
                            Text(
                                text = message.text,
                                color = if (message.deletedForEveryone) Color.Gray else if (isMine) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp,
                                fontStyle = if (message.deletedForEveryone) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
                                lineHeight = 20.sp
                            )
                            
                            Row(
                                modifier = Modifier.align(Alignment.End).padding(top = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                if (message.edited && !message.deletedForEveryone) {
                                    Text(
                                        text = "Editado",
                                        color = (if (isMine) Color.White else Color.Gray).copy(alpha = 0.5f),
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                }
                                
                                Text(
                                    text = timeString,
                                    color = (if (isMine) Color.White else Color.Gray).copy(alpha = 0.7f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                
                                if (isMine && !message.deletedForEveryone) {
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.DoneAll,
                                        contentDescription = null,
                                        tint = if (message.readAt != null) Color(0xFF34B7F1) else Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                
                val hasLikes = message.likes.isNotEmpty() && !message.deletedForEveryone
                val hasFavorites = message.favorites.contains(myId) && !message.deletedForEveryone


                if (hasLikes || hasFavorites) {
                    Surface(
                        modifier = Modifier.offset(x = (4).dp, y = (8).dp),
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (hasLikes) {
                                Icon(Icons.Default.Favorite, null, tint = Color.Red, modifier = Modifier.size(12.dp))
                                if (message.likes.size > 1) {
                                    Text(message.likes.size.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 2.dp, end = if (hasFavorites) 4.dp else 0.dp))
                                }
                            }
                            if (hasFavorites) {
                                if (hasLikes && message.likes.size == 1) Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.Star, "Favorito", tint = Color.Yellow, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GroupInfoSheet(
    groupName: String,
    groupImageUrl: String?,
    participants: List<UserData>,
    isLoading: Boolean,
    onImageClick: () -> Unit,
    onEditClick: () -> Unit,
    onParticipantClick: (UserData) -> Unit,
    onAddMemberClick: () -> Unit,
    onLeaveGroup: () -> Unit,
    isEventChat: Boolean,
    canEditImage: Boolean
) {
    val premiumGradient = Brush.linearGradient(
        colors = listOf(MaterialTheme.colorScheme.primary, Color(0xFF1565C0))
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // --- CABECERA PREMIUM CON GRADIENTE ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(premiumGradient)
        ) {
            // Drag handle sutil
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .align(Alignment.TopCenter)
                    .width(40.dp)
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.3f), CircleShape)
            )
        }

        // --- IMAGEN DE GRUPO FLOTANTE ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-60).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                Surface(
                    modifier = Modifier
                        .size(120.dp)
                        .shadow(12.dp, CircleShape),
                    shape = CircleShape,
                    border = BorderStroke(4.dp, Color.White),
                    color = Color.White
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { onImageClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = groupImageUrl ?: com.bitronix.bycompas.R.drawable.grupopordefecto,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                
                if (canEditImage) {
                    Surface(
                        modifier = Modifier
                            .offset(x = (-4).dp, y = (-4).dp)
                            .size(36.dp)
                            .clickable { onEditClick() },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp), tint = Color.White)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Nombre y Miembros
            Text(
                text = groupName,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = Color.Black,
                textAlign = TextAlign.Center
            )
            
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    text = "${participants.size} MIEMBROS",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // --- CONTENIDO ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-30).dp)
                .padding(horizontal = 24.dp)
        ) {
            // SECCIÓN DE MIEMBROS PREMIUM
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 4.dp,
                border = BorderStroke(1.dp, Color(0xFFF1F4F9))
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "MIEMBROS DEL EQUIPO",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (!isEventChat) {
                            IconButton(onClick = onAddMemberClick, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.PersonAdd, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(20.dp))
                    
                    if (isLoading) {
                        Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    } else {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(participants) { participant ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable { onParticipantClick(participant) }
                                ) {
                                    Surface(
                                        modifier = Modifier.size(56.dp),
                                        shape = CircleShape,
                                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                    ) {
                                        AsyncImage(
                                            model = participant.profileImageUrl,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = participant.name.substringBefore(" "),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.width(56.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // ACCIONES
            if (!isEventChat) {
                Surface(
                    onClick = { onLeaveGroup() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Red.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.1f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Close, null, tint = Color.Red, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("SALIR DEL GRUPO", color = Color.Red, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Chat temporal del evento. Solo el creador gestiona la imagen.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
fun AddMemberDialog(
    onDismiss: () -> Unit,
    onAddMember: (String, String) -> Unit
) {
    var userSearch by remember { mutableStateOf("") }
    var foundUser by remember { mutableStateOf<UserData?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val premiumGradient = Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, Color(0xFF1565C0)))

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Cabecera Premium
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(premiumGradient)
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Añadir al Equipo",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                }

                Column(modifier = Modifier.padding(24.dp)) {
                    // Buscador Premium
                    OutlinedTextField(
                        value = userSearch,
                        onValueChange = { userSearch = it },
                        placeholder = { Text("Nombre de usuario...", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF5F7FB),
                            unfocusedContainerColor = Color(0xFFF5F7FB),
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        trailingIcon = {
                            IconButton(onClick = {
                                if (userSearch.isNotBlank()) {
                                    isSearching = true
                                    db.collection("users").whereEqualTo("nameLower", userSearch.lowercase().trim()).get()
                                        .addOnSuccessListener { snap ->
                                            isSearching = false
                                            if (!snap.isEmpty) {
                                                foundUser = snap.documents[0].toObject(UserData::class.java)?.copy(uid = snap.documents[0].id)
                                            } else {
                                                Toast.makeText(context, "Usuario no encontrado", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                }
                            }) {
                                if (isSearching) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                else Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    )
                    
                    Spacer(Modifier.height(20.dp))

                    // Resultado de búsqueda
                    Box(modifier = Modifier.heightIn(min = 80.dp), contentAlignment = Alignment.Center) {
                        if (foundUser != null) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = Color.White,
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, Color(0xFFF1F4F9)),
                                shadowElevation = 2.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        modifier = Modifier.size(48.dp),
                                        shape = CircleShape,
                                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                    ) {
                                        AsyncImage(
                                            model = foundUser!!.profileImageUrl,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(foundUser!!.name, fontWeight = FontWeight.Black, fontSize = 15.sp)
                                        Text("Disponible para añadir", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    
                                    IconButton(
                                        onClick = { onAddMember(foundUser!!.uid, foundUser!!.name) },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(premiumGradient, CircleShape)
                                    ) {
                                        Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        } else {
                            Text(
                                "Busca a un compa por su nombre",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Botón Cerrar
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            "CANCELAR",
                            fontWeight = FontWeight.Black,
                            color = Color.Gray,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}

