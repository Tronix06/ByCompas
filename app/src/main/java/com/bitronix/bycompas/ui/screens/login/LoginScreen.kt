package com.bitronix.bycompas.ui.screens.login

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.bitronix.bycompas.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun LoginScreen(navController: NavController) {
    // Variables para guardar lo que escribe el usuario
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    // Interruptor: ¿Estamos iniciando sesión (true) o registrando (false)?
    var isLoginMode by remember { mutableStateOf(true) }

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // --- LOGOTIPO (TAMAÑO XL) ---
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "Logo de ByCompas",
            modifier = Modifier
                .fillMaxWidth(0.95f) // Le damos permiso para usar casi todo el ancho (95%)
                .height(220.dp),     // Subimos el "techo" drásticamente para que pueda crecer
            contentScale = ContentScale.Fit // Mantiene las proporciones sin deformarlo
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- TÍTULO ---
        Text(
            text = if (isLoginMode) "Bienvenido a ByCompas" else "Crea tu cuenta",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        // --- CAMPO NOMBRE (Solo si se está registrando) ---
        if (!isLoginMode) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Tu nombre o apodo (Ej: BiTronix)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- CAMPO EMAIL ---
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- CAMPO CONTRASEÑA ---
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        // --- RECUPERAR CONTRASEÑA (Solo en modo Login) ---
        if (isLoginMode) {
            TextButton(
                onClick = {
                    if (email.isNotEmpty()) {
                        auth.sendPasswordResetEmail(email)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(context, "Correo de recuperación enviado", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                    } else {
                        Toast.makeText(context, "Escribe tu email arriba primero", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("¿Olvidaste tu contraseña?")
            }
        } else {
            Spacer(modifier = Modifier.height(32.dp))
        }

        // --- BOTÓN PRINCIPAL (Login o Registro) ---
        Button(
            onClick = {
                if (email.isNotEmpty() && password.isNotEmpty()) {
                    if (isLoginMode) {
                        // LÓGICA DE INICIO DE SESIÓN
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(context, "¡Sesión iniciada!", Toast.LENGTH_SHORT).show()
                                    navController.navigate("home")
                                } else {
                                    Toast.makeText(context, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                    } else {
                        // LÓGICA DE REGISTRO
                        if (name.isNotEmpty()) {
                            auth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val userId = auth.currentUser?.uid

                                        val userMap = hashMapOf(
                                            "uid" to userId,
                                            "name" to name,
                                            "email" to email,
                                            "rating" to 5.0,
                                            "totalReviews" to 0
                                        )

                                        if (userId != null) {
                                            db.collection("users").document(userId).set(userMap)
                                                .addOnSuccessListener {
                                                    Toast.makeText(context, "¡Cuenta creada con éxito!", Toast.LENGTH_SHORT).show()
                                                    navController.navigate("home")
                                                }
                                                .addOnFailureListener { e ->
                                                    Toast.makeText(context, "Error al guardar perfil: ${e.message}", Toast.LENGTH_LONG).show()
                                                }
                                        }
                                    } else {
                                        Toast.makeText(context, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                        } else {
                            Toast.makeText(context, "Por favor, escribe un nombre", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "Rellena email y contraseña", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text(if (isLoginMode) "Entrar" else "Registrarse", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- INTERRUPTOR PARA CAMBIAR DE MODO ---
        TextButton(onClick = { isLoginMode = !isLoginMode }) {
            Text(
                if (isLoginMode) "¿No tienes cuenta? Regístrate aquí"
                else "¿Ya tienes cuenta? Inicia sesión"
            )
        }
    }
}