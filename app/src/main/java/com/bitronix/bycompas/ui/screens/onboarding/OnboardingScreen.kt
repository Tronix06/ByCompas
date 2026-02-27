package com.bitronix.bycompas.ui.screens.onboarding

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser

    val deportesConfig = mapOf(
        "Pádel" to listOf("Iniciación", "Intermedio", "Avanzado", "Competición"),
        "Fútbol" to listOf("Pachanga / Amigos", "Amateur", "Federado / Alto Nivel"),
        "Running" to listOf("Trote / Caminata", "5:30 - 6:30 min/km", "< 5:00 min/km"),
        "Ciclismo" to listOf("Paseo Urbano", "Ruta Media (20-50km)", "Ruta Larga / Montaña"),
        "Tenis" to listOf("Iniciación", "Intermedio", "Avanzado", "Competición"),
        "Baloncesto" to listOf("Pachanga / Parque", "Amateur", "Federado")
    )

    var selecciones by remember { mutableStateOf(mapOf<String, String>()) }
    var isSaving by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text("¿A qué juegas?", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(
            text = "Configura tus deportes y tu nivel para que el radar sea preciso.",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        deportesConfig.forEach { (deporte, niveles) ->
            val isSelected = selecciones.containsKey(deporte)

            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { checked ->
                                val nuevasSelecciones = selecciones.toMutableMap()
                                if (checked) {
                                    nuevasSelecciones[deporte] = niveles[0]
                                } else {
                                    nuevasSelecciones.remove(deporte)
                                }
                                selecciones = nuevasSelecciones
                            }
                        )
                        Text(text = deporte, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }

                    if (isSelected) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Indica tu nivel / ritmo:")

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            niveles.forEach { nivel ->
                                FilterChip(
                                    selected = selecciones[deporte] == nivel,
                                    onClick = {
                                        val nuevasSelecciones = selecciones.toMutableMap()
                                        nuevasSelecciones[deporte] = nivel
                                        selecciones = nuevasSelecciones
                                    },
                                    label = { Text(nivel) }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (selecciones.isEmpty()) {
                    Toast.makeText(context, "Por favor, selecciona al menos un deporte", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                isSaving = true

                val listaDeportesFirebase = selecciones.map { "${it.key} - ${it.value}" }

                if (currentUser != null) {
                    db.collection("users").document(currentUser.uid)
                        .update("sports", listaDeportesFirebase)
                        .addOnSuccessListener {
                            isSaving = false

                            // ¡CAMBIO AQUÍ! Ahora viajamos a "permissions" en vez de "home"
                            navController.navigate("permissions") {
                                popUpTo("onboarding") { inclusive = true }
                            }
                        }
                        .addOnFailureListener { e ->
                            isSaving = false
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !isSaving
        ) {
            if (isSaving) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
            } else {
                Text("Continuar", fontSize = 18.sp)
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}