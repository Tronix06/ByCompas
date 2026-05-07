package com.bitronix.bycompas.ui.screens.home

import com.bitronix.bycompas.R
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Dialog
import androidx.compose.animation.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.bitronix.bycompas.models.UserData
import com.bitronix.bycompas.models.UserReview
import com.bitronix.bycompas.navigation.Screen
import com.bitronix.bycompas.utils.ThemeManager
import com.bitronix.bycompas.utils.LevelManager
import com.bitronix.bycompas.utils.TimeUtils
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.bitronix.bycompas.utils.AppConstants
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    onShowSettingsChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()
    val currentUser = auth.currentUser

    // Estados de datos del usuario
    val scope = rememberCoroutineScope()
    var userData by remember { mutableStateOf<UserData?>(null) }
    var userReviews by remember { mutableStateOf<List<UserReview>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Estados de visibilidad
    var showEditForm by remember { mutableStateOf(false) }
    var showSportsDialog by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showAchievements by remember { mutableStateOf(false) }
    var showReviewsDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    
    // Notificar al padre cuando cambie la visibilidad de los ajustes
    LaunchedEffect(showSettings) {
        onShowSettingsChange(showSettings)
    }
    var showLinkDialog by remember { mutableStateOf(false) }
    var showDarkModeDialog by remember { mutableStateOf(false) }
    var showLevelInfo by remember { mutableStateOf(false) }
    
    // Estados temporales
    var tempName by remember { mutableStateOf("") }
    var tempPhone by remember { mutableStateOf("") }
    var tempBirthDate by remember { mutableStateOf("") }
    var tempGender by remember { mutableStateOf("") }
    var tempSports = remember { mutableStateListOf<String>() }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var confirmDeleteText by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var isChangingPassword by remember { mutableStateOf(false) }
    var isLinkingGoogle by remember { mutableStateOf(false) }
    var isGoogleLinked by remember { mutableStateOf(false) }
    var permissionRequested by  rememberSaveable { mutableStateOf(false) }

    // Estados para cambio de contraseña
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var currentPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val allSports = AppConstants.SPORTS_LIST
    val genderOptions = listOf("Hombre", "Mujer", "Prefiero no decirlo")

    // Google Sign In Launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken!!, null)
            
            currentUser?.linkWithCredential(credential)
                ?.addOnSuccessListener {
                    isLinkingGoogle = false
                    isGoogleLinked = true // Actualización instantánea
                    Toast.makeText(context, "Cuenta de Google vinculada con éxito", Toast.LENGTH_SHORT).show()
                }
                ?.addOnFailureListener { e ->
                    isLinkingGoogle = false
                    Toast.makeText(context, "Error al vincular: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } catch (e: Exception) {
            isLinkingGoogle = false
            Toast.makeText(context, "Error en Google Sign In: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    LaunchedEffect(currentUser) {
        currentUser?.uid?.let { uid ->
            // Perfil
            db.collection("users").document(uid).addSnapshotListener { snap, _ ->
                val data = snap?.toObject(UserData::class.java)?.copy(uid = uid)
                userData = data
                isLoading = false
                
                // --- AUTO-MIGRACIÓN ---
                // Si el usuario ya tiene nombre pero le falta nameLower, lo añadimos
                if (data != null && data.name.isNotBlank() && data.nameLower.isBlank()) {
                    db.collection("users").document(uid).update("nameLower", data.name.lowercase())
                }
            }
            // Valoraciones
            db.collection("users").document(uid).collection("reviews")
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener { snap, _ ->
                    userReviews = snap?.toObjects(UserReview::class.java) ?: emptyList()
                }
            
            // Inicializar estado de Google
            isGoogleLinked = currentUser.providerData.any { it.providerId == "google.com" }
        }
    }

    var followRequestUsers by remember { mutableStateOf<List<UserData>>(emptyList()) }
    var showRequestsSheet by remember { mutableStateOf(false) }

    LaunchedEffect(userData?.followRequests) {
        val requests = userData?.followRequests ?: emptyList()
        if (requests.isEmpty()) {
            followRequestUsers = emptyList()
            return@LaunchedEffect
        }
        val users = mutableListOf<UserData>()
        for (reqUid in requests) {
            try {
                val doc = db.collection("users").document(reqUid).get().await()
                doc.toObject(UserData::class.java)?.copy(uid = doc.id)?.let { users.add(it) }
            } catch (e: Exception) {}
        }
        followRequestUsers = users
    }

    var showConnectionsSheet by remember { mutableStateOf(false) }
    var connectionTitle by remember { mutableStateOf("") }
    var connectionsToFetch by remember { mutableStateOf<List<String>>(emptyList()) }
    var connectionUsers by remember { mutableStateOf<List<UserData>>(emptyList()) }
    var isFetchingConnections by remember { mutableStateOf(false) }
    var selectedUserForDialog by remember { mutableStateOf<UserData?>(null) }
    var showPhotoDialog by remember { mutableStateOf(false) }

    LaunchedEffect(selectedImageUri) {
        if (selectedImageUri != null && showPhotoDialog) {
            // Subir imagen inmediatamente si viene del diálogo de foto
            isSaving = true
            val uid = currentUser!!.uid
            val ref = storage.reference.child("profile_images/$uid.jpg")
            ref.putFile(selectedImageUri!!)
                .continueWithTask { task ->
                    if (!task.isSuccessful) task.exception?.let { throw it }
                    ref.downloadUrl
                }
                .addOnSuccessListener { downloadUri ->
                    db.collection("users").document(uid).update("profileImageUrl", downloadUri.toString())
                        .addOnSuccessListener {
                            isSaving = false
                            showPhotoDialog = false
                            selectedImageUri = null
                            Toast.makeText(context, "Foto actualizada", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {
                    isSaving = false
                    Toast.makeText(context, "Error al subir foto", Toast.LENGTH_SHORT).show()
                }
        }
    }

    LaunchedEffect(connectionsToFetch) {
        if (connectionsToFetch.isEmpty()) {
            connectionUsers = emptyList()
            isFetchingConnections = false
            return@LaunchedEffect
        }
        isFetchingConnections = true
        val users = mutableListOf<UserData>()
        for (uid in connectionsToFetch) {
            try {
                val doc = db.collection("users").document(uid).get().await()
                doc.toObject(UserData::class.java)?.copy(uid = doc.id)?.let { users.add(it) }
            } catch (e: Exception) {}
        }
        connectionUsers = users
        isFetchingConnections = false
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else {
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val filledFields = listOf(
                    userData?.name?.isNotBlank() == true,
                    userData?.profileImageUrl?.isNotBlank() == true,
                    (auth.currentUser?.email?.isNotBlank() == true),
                    userData?.phone?.isNotBlank() == true,
                    userData?.birthDate?.isNotBlank() == true,
                    userData?.gender?.isNotBlank() == true,
                    userData?.sports?.isNotEmpty() == true
                ).count { it }

                Spacer(Modifier.height(8.dp))

                // Cabecera Premium con Aura
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Aura de fondo
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Seguidores
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { 
                                    connectionTitle = "Seguidores"
                                    connectionsToFetch = userData?.followers ?: emptyList()
                                    showConnectionsSheet = true
                                }
                                .padding(8.dp)
                        ) {
                            Text(
                                text = (userData?.followers?.size ?: 0).toString(),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Seguidores",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray.copy(alpha = 0.8f)
                            )
                        }

                        // Imagen de Perfil Central
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(124.dp)
                                .clickable { showPhotoDialog = true }
                        ) {
                            // Borde decorativo
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape)
                            )
                            
                            AsyncImage(
                                model = userData?.profileImageUrl ?: R.drawable.logo,
                                contentDescription = "Foto de perfil",
                                modifier = Modifier
                                    .size(110.dp)
                                    .shadow(8.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // Seguidos
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    connectionTitle = "Seguidos"
                                    connectionsToFetch = userData?.following ?: emptyList()
                                    showConnectionsSheet = true
                                }
                                .padding(8.dp)
                        ) {
                            Text(
                                text = (userData?.following?.size ?: 0).toString(),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Seguidos",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally, 
                    verticalArrangement = Arrangement.spacedBy((-4).dp),
                    modifier = Modifier.offset(y = (-8).dp) // Subido para estar más cerca de la foto
                ) {
                    Text(userData?.name ?: "Usuario", fontSize = 28.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                    
                    if (filledFields == 7) {
                        Text(
                            "Perfil completado",
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    
                    Text(userData?.email ?: "", color = Color.Gray, fontSize = 14.sp)
                }

                // Botón Completar Perfil (Premium Blue)
                if (filledFields < 7) {
                    Button(
                        onClick = { 
                            tempName = userData?.name ?: ""
                            tempPhone = userData?.phone ?: ""
                            tempBirthDate = userData?.birthDate ?: ""
                            tempGender = userData?.gender ?: ""
                            tempSports.clear()
                            tempSports.addAll(userData?.sports ?: emptyList())
                            showEditForm = true 
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(top = 16.dp)
                            .height(56.dp)
                            .shadow(12.dp, RoundedCornerShape(20.dp), ambientColor = MaterialTheme.colorScheme.primary, spotColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.BorderColor, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Completar Perfil ($filledFields de 7)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Bloque de Estadísticas (Puntuación)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 12.dp)
                        .height(72.dp)
                        .clickable { showReviewsDialog = true },
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Reputación", fontSize = 15.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val hasRatings = (userData?.totalRatingsCount ?: 0) > 0
                            Text(
                                if (hasRatings) String.format("%.1f", userData?.rating ?: 5.0) else "-", 
                                fontWeight = FontWeight.Black, 
                                fontSize = 22.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            if (hasRatings) {
                                PartialStar(userData?.rating ?: 5.0)
                            } else {
                                Icon(Icons.Default.StarBorder, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // SECCIÓN DE DEPORTES FAVORITOS PREMIUM
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Cabecera de la sección
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.SportsTennis, 
                                    null, 
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Deportes favoritos", 
                                    fontSize = 17.sp, 
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            IconButton(
                                onClick = {
                                    tempSports.clear()
                                    tempSports.addAll(userData?.sports ?: emptyList())
                                    showSportsDialog = true
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.AddCircle, 
                                    "Añadir", 
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        if (userData?.sports?.isEmpty() == true) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 12.dp)
                            ) {
                                Icon(
                                    Icons.Default.SportsScore, 
                                    null, 
                                    tint = Color.Gray.copy(alpha = 0.3f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Añade tus deportes para encontrar partidas", 
                                    color = Color.Gray, 
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                userData?.sports?.forEach { sport ->
                                    // Chip Personalizado Premium
                                    Box(
                                        modifier = Modifier
                                            .shadow(4.dp, RoundedCornerShape(12.dp))
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    colors = listOf(
                                                        MaterialTheme.colorScheme.primary,
                                                        Color(0xFF1565C0)
                                                    )
                                                )
                                            )
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = sport,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // SECCIÓN RADIO RADAR PREMIUM
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        var tempSliderValue by remember(userData?.radarRadius) { 
                            mutableStateOf((userData?.radarRadius ?: 15.0).toFloat()) 
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Radar, 
                                    null, 
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Radio Radar", 
                                    fontSize = 17.sp, 
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            // Burbuja con el valor actual (Estilo Chip Premium)
                            Box(
                                modifier = Modifier
                                    .shadow(4.dp, RoundedCornerShape(12.dp))
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                Color(0xFF1565C0)
                                            )
                                        )
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${tempSliderValue.toInt()} km",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        Spacer(Modifier.height(16.dp))
                        
                        // Slider con Track de Degradado Premium
                        Slider(
                            value = tempSliderValue,
                            onValueChange = { tempSliderValue = it },
                            onValueChangeFinished = {
                                db.collection("users").document(currentUser!!.uid)
                                    .update("radarRadius", tempSliderValue.toDouble())
                            },
                            valueRange = 1f..50f,
                            modifier = Modifier.fillMaxWidth(),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.Transparent,
                                inactiveTrackColor = Color.Transparent
                            ),
                            track = { sliderState ->
                                val fraction = (tempSliderValue - 1f) / (50f - 1f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                                            .fillMaxHeight()
                                            .clip(CircleShape)
                                            .background(
                                                Brush.horizontalGradient(
                                                    colors = listOf(
                                                        MaterialTheme.colorScheme.primary,
                                                        Color(0xFF1565C0)
                                                    )
                                                )
                                            )
                                    )
                                }
                            },
                            thumb = {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .shadow(4.dp, CircleShape)
                                        .background(Color.White, CircleShape)
                                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                )
                            }
                        )
                        
                        Spacer(Modifier.height(4.dp))
                        
                        Text(
                            text = "Solo verás eventos dentro de este radio en el mapa de exploración.", 
                            fontSize = 11.sp, 
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(Modifier.height(120.dp)) // Padding final para librar la barra flotante
            }

            // Fila de Iconos Superiores Derecha (Notificaciones + Ajustes)
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-8).dp, y = 8.dp) // Bajados para que no estén pegados arriba
            ) {
                // Campanita de Notificaciones
                IconButton(
                    onClick = { showRequestsSheet = true },
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    BadgedBox(
                        badge = {
                            if ((userData?.followRequests?.size ?: 0) > 0) {
                                Badge(
                                    containerColor = Color.Red,
                                    contentColor = Color.White
                                ) {
                                    Text(userData?.followRequests?.size.toString())
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications, 
                            contentDescription = "Notificaciones", 
                            tint = MaterialTheme.colorScheme.primary, 
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                
    val scope = rememberCoroutineScope()

    // Botón de Ajustes (Hamburguesa)
    IconButton(onClick = { 
        // Primero escondemos la barra de navegación
        onShowSettingsChange(true)
        // Esperamos un instante antes de lanzar el menú de ajustes
        scope.launch {
            kotlinx.coroutines.delay(150)
            showSettings = true 
        }
    }) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Ajustes",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }

    if (showLevelInfo) {
        AlertDialog(
            onDismissRequest = { showLevelInfo = false },
            confirmButton = {
                TextButton(onClick = { showLevelInfo = false }) {
                    Text("Entendido", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Stars, null, tint = Color(0xFFFFB300), modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Sube de Nivel", fontWeight = FontWeight.Black)
                }
            },
            text = {
                Column {
                    Text(
                        "¡Estamos trabajando en nuevas formas de recompensar tu actividad!",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Próximamente, podrás ganar XP realizando acciones como:\n\n" +
                        "• Crear y completar eventos\n" +
                        "• Recibir valoraciones positivas\n" +
                        "• Cumplir retos semanales\n" +
                        "• Invitar a nuevos compas",
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }

    // --- MENÚ LATERAL DE AJUSTES PREMIUM (ANIMACIÓN INDEPENDIENTE) ---
    // Usamos un Box contenedor para que el dimmer y el panel tengan animaciones distintas
    if (showSettings) {
        // Bloqueamos el botón atrás para cerrar el menú si está abierto
        BackHandler { showSettings = false }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterEnd
        ) {
            // 1. Animación del FONDO (Solo Fundido, sin deslizamiento)
            androidx.compose.animation.AnimatedVisibility(
                visible = showSettings,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable { showSettings = false }
                )
            }
            
            // 2. Animación del PANEL (Deslizamiento + Fundido)
            androidx.compose.animation.AnimatedVisibility(
                visible = showSettings,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.8f),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp),
                    tonalElevation = 16.dp,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 40.dp, start = 20.dp, end = 20.dp, bottom = 20.dp)
                    ) {
                        // Header del Sidebar
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp, start = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                shape = CircleShape
                            ) {
                                Icon(
                                    Icons.Default.Settings, 
                                    null, 
                                    modifier = Modifier.padding(8.dp).size(24.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            Text(
                                text = "Ajustes",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Sección: Personalización
                        Text(
                            "Personalización",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                        )
                        
                        SettingsItem(
                            icon = Icons.Default.Person,
                            title = "Editar información",
                            onClick = { 
                                showSettings = false
                                // Precargamos los datos actuales para editar con seguridad nula
                                userData?.let {
                                    tempName = it.name ?: ""
                                    tempPhone = it.phone ?: ""
                                    tempBirthDate = it.birthDate ?: ""
                                    tempGender = it.gender ?: ""
                                    tempSports.clear()
                                    tempSports.addAll(it.sports)
                                }
                                showEditForm = true 
                            }
                        )
                        SettingsItem(
                            icon = Icons.Default.DarkMode,
                            title = "Modo oscuro",
                            onClick = { 
                                showSettings = false
                                showDarkModeDialog = true 
                            }
                        )
                        
                        Spacer(Modifier.height(24.dp))
                        
                        // Sección: Seguridad y Cuenta
                        Text(
                            "Seguridad y Cuenta",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                        )

                        SettingsItem(
                            icon = Icons.Default.LockReset,
                            title = "Cambiar contraseña",
                            onClick = { 
                                showSettings = false
                                currentPassword = ""
                                newPassword = ""
                                confirmPassword = ""
                                showChangePasswordDialog = true 
                            }
                        )
                        SettingsItem(
                            icon = Icons.Default.Link,
                            title = "Vincular cuentas",
                            onClick = { 
                                showSettings = false
                                showLinkDialog = true 
                            }
                        )
                        
                        Spacer(Modifier.weight(1f))
                        
                        // Botones de Acción Crítica
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color.LightGray.copy(alpha = 0.2f))
                        
                        Text(
                            "Salir",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                        )
                        
                        SettingsItem(
                            icon = Icons.Default.PersonRemove,
                            title = "Borrar cuenta",
                            titleColor = MaterialTheme.colorScheme.error,
                            onClick = { 
                                showSettings = false
                                showDeleteDialog = true 
                            }
                        )
                        SettingsItem(
                            icon = Icons.AutoMirrored.Filled.Logout,
                            title = "Cerrar sesión",
                            titleColor = MaterialTheme.colorScheme.error,
                            onClick = { 
                                scope.launch {
                                    showSettings = false
                                    // Seteamos desconectado antes de cerrar sesión
                                    currentUser?.uid?.let { uid ->
                                        try {
                                            db.collection("users").document(uid).update("isOnline", false).await()
                                        } catch (e: Exception) {
                                            // Si falla por falta de internet o lo que sea, cerramos igual
                                        }
                                    }
                                    auth.signOut()
                                    navController.navigate(Screen.Login.route) { popUpTo(0) } 
                                }
                            }
                        )
                        
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    // --- CENTRO DE NOTIFICACIONES PREMIUM ---
    if (showRequestsSheet) {
        @OptIn(ExperimentalMaterial3Api::class)
        ModalBottomSheet(
            onDismissRequest = { showRequestsSheet = false },
            containerColor = Color.Transparent,
            dragHandle = null,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            ) {
                val premiumGradient = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        Color(0xFF1565C0)
                    )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                ) {
                    // HEADER PREMIUM
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(premiumGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        // Drag Handle Manual
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 16.dp)
                                .width(40.dp)
                                .height(5.dp)
                                .background(Color.White.copy(alpha = 0.4f), CircleShape)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp)
                                .padding(top = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Notificaciones", 
                                    fontSize = 28.sp, 
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    letterSpacing = (-0.5).sp
                                )
                                Text(
                                    text = "Centro de actividad", 
                                    fontSize = 14.sp, 
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Surface(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = CircleShape
                            ) {
                                Icon(
                                    Icons.Default.NotificationsActive,
                                    null,
                                    modifier = Modifier.padding(12.dp).size(24.dp),
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        if (followRequestUsers.isEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    modifier = Modifier.size(100.dp),
                                    shape = CircleShape,
                                    color = Color.LightGray.copy(alpha = 0.05f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.NotificationsNone,
                                            null,
                                            modifier = Modifier.size(48.dp),
                                            tint = Color.Gray.copy(alpha = 0.2f)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(24.dp))
                                Text(
                                    "Todo al día",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = Color.Black
                                )
                                Text(
                                    "No tienes notificaciones pendientes",
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            // SECCIÓN: SOLICITUDES
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.GroupAdd, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "SOLICITUDES DE SEGUIMIENTO",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.sp
                                )
                            }
                            
                            Spacer(Modifier.height(16.dp))

                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().weight(1f, false),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(followRequestUsers) { requestUser ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = Color.White,
                                        shape = RoundedCornerShape(20.dp),
                                        shadowElevation = 2.dp,
                                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Avatar Premium
                                            Box {
                                                AsyncImage(
                                                    model = if (requestUser.profileImageUrl.isNullOrEmpty()) R.drawable.perfilpordefecto else requestUser.profileImageUrl,
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .size(54.dp)
                                                        .clip(CircleShape)
                                                        .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
                                                    contentScale = ContentScale.Crop
                                                )
                                                Surface(
                                                    modifier = Modifier.align(Alignment.BottomEnd).offset(x = 4.dp, y = 4.dp),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    shape = CircleShape,
                                                    border = BorderStroke(2.dp, Color.White)
                                                ) {
                                                    Icon(Icons.Default.PersonAdd, null, tint = Color.White, modifier = Modifier.padding(3.dp).size(10.dp))
                                                }
                                            }
                                            
                                            Spacer(Modifier.width(16.dp))
                                            
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = requestUser.name,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 16.sp
                                                )
                                                Text(
                                                    text = "Quiere ser tu amigo",
                                                    fontSize = 12.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                            
                                            // Acciones
                                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                IconButton(
                                                    onClick = {
                                                        val myUid = currentUser?.uid ?: return@IconButton
                                                        db.runBatch { batch ->
                                                            val myRef = db.collection("users").document(myUid)
                                                            val theirRef = db.collection("users").document(requestUser.uid)
                                                            
                                                            batch.update(myRef, "followers", FieldValue.arrayUnion(requestUser.uid))
                                                            batch.update(myRef, "followRequests", FieldValue.arrayRemove(requestUser.uid))
                                                            batch.update(theirRef, "following", FieldValue.arrayUnion(myUid))
                                                        }
                                                        followRequestUsers = followRequestUsers.filter { it.uid != requestUser.uid }
                                                    },
                                                    modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary, CircleShape)
                                                ) {
                                                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(22.dp))
                                                }
                                                
                                                IconButton(
                                                    onClick = {
                                                        val myUid = currentUser?.uid ?: return@IconButton
                                                        db.collection("users").document(myUid)
                                                            .update("followRequests", FieldValue.arrayRemove(requestUser.uid))
                                                        followRequestUsers = followRequestUsers.filter { it.uid != requestUser.uid }
                                                    },
                                                    modifier = Modifier.size(40.dp).background(Color.LightGray.copy(alpha = 0.2f), CircleShape)
                                                ) {
                                                    Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(22.dp))
                                                }
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
        }
    }

    // Modal de Seguidores / Seguidos
    if (showConnectionsSheet) {
        @OptIn(ExperimentalMaterial3Api::class)
        ModalBottomSheet(
            onDismissRequest = { showConnectionsSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    text = connectionTitle, 
                    fontSize = 20.sp, 
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(16.dp))
                
                if (isFetchingConnections) {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { 
                        CircularProgressIndicator() 
                    }
                } else if (connectionUsers.isEmpty()) {
                    Text("No hay usuarios en esta lista.", color = Color.Gray)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f, false)) {
                        items(connectionUsers) { connUser ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedUserForDialog = connUser }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Avatar Mini
                                AsyncImage(
                                    model = connUser.profileImageUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color.LightGray),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.width(12.dp))
                                
                                Text(
                                    text = connUser.name,
                                    modifier = Modifier.weight(1f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                
                                Spacer(Modifier.width(8.dp))
                                
                                Button(
                                    onClick = {
                                        val myUid = currentUser?.uid ?: return@Button
                                        db.runBatch { batch ->
                                            val myRef = db.collection("users").document(myUid)
                                            val theirRef = db.collection("users").document(connUser.uid)
                                            if (connectionTitle == "Seguidores") {
                                                batch.update(myRef, "followers", FieldValue.arrayRemove(connUser.uid))
                                                batch.update(theirRef, "following", FieldValue.arrayRemove(myUid))
                                            } else {
                                                batch.update(myRef, "following", FieldValue.arrayRemove(connUser.uid))
                                                batch.update(theirRef, "followers", FieldValue.arrayRemove(myUid))
                                            }
                                        }
                                        // Refresco visual automático
                                        connectionUsers = connectionUsers.filter { it.uid != connUser.uid }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                ) {
                                    Text(
                                        text = if (connectionTitle == "Seguidores") "Eliminar" else "Dejar de seguir",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    // Perfil Público del Usuario (Sheet)
    if (selectedUserForDialog != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedUserForDialog = null },
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            PublicProfileSheet(userData = selectedUserForDialog!!)
        }
    }

    // DIÁLOGO VINCULAR CUENTAS
    if (showLinkDialog) {
        val providers = currentUser?.providerData?.map { it.providerId } ?: emptyList()
        val hasEmail = providers.contains("password")
        val hasGoogle = providers.contains("google.com")

        AlertDialog(
            onDismissRequest = { if (!isLinkingGoogle) showLinkDialog = false },
            title = { Text("Vinculación de cuentas") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Vincula otros métodos de inicio de sesión para no perder el acceso a tu cuenta.", fontSize = 14.sp)
                    
                    // Email/Password
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = if (hasEmail) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Email, null, tint = if (hasEmail) MaterialTheme.colorScheme.primary else Color.Gray)
                            Spacer(Modifier.width(12.dp))
                            Text("Correo y contraseña", modifier = Modifier.weight(1f), fontWeight = if (hasEmail) FontWeight.Bold else FontWeight.Normal)
                            if (hasEmail) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Google
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = if (hasGoogle) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant,
                        onClick = { 
                            if (!hasGoogle) {
                                isLinkingGoogle = true
                                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                    .requestIdToken(context.getString(R.string.bycompas_web_client_id))
                                    .requestEmail()
                                    .build()
                                val googleSignInClient = GoogleSignIn.getClient(context, gso)
                                googleSignInLauncher.launch(googleSignInClient.signInIntent)
                            }
                        }
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountCircle, null, tint = if (hasGoogle) Color(0xFF4285F4) else Color.Gray)
                            Spacer(Modifier.width(12.dp))
                            Text("Cuenta de Google", modifier = Modifier.weight(1f), fontWeight = if (hasGoogle) FontWeight.Bold else FontWeight.Normal)
                            if (hasGoogle) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                            else if (isLinkingGoogle) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLinkDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // DIÁLOGO CAMBIO DE CONTRASEÑA
    if (showChangePasswordDialog) {
        AlertDialog(
            onDismissRequest = { if (!isChangingPassword) showChangePasswordDialog = false },
            title = { Text("Cambiar Contraseña") },
            text = {
                Column {
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { Text("Contraseña actual") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (currentPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { currentPasswordVisible = !currentPasswordVisible }) {
                                Icon(if (currentPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                            }
                        }
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Nueva contraseña") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                                Icon(if (newPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                            }
                        }
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirmar nueva contraseña") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                            }
                        }
                    )

                    Spacer(Modifier.height(16.dp))

                    // Requisitos de contraseña
                    val hasMinChars = newPassword.length >= 6
                    val hasLowercase = newPassword.any { it.isLowerCase() }
                    val hasUppercase = newPassword.any { it.isUpperCase() }
                    val hasDigit = newPassword.any { it.isDigit() }
                    val hasSpecial = newPassword.any { !it.isLetterOrDigit() }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("La nueva contraseña debe tener:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        PasswordRequirementRow("Mínimo 6 caracteres", hasMinChars)
                        PasswordRequirementRow("Al menos una minúscula", hasLowercase)
                        PasswordRequirementRow("Al menos una mayúscula", hasUppercase)
                        PasswordRequirementRow("Al menos un número", hasDigit)
                        PasswordRequirementRow("Al menos un carácter especial", hasSpecial)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPassword != confirmPassword) {
                            Toast.makeText(context, "Las contraseñas nuevas no coinciden", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        val hasMinChars = newPassword.length >= 6
                        val hasLowercase = newPassword.any { it.isLowerCase() }
                        val hasUppercase = newPassword.any { it.isUpperCase() }
                        val hasDigit = newPassword.any { it.isDigit() }
                        val hasSpecial = newPassword.any { !it.isLetterOrDigit() }
                        
                        if (!hasMinChars || !hasLowercase || !hasUppercase || !hasDigit || !hasSpecial) {
                            Toast.makeText(context, "La nueva contraseña no cumple los requisitos de seguridad", Toast.LENGTH_LONG).show()
                            return@Button
                        }

                        isChangingPassword = true
                        val user = auth.currentUser
                        val credential = EmailAuthProvider.getCredential(user?.email!!, currentPassword)

                        user.reauthenticate(credential)
                            .addOnSuccessListener {
                                user.updatePassword(newPassword)
                                    .addOnSuccessListener {
                                        isChangingPassword = false
                                        showChangePasswordDialog = false
                                        Toast.makeText(context, "Contraseña actualizada con éxito", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener { e ->
                                        isChangingPassword = false
                                        Toast.makeText(context, "Error al actualizar: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                            }
                            .addOnFailureListener { e ->
                                isChangingPassword = false
                                Toast.makeText(context, "Contraseña actual incorrecta", Toast.LENGTH_LONG).show()
                            }
                    },
                    enabled = currentPassword.isNotEmpty() && newPassword.isNotEmpty() && confirmPassword.isNotEmpty() && !isChangingPassword
                ) {
                    if (isChangingPassword) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White)
                    else Text("Cambiar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }, enabled = !isChangingPassword) { Text("Cancelar") }
            }
        )
    }

    // DIÁLOGO BORRAR CUENTA
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteDialog = false },
            title = { Text("¿Deseas borrar tu cuenta?", color = MaterialTheme.colorScheme.error) },
            text = {
                Column {
                    Text("Esta acción es irreversible. Se eliminarán todos tus datos en la aplicación (perfil, medallas y fotos).")
                    Spacer(Modifier.height(16.dp))
                    Text("Escribe 'CONFIRMAR' para continuar:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmDeleteText,
                        onValueChange = { confirmDeleteText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Escribe aquí...") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isDeleting = true
                        val uid = currentUser!!.uid
                        
                        // 1. Borrar en Firestore
                        db.collection("users").document(uid).delete()
                            .addOnSuccessListener {
                                // 2. Borrar en Storage (si existe)
                                storage.reference.child("profile_images/$uid.jpg").delete()
                                    .addOnCompleteListener {
                                        // 3. Borrar de Authentication
                                        currentUser.delete()
                                            .addOnSuccessListener {
                                                isDeleting = false
                                                showDeleteDialog = false
                                                auth.signOut() // Limpiar sesión local por seguridad
                                                Toast.makeText(context, "Cuenta eliminada correctamente", Toast.LENGTH_LONG).show()
                                                navController.navigate(Screen.Login.route) { 
                                                    popUpTo(0) { inclusive = true } 
                                                }
                                            }
                                            .addOnFailureListener { e ->
                                                isDeleting = false
                                                if (e is FirebaseAuthRecentLoginRequiredException) {
                                                    Toast.makeText(context, "Por seguridad, debes cerrar sesión y volver a entrar antes de borrar tu cuenta.", Toast.LENGTH_LONG).show()
                                                } else {
                                                    Toast.makeText(context, "Error al eliminar acceso: ${e.message}", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                    }
                            }
                            .addOnFailureListener { e ->
                                isDeleting = false
                                Toast.makeText(context, "Error al borrar datos: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    },
                    enabled = confirmDeleteText == "CONFIRMAR" && !isDeleting,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    if (isDeleting) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White)
                    else Text("Borrar definitivamente")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }, enabled = !isDeleting) { Text("Cancelar") }
            }
        )
    }

    if (showDarkModeDialog) {
        AlertDialog(
            onDismissRequest = { showDarkModeDialog = false },
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DarkMode, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text("Modo Oscuro")
                }
            },
            text = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Activar tema oscuro")
                    Switch(
                        checked = ThemeManager.isDarkMode,
                        onCheckedChange = { ThemeManager.setDarkMode(context, it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showDarkModeDialog = false }) {
                    Text("Aceptar")
                }
            }
        )
    }

    // DIÁLOGO SELECCIÓN DE DEPORTES
    if (showPhotoDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoDialog = false },
            title = { Text("Tu foto de perfil", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        AsyncImage(
                            model = userData?.profileImageUrl ?: R.drawable.logo,
                            contentDescription = null,
                            modifier = Modifier
                                .size(240.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.Black.copy(alpha = 0.05f)),
                            contentScale = ContentScale.Crop
                        )
                        if (isSaving) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "Puedes ver tu foto actual o cambiarla por una nueva",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    enabled = !isSaving
                ) {
                    Icon(Icons.Default.PhotoCamera, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Cambiar foto")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPhotoDialog = false }) {
                    Text("Cerrar")
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }

    if (showSportsDialog) {
        AlertDialog(
            onDismissRequest = { showSportsDialog = false },
            title = { 
                Text("Tus Deportes", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary) 
            },
            text = {
                Column {
                    Text("Selecciona los deportes que más te gustan para que podamos personalizar tu experiencia.", fontSize = 13.sp, color = Color.Gray)
                    Spacer(Modifier.height(20.dp))
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        allSports.chunked(2).forEach { rowSports ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowSports.forEach { sport ->
                                    val isSelected = tempSports.contains(sport)
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(50.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                        onClick = {
                                            if (isSelected) tempSports.remove(sport)
                                            else tempSports.add(sport)
                                        }
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = sport,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                                if (rowSports.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { 
                        showSportsDialog = false
                        if (!showEditForm) {
                            db.collection("users").document(currentUser!!.uid)
                                .update("sports", tempSports.toList())
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { 
                    Text("Confirmar Selección", fontWeight = FontWeight.Bold) 
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // FORMULARIO EDITAR PERFIL PREMIUM
    if (showEditForm) {
        val datePickerState = rememberDatePickerState()
        var showDatePicker by remember { mutableStateOf(false) }

        Dialog(
            onDismissRequest = { if (!isSaving) showEditForm = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header Estilo Premium
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showEditForm = false }, enabled = !isSaving) {
                            Icon(Icons.Default.Close, null, tint = Color.Gray)
                        }
                        Text(
                            "Editar Perfil", 
                            modifier = Modifier.weight(1f),
                            fontSize = 20.sp, 
                            fontWeight = FontWeight.Black, 
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.width(48.dp)) // Para centrar el texto
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp)
                    ) {
                        // Foto editable con Estética de Aura
                        Box(
                            Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(vertical = 32.dp)
                                .clickable { imagePickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            // Aura de fondo
                            Surface(
                                modifier = Modifier.size(130.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            ) {}
                            
                            AsyncImage(
                                model = selectedImageUri ?: userData?.profileImageUrl ?: R.drawable.logo,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = (-4).dp, y = (-4).dp),
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape,
                                shadowElevation = 4.dp
                            ) {
                                Icon(
                                    Icons.Default.CameraAlt, 
                                    null, 
                                    modifier = Modifier.padding(8.dp).size(20.dp), 
                                    tint = Color.White
                                )
                            }
                        }

                        Text(
                            "Información Básica",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Inputs Personalizados
                        val textFieldShape = RoundedCornerShape(16.dp)
                        val textFieldColors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = Color.Gray,
                            disabledBorderColor = Color.LightGray.copy(alpha = 0.3f),
                            disabledTextColor = Color.Gray,
                            disabledLabelColor = Color.Gray
                        )

                        OutlinedTextField(
                            value = tempName, 
                            onValueChange = { tempName = it }, 
                            label = { Text("Nombre de usuario") }, 
                            modifier = Modifier.fillMaxWidth(),
                            shape = textFieldShape,
                            colors = textFieldColors,
                            singleLine = true
                        )
                        
                        Spacer(Modifier.height(20.dp))
                        
                        OutlinedTextField(
                            value = currentUser?.email ?: userData?.email ?: "", 
                            onValueChange = {}, 
                            label = { Text("Email (No editable)") }, 
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = textFieldShape,
                            colors = textFieldColors,
                            enabled = false
                        )
                        
                        Spacer(Modifier.height(20.dp))
                        
                        OutlinedTextField(
                            value = tempPhone, 
                            onValueChange = { tempPhone = it }, 
                            label = { Text("Teléfono móvil") }, 
                            modifier = Modifier.fillMaxWidth(),
                            shape = textFieldShape,
                            colors = textFieldColors,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )
                        
                        Spacer(Modifier.height(20.dp))

                        OutlinedTextField(
                            value = tempBirthDate,
                            onValueChange = {},
                            label = { Text("Fecha de nacimiento") },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            shape = textFieldShape,
                            colors = textFieldColors,
                            trailingIcon = { 
                                IconButton(onClick = { showDatePicker = true }) { 
                                    Icon(Icons.Default.CalendarToday, null, tint = MaterialTheme.colorScheme.primary) 
                                } 
                            }
                        )

                        Spacer(Modifier.height(32.dp))
                        
                        Text(
                            "Preferencias y Otros",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Sexo (Dropdown Premium)
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedTextField(
                                value = tempGender,
                                onValueChange = {},
                                label = { Text("Sexo") },
                                modifier = Modifier.fillMaxWidth(),
                                readOnly = true,
                                shape = textFieldShape,
                                colors = textFieldColors,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                            )
                            Box(Modifier.matchParentSize().clickable { expanded = !expanded })
                            DropdownMenu(
                                expanded = expanded, 
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                genderOptions.forEach { gender ->
                                    DropdownMenuItem(
                                        text = { Text(gender) }, 
                                        onClick = { tempGender = gender; expanded = false }
                                    )
                                }
                            }
                        }
                        
                        Spacer(Modifier.height(20.dp))

                        // Selector de Deportes (Estilo Botón Selección)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = textFieldShape,
                            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                            color = Color.Transparent,
                            onClick = { showSportsDialog = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.SportsTennis, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(16.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Deportes favoritos", fontSize = 12.sp, color = Color.Gray)
                                    Text(
                                        if (tempSports.isEmpty()) "Ninguno seleccionado" else tempSports.joinToString(", "),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
                            }
                        }

                        Spacer(Modifier.height(40.dp))
                    }

                    // Botón de Guardar Premium (Sticky Bottom)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        tonalElevation = 8.dp,
                        shadowElevation = 16.dp,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Button(
                            onClick = {
                                isSaving = true
                                val uid = currentUser!!.uid
                                val trimmedName = tempName.trim()
                                
                                db.collection("users")
                                    .whereEqualTo("nameLower", trimmedName.lowercase())
                                    .get()
                                    .addOnSuccessListener { queryResult ->
                                        val isFoundInLower = queryResult.documents.any { it.id != uid }
                                        
                                        if (isFoundInLower) {
                                            isSaving = false
                                            Toast.makeText(context, "El nombre '${trimmedName}' ya está en uso", Toast.LENGTH_LONG).show()
                                        } else {
                                            db.collection("users")
                                                .whereEqualTo("name", trimmedName)
                                                .get()
                                                .addOnSuccessListener { secondaryResult ->
                                                    val isFoundExact = secondaryResult.documents.any { it.id != uid }
                                                    if (isFoundExact) {
                                                        isSaving = false
                                                        Toast.makeText(context, "El nombre '${trimmedName}' ya está en uso", Toast.LENGTH_LONG).show()
                                                    } else {
                                                        db.collection("users").get().addOnSuccessListener { allDocs ->
                                                            val isFoundInScan = allDocs.documents.any { doc ->
                                                                val otherName = doc.getString("name") ?: ""
                                                                doc.id != uid && otherName.trim().equals(trimmedName, ignoreCase = true)
                                                            }
                                                            if (isFoundInScan) {
                                                                isSaving = false
                                                                Toast.makeText(context, "El nombre '${trimmedName}' ya está en uso", Toast.LENGTH_LONG).show()
                                                            } else {
                                                                if (selectedImageUri != null) {
                                                                    val ref = storage.reference.child("profile_images/$uid.jpg")
                                                                    ref.putFile(selectedImageUri!!)
                                                                        .continueWithTask { task ->
                                                                            if (!task.isSuccessful) task.exception?.let { throw it }
                                                                            ref.downloadUrl
                                                                        }
                                                                        .addOnSuccessListener { downloadUri ->
                                                                            val updates = hashMapOf(
                                                                                "name" to trimmedName,
                                                                                "nameLower" to trimmedName.lowercase(),
                                                                                "phone" to tempPhone,
                                                                                "birthDate" to tempBirthDate,
                                                                                "gender" to tempGender,
                                                                                "sports" to tempSports.toList(),
                                                                                "profileImageUrl" to downloadUri.toString()
                                                                            )
                                                                            db.collection("users").document(uid)
                                                                                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                                                                                .addOnSuccessListener {
                                                                                    isSaving = false
                                                                                    showEditForm = false
                                                                                    Toast.makeText(context, "¡Perfil actualizado!", Toast.LENGTH_SHORT).show()
                                                                                }
                                                                        }
                                                                } else {
                                                                    val updates = hashMapOf(
                                                                        "name" to trimmedName,
                                                                        "nameLower" to trimmedName.lowercase(),
                                                                        "phone" to tempPhone,
                                                                        "birthDate" to tempBirthDate,
                                                                        "gender" to tempGender,
                                                                        "sports" to tempSports.toList()
                                                                    )
                                                                    db.collection("users").document(uid)
                                                                        .set(updates, com.google.firebase.firestore.SetOptions.merge())
                                                                        .addOnSuccessListener {
                                                                            isSaving = false
                                                                            showEditForm = false
                                                                            Toast.makeText(context, "¡Perfil actualizado!", Toast.LENGTH_SHORT).show()
                                                                        }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                        }
                                    }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            enabled = !isSaving
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text("Guardar Cambios", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        if (showDatePicker) {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let {
                            val cal = Calendar.getInstance()
                            cal.timeInMillis = it
                            tempBirthDate = "${cal.get(Calendar.DAY_OF_MONTH)}/${cal.get(Calendar.MONTH) + 1}/${cal.get(Calendar.YEAR)}"
                        }
                        showDatePicker = false
                    }) { Text("Aceptar") }
                },
                dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }

    if (showAchievements) {
        AlertDialog(
            onDismissRequest = { showAchievements = false },
            confirmButton = {
                TextButton(onClick = { showAchievements = false }) {
                    Text("Entendido", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EmojiEvents, null, tint = Color(0xFFFFD700), modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Medallas y Logros", fontWeight = FontWeight.Black)
                }
            },
            text = {
                Column {
                    Text(
                        "¡Prepárate para demostrar de qué estás hecho!",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Estamos diseñando un sistema de medallas exclusivas que podrás lucir en tu perfil:\n\n" +
                        "• MVP (Votado mejor jugador)\n" +
                        "• Fair Play (Conducta ejemplar)\n" +
                        "• Gran Anfitrión (Crea eventos populares)\n" +
                        "• Maratoniano (Participa en 5 deportes)",
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }

    if (showReviewsDialog) {
        ReviewsDialog(
            userData = userData,
            reviews = userReviews,
            onDismiss = { showReviewsDialog = false }
        )
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector, 
    title: String, 
    onClick: () -> Unit,
    titleColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (titleColor == MaterialTheme.colorScheme.error) titleColor else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = titleColor,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun PartialStar(rating: Double) {
    val progress = (rating / 5.0).toFloat()
    Box(contentAlignment = Alignment.Center) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = Color.LightGray,
            modifier = Modifier.size(24.dp)
        )
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(object : Shape {
                    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
                        return Outline.Rectangle(Rect(0f, 0f, size.width * progress, size.height))
                    }
                })
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Color(0xFFFFC107),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun AchievementsDialog(userMedals: List<String>, onDismiss: () -> Unit) {
    val achievements = listOf(
        Pair("Social", listOf(
            Triple("Novato", "¡Bienvenido a ByCompas!", Icons.Default.SentimentSatisfied),
            Triple("Socializer", "Has hecho 5 amigos nuevos.", Icons.Default.Groups),
            Triple("Compas Fiel", "Has jugado 10 partidos.", Icons.Default.Handshake)
        )),
        Pair("Deporte", listOf(
            Triple("MVP", "Te han votado como el mejor.", Icons.Default.Grade),
            Triple("Deportista Pro", "Perfil al 100%.", Icons.Default.EmojiEvents),
            Triple("Organizador", "Has creado un evento.", Icons.Default.Event)
        )),
        Pair("Reputación", listOf(
            Triple("Fair Play", "Reputación impecable.", Icons.Default.Shield),
            Triple("Explorador", "Eventos en otra ciudad.", Icons.Default.Explore)
        ))
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .padding(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 8.dp
        ) {
            Column(Modifier.padding(24.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mis Logros", 
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null)
                    }
                }

                LinearProgressIndicator(
                    progress = userMedals.size.toFloat() / 8f,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).height(8.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    achievements.forEach { (category, list) ->
                        Column {
                            Text(
                                text = category, 
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            @OptIn(ExperimentalLayoutApi::class)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                list.forEach { (name, desc, icon) ->
                                    val isUnlocked = userMedals.contains(name)
                                    AchievementItem(name, desc, icon, isUnlocked)
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
fun AchievementItem(name: String, desc: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isUnlocked: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(
                    if (isUnlocked) MaterialTheme.colorScheme.primaryContainer 
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
                .border(
                    width = 2.dp,
                    color = if (isUnlocked) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isUnlocked) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.6f),
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = name, 
            fontSize = 11.sp, 
            fontWeight = if (isUnlocked) FontWeight.Bold else FontWeight.Normal,
            color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else Color.Gray,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
fun ReviewsDialog(userData: UserData?, reviews: List<UserReview>, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .padding(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(32.dp),
            tonalElevation = 12.dp
        ) {
            Column(Modifier.padding(24.dp)) {
                // Header con botón cerrar
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Reputación", 
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Lo que dicen tus compas",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Resumen de Puntuación (Estilo Moderno)
                val hasRatings = (userData?.totalRatingsCount ?: 0) > 0
                
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (hasRatings) String.format("%.1f", userData?.rating ?: 0.0) else "-",
                                fontSize = 52.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                if (hasRatings) {
                                    for (i in 1..5) {
                                        val fill = (userData?.rating ?: 0.0) - (i - 1)
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = when {
                                                fill >= 1 -> Color(0xFFFFC107)
                                                fill > 0 -> Color(0xFFFFC107).copy(alpha = 0.6f)
                                                else -> Color.Gray.copy(alpha = 0.2f)
                                            },
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${userData?.totalRatingsCount ?: 0} votos",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                        }
                        
                        Box(modifier = Modifier.height(100.dp).width(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))
                        
                        Column(modifier = Modifier.weight(1.8f).padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (i in 5 downTo 0) {
                                val count = userData?.ratingCounts?.get(i.toString()) ?: 0
                                val total = userData?.totalRatingsCount ?: 1
                                val progress = count.toFloat() / total.coerceAtLeast(1).toFloat()
                                
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.width(28.dp)) {
                                        Text("$i", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Icon(Icons.Default.Star, null, modifier = Modifier.size(10.dp), tint = Color.Black)
                                    }
                                    Spacer(Modifier.width(4.dp))
                                    Box(modifier = Modifier.weight(1f).height(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(progress.coerceIn(0f, 1f))
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.horizontalGradient(
                                                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                                                    )
                                                )
                                        )
                                    }
                                    Text(
                                        text = "x$count", 
                                        fontSize = 9.sp, 
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(start = 8.dp).width(25.dp), 
                                        color = if (count > 0) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                
                Text(
                    text = "RESEÑAS RECIENTES", 
                    fontSize = 11.sp, 
                    fontWeight = FontWeight.Black, 
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 4.dp)
                )
                
                Spacer(Modifier.height(12.dp))

                if (reviews.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier.size(64.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = CircleShape
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.RateReview, 
                                    null, 
                                    modifier = Modifier.size(32.dp), 
                                    tint = Color.Gray.copy(alpha = 0.5f)
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Sin valoraciones aún", 
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "¡Anímate a jugar y a conocer gente!", 
                            color = Color.Gray.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        reviews.forEach { review ->
                            ReviewItem(review)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewItem(review: UserReview) {
    val sdf = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
    val dateStr = review.date?.toDate()?.let { sdf.format(it) } ?: "Reciente"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = review.reviewerImageUrl ?: R.drawable.logo,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(review.reviewerName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                    Text(dateStr, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(review.rating.toString(), fontWeight = FontWeight.SemiBold, color = Color(0xFFFFC107))
                    Icon(Icons.Default.Star, null, tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp))
                }
            }
            if (review.comment.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = review.comment,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PasswordRequirementRow(text: String, isMet: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (isMet) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            tint = if (isMet) Color(0xFF4CAF50) else Color(0xFFF44336),
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            color = if (isMet) Color(0xFF4CAF50) else Color(0xFFF44336)
        )
    }
}