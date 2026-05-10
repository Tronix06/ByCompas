package com.bitronix.bycompa.ui.screens.home

import android.widget.Toast
import androidx.navigation.NavController
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bitronix.bycompa.models.UserData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import coil.compose.AsyncImage
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Modelo de datos local para la búsqueda de usuarios
data class UserCardData(
    val uid: String = "",
    val name: String = "",
    val profileImageUrl: String? = null,
    val birthDate: String? = null,
    val rating: Double = 5.0,
    val sports: List<String> = emptyList(),
    val followers: List<String> = emptyList(),
    val followRequests: List<String> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuscarScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    // Estados para búsqueda y datos
    var searchQuery by remember { mutableStateOf("") }
    var allUsers by remember { mutableStateOf<List<UserCardData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Estados para el BottomSheet de perfil
    var showProfileSheet by remember { mutableStateOf(false) }
    var selectedUserForSheet by remember { mutableStateOf<UserData?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Manejo del botón atrás para limpiar la búsqueda antes de salir
    BackHandler(enabled = searchQuery.isNotBlank()) {
        searchQuery = ""
    }

    // Escucha activa de la colección de usuarios en Firebase
    LaunchedEffect(Unit) {
        db.collection("users").addSnapshotListener { snap, error ->
            if (error != null) {
                isLoading = false
                return@addSnapshotListener
            }
            if (snap != null) {
                allUsers = snap.documents.mapNotNull { doc ->
                    doc.toObject(UserCardData::class.java)?.copy(uid = doc.id)
                }
            }
            isLoading = false
        }
    }

    // Lógica de filtrado dinámico (por nombre o por deporte)
    val filteredUsers = allUsers.filter { user ->
        user.name.contains(searchQuery, ignoreCase = true) ||
                user.sports.any { sport -> sport.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        PaddingValues(horizontal = 16.dp).let {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Buscar",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(16.dp))

            // Barra de Búsqueda estilo Instagram
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                placeholder = { Text("Busca a tus compas...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.4f),
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
                    unfocusedLeadingIconColor = Color.Gray
                )
            )
        }

        Spacer(Modifier.height(12.dp))

        // Gestión de estados de la lista
        when {
            searchQuery.isBlank() -> {
                // Pantalla de Descubrimiento (Destacados + Categorías)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    // --- SECCIÓN 1: DEPORTISTAS DESTACADOS ---
                    val topAthletes = allUsers
                        .filter { it.uid.isNotEmpty() && it.uid != (currentUser?.uid ?: "") }
                        .sortedByDescending { it.followers.size }
                        .take(10)

                    if (topAthletes.isNotEmpty()) {
                        item {
                            Text(
                                "Deportistas Destacados",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                topAthletes.forEach { athlete ->
                                    FeaturedUserCard(
                                        user = athlete,
                                        onClick = {
                                            scope.launch {
                                                try {
                                                    val doc = db.collection("users").document(athlete.uid).get().await()
                                                    selectedUserForSheet = doc.toObject(UserData::class.java)
                                                    showProfileSheet = true
                                                } catch (e: Exception) {}
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // --- SECCIÓN 2: CATEGORÍAS DE INTERÉS (DINÁMICAS) ---
                    val mySports = allUsers.find { it.uid == currentUser?.uid }?.sports ?: emptyList()

                    if (mySports.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(32.dp))
                            Text(
                                "Tus intereses",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                            )
                            Text(
                                "Encuentra compas con tus mismos gustos",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp)
                            )
                        }

                        item {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                mySports.chunked(2).forEach { row ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        row.forEach { sportName ->
                                            val (icon, color) = getSportDetails(sportName)
                                            SportCategoryCard(
                                                name = sportName,
                                                icon = icon,
                                                modifier = Modifier.weight(1f),
                                                onClick = { searchQuery = sportName }
                                            )
                                        }
                                        if (row.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            filteredUsers.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No se han encontrado resultados", color = Color.Gray)
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    items(filteredUsers) { user ->
                        val myUid = currentUser?.uid ?: ""
                        if (user.uid.isNotEmpty() && user.uid != myUid) { // Ocultarse a sí mismo en los resultados
                            UserSearchItem(
                                user = user,
                                currentUid = currentUser?.uid ?: "",
                                onClick = {
                                    // Cargar datos completos para el sheet
                                    scope.launch {
                                        try {
                                            val doc = db.collection("users").document(user.uid).get().await()
                                            selectedUserForSheet = doc.toObject(UserData::class.java)
                                            showProfileSheet = true
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error al cargar perfil", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onFollowClick = {
                                    if (currentUser != null) {
                                        db.collection("users").document(user.uid)
                                            .update("followRequests", FieldValue.arrayUnion(currentUser.uid))
                                            .addOnFailureListener {
                                                Toast.makeText(context, "Error al enviar solicitud", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal para el Perfil Público
    if (showProfileSheet && selectedUserForSheet != null) {
        ModalBottomSheet(
            onDismissRequest = { showProfileSheet = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            dragHandle = {},
            scrimColor = Color.Black.copy(alpha = 0.5f)
        ) {
            PublicProfileSheet(userData = selectedUserForSheet!!)
        }
    }
}

@Composable
fun UserSearchItem(user: UserCardData, currentUid: String, onClick: () -> Unit, onFollowClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Foto de Perfil Circular
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!user.profileImageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = user.profileImageUrl,
                        contentDescription = "Foto de ${user.name}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Text(
                        text = user.name.take(1).uppercase(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = if (user.sports.isEmpty()) "Compas" 
                           else user.sports.joinToString(", "),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            
            // Botón Inteligente de Seguir
            val isFollowing = user.followers.contains(currentUid)
            val isPending = user.followRequests.contains(currentUid)

            Button(
                onClick = { if (!isFollowing && !isPending) onFollowClick() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        isFollowing -> Color.LightGray.copy(alpha = 0.5f)
                        isPending -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                        else -> MaterialTheme.colorScheme.primary
                    },
                    contentColor = if (isFollowing) Color.DarkGray else Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(
                    text = when {
                        isFollowing -> "Siguiendo"
                        isPending -> "Pendiente"
                        else -> "Seguir"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun FeaturedUserCard(user: UserCardData, onClick: () -> Unit) {
    val premiumGradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            Color(0xFF1565C0)
        )
    )

    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(140.dp)
            .shadow(
                elevation = 12.dp, 
                shape = RoundedCornerShape(24.dp),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                spotColor = MaterialTheme.colorScheme.primary
            )
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(premiumGradient)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .border(1.5.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (!user.profileImageUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = user.profileImageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Text(user.name.take(1).uppercase(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = user.name, 
                    fontWeight = FontWeight.Black, 
                    fontSize = 14.sp, 
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Edad
                user.birthDate?.let { dateStr ->
                    val age = calculateAge(dateStr)
                    if (age > 0) {
                        Text(
                            text = "$age años", 
                            fontSize = 11.sp, 
                            color = Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, modifier = Modifier.size(12.dp), tint = Color(0xFFFFD700))
                    Spacer(Modifier.width(4.dp))
                    Text(String.format("%.1f", user.rating), fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f), fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "${user.followers.size} seguidores", 
                        fontSize = 9.sp, 
                        fontWeight = FontWeight.Black, 
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun SportCategoryCard(name: String, icon: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val premiumGradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            Color(0xFF1565C0)
        )
    )

    Surface(
        onClick = onClick,
        modifier = modifier
            .height(100.dp)
            .shadow(
                elevation = 8.dp, 
                shape = RoundedCornerShape(20.dp),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                spotColor = MaterialTheme.colorScheme.primary
            )
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(premiumGradient),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(icon, fontSize = 32.sp)
                Spacer(Modifier.height(4.dp))
                Text(name, fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.White)
            }
        }
    }
}

fun getSportDetails(sportName: String): Pair<String, Color> {
    val name = sportName.lowercase().trim()
    return when {
        name.contains("fútbol") || name.contains("futbol") -> "⚽" to Color(0xFF4CAF50)
        name.contains("baloncesto") || name.contains("basket") -> "🏀" to Color(0xFFFF9800)
        name.contains("pádel") || name.contains("padel") -> "🎾" to Color(0xFFCDDC39)
        name.contains("tenis") -> "🥎" to Color(0xFFFFEB3B)
        name.contains("running") || name.contains("correr") -> "🏃" to Color(0xFF2196F3)
        name.contains("gimnasio") || name.contains("gym") -> "🏋️" to Color(0xFF9C27B0)
        name.contains("fitness") -> "💪" to Color(0xFFFF4081)
        name.contains("crossfit") -> "⛓️" to Color(0xFF455A64)
        name.contains("ciclismo") || name.contains("bici") -> "🚲" to Color(0xFFF44336)
        name.contains("natación") || name.contains("nadar") -> "🏊" to Color(0xFF00BCD4)
        name.contains("yoga") -> "🧘" to Color(0xFFE91E63)
        name.contains("pilates") -> "🤸" to Color(0xFF8BC34A)
        name.contains("senderismo") -> "🥾" to Color(0xFF795548)
        name.contains("golf") -> "🏌️" to Color(0xFF4CAF50)
        name.contains("boxeo") -> "🥊" to Color(0xFFD32F2F)
        name.contains("voleibol") -> "🏐" to Color(0xFFFDD835)
        name.contains("skate") -> "🛹" to Color(0xFF607D8B)
        name.contains("ajedrez") -> "♟️" to Color(0xFF455A64)
        else -> "🏆" to Color(0xFF607D8B)
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
        
        val dob = java.util.Calendar.getInstance()
        dob.set(year, month - 1, day)
        
        val today = java.util.Calendar.getInstance()
        var age = today.get(java.util.Calendar.YEAR) - dob.get(java.util.Calendar.YEAR)
        
        if (today.get(java.util.Calendar.MONTH) < dob.get(java.util.Calendar.MONTH) || 
            (today.get(java.util.Calendar.MONTH) == dob.get(java.util.Calendar.MONTH) && today.get(java.util.Calendar.DAY_OF_MONTH) < dob.get(java.util.Calendar.DAY_OF_MONTH))) {
            age--
        }
        age
    } catch (e: Exception) {
        0
    }
}