package com.bitronix.bycompa.ui.screens.home

import com.bitronix.bycompa.R
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bitronix.bycompa.models.UserData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.ui.draw.shadow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicProfileSheet(userData: UserData) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    
    var currentProfile by remember { mutableStateOf(userData) }
    
    LaunchedEffect(userData.uid) {
        db.collection("users").document(userData.uid).addSnapshotListener { snap, _ ->
            snap?.toObject(UserData::class.java)?.copy(uid = snap.id)?.let {
                currentProfile = it
            }
        }
    }

    val isFollowing = currentProfile.followers.contains(currentUser?.uid ?: "")
    val isPending = currentProfile.followRequests.contains(currentUser?.uid ?: "")

    val premiumGradient = Brush.linearGradient(
        colors = listOf(MaterialTheme.colorScheme.primary, Color(0xFF1565C0))
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- CABECERA PREMIUM CON FOTO ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(premiumGradient),
            contentAlignment = Alignment.Center
        ) {
            // Drag Handle
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.4f), CircleShape)
            )

            // Contenedor de la Foto
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    modifier = Modifier.size(110.dp),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.15f),
                    shadowElevation = 16.dp,
                    border = BorderStroke(3.dp, Color.White)
                ) {
                    AsyncImage(
                        model = userData.profileImageUrl ?: R.drawable.perfilpordefecto,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
                
                // Indicador Online (dentro del mismo contexto)
                if (currentProfile.isOnline) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (-4).dp, y = (-4).dp)
                            .size(24.dp),
                        shape = CircleShape,
                        color = Color(0xFF4CAF50),
                        border = BorderStroke(3.dp, Color.White)
                    ) {}
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            // Nombre
            Text(
                text = userData.name,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = Color.Black,
                letterSpacing = (-0.5).sp
            )
            
            Spacer(Modifier.height(8.dp))

            // --- BOTÓN DE ACCIÓN ---
            if (currentUser != null && currentUser.uid != currentProfile.uid) {
                val myUid = currentUser.uid
                var myData by remember { mutableStateOf<UserData?>(null) }
                
                LaunchedEffect(myUid) {
                    db.collection("users").document(myUid).addSnapshotListener { snap, _ ->
                        myData = snap?.toObject(UserData::class.java)
                    }
                }

                val hasReceivedRequest = myData?.followRequests?.contains(currentProfile.uid) == true
                
                val buttonText = when {
                    isFollowing -> "SIGUIENDO"
                    isPending -> "SOLICITUD ENVIADA"
                    hasReceivedRequest -> "ACEPTAR SOLICITUD"
                    else -> "SEGUIR"
                }
                val isActive = !isFollowing && !isPending && !hasReceivedRequest

                Surface(
                    onClick = {
                        val theirUid = currentProfile.uid
                        db.runBatch { batch ->
                            val myRef = db.collection("users").document(myUid)
                            val theirRef = db.collection("users").document(theirUid)
                            when {
                                isFollowing -> {
                                    batch.update(theirRef, "followers", FieldValue.arrayRemove(myUid))
                                    batch.update(myRef, "following", FieldValue.arrayRemove(theirUid))
                                }
                                hasReceivedRequest -> {
                                    batch.update(myRef, "followRequests", FieldValue.arrayRemove(theirUid))
                                    batch.update(myRef, "followers", FieldValue.arrayUnion(theirUid))
                                    batch.update(theirRef, "following", FieldValue.arrayUnion(myUid))
                                    
                                    db.collection("chats").add(hashMapOf(
                                        "participants" to listOf(myUid, theirUid),
                                        "isGroup" to false,
                                        "isEventChat" to false,
                                        "lastMessage" to "¡Ya sois amigos!",
                                        "updatedAt" to System.currentTimeMillis()
                                    ))
                                }
                                isPending -> batch.update(theirRef, "followRequests", FieldValue.arrayRemove(myUid))
                                else -> batch.update(theirRef, "followRequests", FieldValue.arrayUnion(myUid))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(0.8f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(if (isActive || hasReceivedRequest) premiumGradient else Brush.linearGradient(listOf(Color.Gray.copy(alpha = 0.1f), Color.Gray.copy(alpha = 0.2f)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            buttonText,
                            fontWeight = FontWeight.Black,
                            color = if (isActive || hasReceivedRequest) Color.White else Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // --- ESTADÍSTICAS SOCIALES PREMIUM ---
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 10.dp, shape = RoundedCornerShape(24.dp), ambientColor = Color(0xFF1565C0).copy(alpha = 0.2f)),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFF1565C0).copy(alpha = 0.05f))
            ) {
                Row(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.White, Color(0xFFF1F4F9).copy(alpha = 0.3f))
                            )
                        )
                        .padding(vertical = 20.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatBox(count = "${currentProfile.followers.size}", label = "Seguidores")
                    Box(modifier = Modifier.height(30.dp).width(1.dp).background(Color.LightGray.copy(alpha = 0.4f)))
                    StatBox(count = "${currentProfile.following.size}", label = "Siguiendo")
                    Box(modifier = Modifier.height(30.dp).width(1.dp).background(Color.LightGray.copy(alpha = 0.4f)))
                    StatBox(count = "${currentProfile.rating}", label = "Valoración", isRating = true)
                }
            }

            Spacer(Modifier.height(32.dp))

            // --- SECCIÓN DEPORTES (ESTILO INTEGRADO Y CENTRADO) ---
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 10.dp, shape = RoundedCornerShape(24.dp), ambientColor = Color(0xFF1565C0).copy(alpha = 0.1f)),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFF1565C0).copy(alpha = 0.05f))
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.White, Color(0xFFF1F4F9).copy(alpha = 0.3f))
                            )
                        )
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SportsScore, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "DEPORTES FAVORITOS", 
                            fontSize = 11.sp, 
                            fontWeight = FontWeight.Bold, 
                            color = Color.Gray, 
                            letterSpacing = 0.5.sp
                        )
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    if (userData.sports.isEmpty()) {
                        Text("Sin deportes elegidos", color = Color.Gray.copy(alpha = 0.6f), fontSize = 13.sp)
                    } else {
                        SharedFlowRow(
                            mainAxisSpacing = 24.dp, 
                            crossAxisSpacing = 12.dp, 
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally)
                        ) {
                            for (sport in userData.sports) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                ) {
                                    Text(
                                        sport, 
                                        color = MaterialTheme.colorScheme.primary, 
                                        fontWeight = FontWeight.Bold, 
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), 
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }



            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun StatBox(count: String, label: String, isRating: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = count, fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color.Black)
            if (isRating) {
                Icon(Icons.Default.Star, null, tint = Color(0xFFFFB300), modifier = Modifier.size(18.dp).padding(start = 4.dp))
            }
        }
        Text(text = label.uppercase(), fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
    }
}

@Composable
fun SectionHeaderWithIcon(icon: ImageVector, title: String) {
    Row(
        modifier = Modifier.fillMaxWidth(), 
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.Black, letterSpacing = 1.sp)
    }
}

@Composable
fun ProfileInfoRow(icon: ImageVector, label: String, value: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp, 
                shape = RoundedCornerShape(16.dp), 
                ambientColor = Color(0xFF1565C0).copy(alpha = 0.15f)
            ),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF1565C0).copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.White, Color(0xFFF1F4F9).copy(alpha = 0.3f))
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(
                text = label, 
                fontSize = 11.sp, 
                fontWeight = FontWeight.Bold, 
                color = Color.Gray, 
                letterSpacing = 0.5.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = value, 
                fontSize = 18.sp, 
                fontWeight = FontWeight.Black, 
                color = Color.Black
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SharedFlowRow(
    mainAxisSpacing: androidx.compose.ui.unit.Dp,
    crossAxisSpacing: androidx.compose.ui.unit.Dp,
    modifier: Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable FlowRowScope.() -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = Arrangement.spacedBy(crossAxisSpacing)
    ) {
        content()
    }
}
