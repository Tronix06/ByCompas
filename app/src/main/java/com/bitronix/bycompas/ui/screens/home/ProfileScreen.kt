package com.bitronix.bycompas.ui.screens.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
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
import com.bitronix.bycompas.navigation.Screen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun ProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser

    var userName by remember { mutableStateOf("Cargando...") }
    var userEmail by remember { mutableStateOf(currentUser?.email ?: "") }
    var userRating by remember { mutableDoubleStateOf(0.0) }
    var radarRadius by remember { mutableFloatStateOf(15f) }
    var userSports by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser) {
        currentUser?.uid?.let { uid ->
            db.collection("users").document(uid).get().addOnSuccessListener { doc ->
                userName = doc.getString("name") ?: "Deportista"
                userRating = doc.getDouble("rating") ?: 5.0
                radarRadius = doc.getDouble("radarRadius")?.toFloat() ?: 15f

                // Corrección del casteo seguro aquí:
                val sportsRaw = doc.get("sports") as? List<*>
                userSports = sportsRaw?.filterIsInstance<String>() ?: emptyList()

                isLoading = false
            }
        }
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(100.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, null, Modifier.size(60.dp), tint = Color.White)
            }
            Spacer(Modifier.height(16.dp))
            Text(userName, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(userEmail, color = Color.Gray)

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                Text("⭐ $userRating", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(Modifier.width(16.dp))
                AssistChip(onClick = {}, label = { Text("Buen rollo") })
                Spacer(Modifier.width(8.dp))
                AssistChip(onClick = {}, label = { Text("Puntual") })
            }

            Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Mis Deportes", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    userSports.forEach { Text("- $it", modifier = Modifier.padding(vertical = 2.dp)) }
                }
            }

            Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Radio Radar: ${radarRadius.toInt()} km", fontWeight = FontWeight.Bold)
                    Slider(value = radarRadius, onValueChange = { radarRadius = it }, valueRange = 1f..50f, onValueChangeFinished = {
                        currentUser?.uid?.let { db.collection("users").document(it).update("radarRadius", radarRadius.toDouble()) }
                        Toast.makeText(context, "Radio actualizado", Toast.LENGTH_SHORT).show()
                    })
                }
            }

            Spacer(Modifier.height(24.dp))
            OutlinedButton(onClick = { auth.signOut(); navController.navigate(Screen.Login.route) { popUpTo(0) } }, modifier = Modifier.fillMaxWidth()) {
                Text("Cerrar Sesión")
            }
            Button(onClick = { showDeleteDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text("Borrar Cuenta")
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("¿Eliminar cuenta?") },
            confirmButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Borrar", color = Color.Red) } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") } }
        )
    }
}