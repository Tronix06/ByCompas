package com.bitronix.bycompas.ui.screens.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// --- MODELOS DE DATOS ---
data class ChatRequest(
    val id: String = "",
    val from: String = "",
    val to: String = "",
    val status: String = "pending",
    val senderName: String = "Cargando..."
)

data class ChatSummary(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: String = "",
    val otherUserName: String = "Compas",
    val isGroup: Boolean = false,
    val groupName: String? = null
)

@Composable
fun ChatsScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Individual", "Grupos")
    var showIndividualDialog by remember { mutableStateOf(false) }
    var showGroupDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) showIndividualDialog = true
                    else showGroupDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(if (selectedTab == 0) Icons.Default.AddComment else Icons.Default.GroupAdd, null)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(
                selectedTabIndex = selectedTab,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) },
                        icon = { Icon(if (index == 0) Icons.Default.Person else Icons.Default.Groups, null) }
                    )
                }
            }
            Box(modifier = Modifier.fillMaxSize()) {
                if (selectedTab == 0) ChatListIndividual()
                else ChatListGrupos()
            }
        }
    }

    if (showIndividualDialog) NewChatRequestDialog(onDismiss = { showIndividualDialog = false })
    if (showGroupDialog) NewGroupDialog(onDismiss = { showGroupDialog = false })
}

