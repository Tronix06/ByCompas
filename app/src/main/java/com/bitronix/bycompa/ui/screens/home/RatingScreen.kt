package com.bitronix.bycompa.ui.screens.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.bitronix.bycompa.models.UserData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatingScreen(navController: NavController, chatId: String, eventName: String) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val context = LocalContext.current
    
    var participants by remember { mutableStateOf<List<UserData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val ratings = remember { mutableStateMapOf<String, Float>() }

    LaunchedEffect(chatId) {
        db.collection("chats").document(chatId).get().addOnSuccessListener { doc ->
            val participantIds = doc.get("participants") as? List<String> ?: emptyList()
            val filteredIds = participantIds.filter { it != currentUser?.uid }
            
            if (filteredIds.isEmpty()) {
                isLoading = false
                return@addOnSuccessListener
            }

            db.collection("users").whereIn("uid", filteredIds).get().addOnSuccessListener { userSnap ->
                participants = userSnap.documents.mapNotNull { it.toObject(UserData::class.java)?.copy(uid = it.id) }
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Valorar Compañeros", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(eventName, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF5F7FB)
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(Modifier.fillMaxSize().padding(padding)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                        .padding(16.dp)
                ) {
                    Text(
                        "¡Esperamos que hayas disfrutado del evento! Valora el comportamiento de tus compañeros para ayudar a la comunidad.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(participants) { user ->
                        ParticipantRatingItem(
                            user = user,
                            currentRating = ratings[user.uid] ?: 0f,
                            onRatingChange = { ratings[user.uid] = it }
                        )
                    }
                }

                Button(
                    onClick = {
                        if (ratings.isEmpty()) {
                            Toast.makeText(context, "Por favor, valora al menos a un compañero", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        isLoading = true
                        val batch = db.batch()
                        val myUid = currentUser?.uid ?: ""
                        val scope = kotlinx.coroutines.MainScope()
                        
                        scope.launch {
                            try {
                                val batch = db.batch()
                                ratings.forEach { (targetUid, stars) ->
                                    val ratingId = "${chatId}_${myUid}_${targetUid}"
                                    val ratingRef = db.collection("ratings").document(ratingId)
                                    val data = hashMapOf(
                                        "from" to myUid,
                                        "to" to targetUid,
                                        "chatId" to chatId,
                                        "stars" to stars,
                                        "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                                    )
                                    batch.set(ratingRef, data)
                                }
                                
                                // Marcamos que este usuario ya ha valorado en este chat
                                val chatRef = db.collection("chats").document(chatId)
                                batch.update(chatRef, "ratedBy.$myUid", true)
                                batch.commit().await()
                                
                                // Actualizar perfiles de los usuarios valorados
                                for ((targetUid, stars) in ratings) {
                                    db.runTransaction { transaction ->
                                        val userRef = db.collection("users").document(targetUid)
                                        val userDoc = transaction.get(userRef)
                                        
                                        val currentSum = userDoc.getDouble("totalRatingSum") ?: 0.0
                                        val currentCount = userDoc.getLong("totalRatingsCount")?.toInt() ?: 0
                                        val currentCounts = (userDoc.get("ratingCounts") as? Map<String, Long>) ?: emptyMap()
                                        
                                        val newSum = currentSum + stars
                                        val newCount = currentCount + 1
                                        val newAverage = newSum / newCount
                                        
                                        val starKey = stars.toInt().toString()
                                        val newCounts = currentCounts.toMutableMap()
                                        newCounts[starKey] = (currentCounts[starKey] ?: 0L) + 1
                                        
                                        transaction.update(userRef, 
                                            "totalRatingSum", newSum,
                                            "totalRatingsCount", newCount,
                                            "rating", newAverage,
                                            "ratingCounts", newCounts
                                        )
                                    }.await()
                                }

                                Toast.makeText(context, "¡Valoraciones enviadas! Gracias por mejorar la comunidad.", Toast.LENGTH_LONG).show()
                                navController.popBackStack()
                            } catch (e: Exception) {
                                isLoading = false
                                Toast.makeText(context, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Enviar Valoraciones", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun ParticipantRatingItem(user: UserData, currentRating: Float, onRatingChange: (Float) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(50.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer)) {
                if (!user.profileImageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = user.profileImageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(user.name.take(1).uppercase(), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                }
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(Modifier.weight(1f)) {
                Text(user.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                StarRatingBar(rating = currentRating, onRatingChange = onRatingChange)
            }
            
            if (currentRating > 0) {
                Text(
                    text = String.format("%.1f", currentRating),
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
fun StarRatingBar(rating: Float, onRatingChange: (Float) -> Unit) {
    Row {
        for (i in 1..5) {
            val starRating = i.toFloat()
            val icon = when {
                rating >= starRating -> Icons.Default.Star
                rating >= starRating - 0.5f -> Icons.Default.StarHalf
                else -> Icons.Default.StarBorder
            }
            val color = if (rating >= starRating - 0.5f) Color(0xFFFFC107) else Color.Gray
            
            Box(
                Modifier.clickable {
                    // Si pinchas en la mitad izquierda es .5, si es en la derecha es 1.0
                    // Por simplicidad en este MVP, alternamos entre .5 y 1 si pinchas la misma estrella
                    val newRating = if (rating == starRating) starRating - 0.5f else starRating
                    onRatingChange(newRating)
                }
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