// --- SECCIÓN INDIVIDUAL ---
@Composable
fun ChatListIndividual() {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    var pendingRequests by remember { mutableStateOf<List<ChatRequest>>(emptyList()) }
    var activeChats by remember { mutableStateOf<List<ChatSummary>>(emptyList()) }

    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            // Escuchar Solicitudes Pendientes dirigidas a mí
            db.collection("chat_requests")
                .whereEqualTo("to", user.uid)
                .whereEqualTo("status", "pending")
                .addSnapshotListener { snap, _ ->
                    val requests = snap?.documents?.mapNotNull { doc ->
                        doc.toObject(ChatRequest::class.java)?.copy(id = doc.id)
                    } ?: emptyList()

                    requests.forEach { req ->
                        db.collection("users").document(req.from).get().addOnSuccessListener { uDoc ->
                            val name = uDoc.getString("name") ?: "Usuario"
                            pendingRequests = pendingRequests.map {
                                if (it.id == req.id) it.copy(senderName = name) else it
                            }
                        }
                    }
                    pendingRequests = requests
                }

            // Escuchar Chats Individuales activos
            db.collection("chats")
                .whereArrayContains("participants", user.uid)
                .whereEqualTo("isGroup", false)
                .addSnapshotListener { snap, _ ->
                    val chats = snap?.documents?.mapNotNull { doc ->
                        val participants = doc.get("participants") as? List<String> ?: emptyList()
                        val otherId = participants.firstOrNull { it != user.uid } ?: ""
                        val chat = ChatSummary(
                            id = doc.id,
                            participants = participants,
                            lastMessage = doc.getString("lastMessage") ?: "¡Chat iniciado!"
                        )

                        db.collection("users").document(otherId).get().addOnSuccessListener { uDoc ->
                            val name = uDoc.getString("name") ?: "Compas"
                            activeChats = activeChats.map {
                                if (it.id == chat.id) it.copy(otherUserName = name) else it
                            }
                        }
                        chat
                    } ?: emptyList()
                    activeChats = chats
                }
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (pendingRequests.isNotEmpty()) {
            item { Text("Solicitudes pendientes", Modifier.padding(16.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
            items(pendingRequests) { request -> RequestItem(request) }
            item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
        }

        item { Text("Tus Chats", Modifier.padding(16.dp), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray) }

        items(activeChats) { chat ->
            ChatItem(name = chat.otherUserName, subtext = chat.lastMessage, isGroup = false)
        }
    }
}

// --- SECCIÓN GRUPOS ---
@Composable
fun ChatListGrupos() {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    var groupChats by remember { mutableStateOf<List<ChatSummary>>(emptyList()) }

    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            db.collection("chats")
                .whereArrayContains("participants", user.uid)
                .whereEqualTo("isGroup", true)
                .addSnapshotListener { snap, _ ->
                    groupChats = snap?.documents?.mapNotNull { doc ->
                        ChatSummary(
                            id = doc.id,
                            groupName = doc.getString("groupName"),
                            lastMessage = doc.getString("lastMessage") ?: "",
                            isGroup = true
                        )
                    } ?: emptyList()
                }
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { Text("Grupos de Compas", Modifier.padding(16.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary) }

        if (groupChats.isEmpty()) {
            item { Box(Modifier.fillParentMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { Text("No hay grupos activos", color = Color.Gray) } }
        }

        items(groupChats) { group ->
            ChatItem(name = group.groupName ?: "Grupo", subtext = group.lastMessage, isGroup = true)
        }
    }
}

// --- DIÁLOGOS ---

@Composable
fun NewGroupDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    var groupName by remember { mutableStateOf("") }
    var userSearch by remember { mutableStateOf("") }
    val selectedUsers = remember { mutableStateListOf<Pair<String, String>>() }
    var isCreating by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crear Nuevo Grupo") },
        text = {
            Column {
                OutlinedTextField(value = groupName, onValueChange = { groupName = it }, label = { Text("Nombre del grupo") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = userSearch, onValueChange = { userSearch = it }, label = { Text("Buscar Compa") }, modifier = Modifier.weight(1f))
                    IconButton(onClick = {
                        db.collection("users").whereEqualTo("name", userSearch).get().addOnSuccessListener { snap ->
                            if (!snap.isEmpty) {
                                val u = snap.documents[0]
                                if (!selectedUsers.any { it.first == u.id }) selectedUsers.add(u.id to (u.getString("name") ?: ""))
                                userSearch = ""
                            } else Toast.makeText(context, "No encontrado", Toast.LENGTH_SHORT).show()
                        }
                    }) { Icon(Icons.Default.PersonAdd, null) }
                }
                LazyRow(Modifier.padding(top = 8.dp)) {
                    items(selectedUsers) { user ->
                        AssistChip(
                            onClick = { selectedUsers.remove(user) },
                            label = { Text(user.second) },
                            trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(14.dp)) },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = groupName.isNotEmpty() && selectedUsers.isNotEmpty() && !isCreating,
                onClick = {
                    isCreating = true
                    val participants = selectedUsers.map { it.first }.toMutableList()
                    auth.currentUser?.uid?.let { participants.add(it) }
                    val data = hashMapOf(
                        "groupName" to groupName,
                        "participants" to participants,
                        "isGroup" to true,
                        "lastMessage" to "¡Grupo creado!",
                        "createdAt" to System.currentTimeMillis()
                    )
                    db.collection("chats").add(data).addOnSuccessListener { onDismiss() }
                }
            ) {
                if (isCreating) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White)
                else Text("Crear")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun NewChatRequestDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    var targetUsername by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva Invitación") },
        text = { OutlinedTextField(value = targetUsername, onValueChange = { targetUsername = it }, label = { Text("Nombre del usuario") }, modifier = Modifier.fillMaxWidth()) },
        confirmButton = {
            Button(
                enabled = targetUsername.isNotEmpty() && !isSending,
                onClick = {
                    isSending = true
                    db.collection("users").whereEqualTo("name", targetUsername).get().addOnSuccessListener { query ->
                        if (!query.isEmpty) {
                            val targetUid = query.documents[0].id
                            if (targetUid == auth.currentUser?.uid) {
                                Toast.makeText(context, "No puedes invitarte a ti mismo", Toast.LENGTH_SHORT).show()
                                isSending = false
                            } else {
                                val req = hashMapOf(
                                    "from" to auth.currentUser?.uid,
                                    "to" to targetUid,
                                    "status" to "pending",
                                    "timestamp" to System.currentTimeMillis()
                                )
                                db.collection("chat_requests").add(req).addOnSuccessListener {
                                    Toast.makeText(context, "Solicitud enviada", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                            }
                        } else {
                            Toast.makeText(context, "Usuario no encontrado", Toast.LENGTH_SHORT).show()
                            isSending = false
                        }
                    }
                }
            ) {
                if (isSending) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White)
                else Text("Enviar")
            }
        }
    )
}

// --- COMPONENTES DE LISTA ---

@Composable
fun RequestItem(request: ChatRequest) {
    val db = FirebaseFirestore.getInstance()
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                Text(request.senderName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(request.senderName, fontWeight = FontWeight.Bold)
                Text("Quiere empezar un chat", fontSize = 12.sp)
            }
            // Aceptar
            IconButton(onClick = {
                db.collection("chat_requests").document(request.id).update("status", "accepted")
                db.collection("chats").add(hashMapOf(
                    "participants" to listOf(request.from, request.to),
                    "isGroup" to false,
                    "lastMessage" to "¡Solicitud aceptada!",
                    "createdAt" to System.currentTimeMillis()
                ))
            }) { Icon(Icons.Default.Check, "Aceptar", tint = Color(0xFF4CAF50)) }

            // Rechazar (La X roja)
            IconButton(onClick = {
                db.collection("chat_requests").document(request.id).update("status", "rejected")
            }) { Icon(Icons.Default.Close, "Rechazar", tint = Color.Red) }
        }
    }
}

@Composable
fun ChatItem(name: String, subtext: String, isGroup: Boolean) {
    Row(Modifier.fillMaxWidth().clickable { /* Abrir conversación */ }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(50.dp).clip(CircleShape)
                .background(if (isGroup) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(if (isGroup) Icons.Default.Groups else Icons.Default.Person, null)
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(name, fontWeight = FontWeight.Bold)
            Text(subtext, fontSize = 14.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text("Hoy", fontSize = 12.sp, color = Color.Gray)
    }
}