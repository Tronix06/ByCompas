package com.bitronix.bycompa.ui.screens.login

import com.bitronix.bycompa.R
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.navigation.NavController
import com.bitronix.bycompa.navigation.Screen
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoginMode by remember { mutableStateOf(true) }
    var isLoggingIn by remember { mutableStateOf(false) }
    val passwordFocusRequester = remember { FocusRequester() }
    var emailError by remember { mutableStateOf<String?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    // GOOGLE SIGN IN CONFIG & LAUNCHER
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken!!, null)
            
            isLoggingIn = true
            auth.signInWithCredential(credential).addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        db.collection("users").document(userId).get().addOnSuccessListener { document ->
                            if (document.exists()) {
                                Toast.makeText(context, "¡Sesión iniciada!", Toast.LENGTH_SHORT).show()
                                navController.navigate("home_main") {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            } else {
                                auth.currentUser?.delete()?.addOnCompleteListener {
                                    auth.signOut()
                                    isLoggingIn = false
                                    Toast.makeText(context, "No estás registrado. Crea una cuenta primero.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }.addOnFailureListener {
                            auth.signOut()
                            isLoggingIn = false
                            Toast.makeText(context, "Error al verificar perfil.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    isLoggingIn = false
                    Toast.makeText(context, "Error de autenticación.", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            isLoggingIn = false
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    val premiumGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            Color(0xFF1565C0)
        )
    )

    Box(modifier = Modifier.fillMaxSize().background(premiumGradient)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(120.dp))
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isLoginMode) "Bienvenido" else "Únete a ByCompa",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = (-1).sp
                        )
                        Text(
                            text = if (isLoginMode) "Inicia sesión para continuar" else "Crea tu cuenta y empieza a jugar",
                            fontSize = 15.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { newValue ->
                            val transformed = newValue.lowercase()
                            val filtered = transformed.filter { char ->
                                char.isLowerCase() || char.isDigit() || char == '.' || char == '@' || char == '_' || char == '-'
                            }
                            if (filtered.length != newValue.length) {
                                emailError = "Caracteres no válidos"
                            } else {
                                emailError = null
                            }
                            email = filtered
                        },
                        placeholder = { Text("Tu correo electrónico", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoggingIn,
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { passwordFocusRequester.requestFocus() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White.copy(alpha = 0.9f),
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    if (emailError != null) {
                        Text(
                            text = emailError!!,
                            color = Color(0xFFFFCDD2), // Rojo muy suave para que se vea sobre el azul
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp).align(Alignment.Start)
                        )
                    }

                    var passwordVisible by remember { mutableStateOf(false) }

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text("Tu contraseña", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary) },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(passwordFocusRequester),
                        enabled = !isLoggingIn,
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = Color.Gray
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White.copy(alpha = 0.9f),
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    if (isLoginMode) {
                        TextButton(
                            onClick = { 
                                resetEmail = email
                                showResetDialog = true 
                            },
                            modifier = Modifier.align(Alignment.End),
                            enabled = !isLoggingIn
                        ) {
                            Text("¿Olvidaste tu contraseña?", color = Color.White.copy(alpha = 0.8f))
                        }
                    } else {
                        val hasMinChars = password.length >= 6
                        val hasLowercase = password.any { it.isLowerCase() }
                        val hasUppercase = password.any { it.isUpperCase() }
                        val hasDigit = password.any { it.isDigit() }
                        val hasSpecial = password.any { !it.isLetterOrDigit() }

                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("La contraseña debe tener:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f))
                            PasswordRequirementRow("Mínimo 6 caracteres", hasMinChars)
                            PasswordRequirementRow("Al menos una minúscula", hasLowercase)
                            PasswordRequirementRow("Al menos una mayúscula", hasUppercase)
                            PasswordRequirementRow("Al menos un número", hasDigit)
                            PasswordRequirementRow("Al menos un carácter especial", hasSpecial)
                        }
                    }

                    Button(
                        onClick = {
                            if (email.isNotEmpty() && password.isNotEmpty()) {
                                isLoggingIn = true
                                if (isLoginMode) {
                                    auth.signInWithEmailAndPassword(email.trim(), password)
                                        .addOnCompleteListener { task ->
                                            if (task.isSuccessful) {
                                                navController.navigate("home_main") {
                                                    popUpTo(Screen.Login.route) { inclusive = true }
                                                }
                                            } else {
                                                isLoggingIn = false
                                                Toast.makeText(context, "Credenciales incorrectas", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                } else {
                                    auth.createUserWithEmailAndPassword(email.trim(), password)
                                        .addOnCompleteListener { task ->
                                            if (task.isSuccessful) {
                                                val userId = auth.currentUser?.uid
                                                val tempName = email.substringBefore("@")
                                                val userMap = hashMapOf(
                                                    "uid" to userId,
                                                    "name" to tempName,
                                                    "nameLower" to tempName.lowercase(),
                                                    "email" to email.trim(),
                                                    "rating" to 5.0,
                                                    "totalReviews" to 0,
                                                    "radarRadius" to 15.0,
                                                    "sports" to emptyList<String>()
                                                )
                                                if (userId != null) {
                                                    db.collection("users").document(userId).set(userMap)
                                                        .addOnSuccessListener {
                                                            navController.navigate("home_main?startTab=perfil") {
                                                                popUpTo(Screen.Login.route) { inclusive = true }
                                                            }
                                                        }
                                                }
                                            } else {
                                                isLoggingIn = false
                                                Toast.makeText(context, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                }
                            } else {
                                Toast.makeText(context, "Rellena ambos campos", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = !isLoggingIn,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (isLoggingIn && isLoginMode) CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary)
                        else Text(if (isLoginMode) "Entrar" else "Registrarse", fontSize = 18.sp, fontWeight = FontWeight.Black)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // BOTÓN GOOGLE CIRCULAR
                    if (isLoginMode) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("O inicia sesión con", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Surface(
                                onClick = {
                                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                        .requestIdToken(context.getString(R.string.bycompa_web_client_id))
                                        .requestEmail()
                                        .build()
                                    val googleSignInClient = GoogleSignIn.getClient(context, gso)
                                    googleSignInClient.signOut().addOnCompleteListener {
                                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                                    }
                                },
                                modifier = Modifier.size(56.dp),
                                shape = CircleShape,
                                color = Color.White,
                                shadowElevation = 8.dp,
                                enabled = !isLoggingIn
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Image(
                                        painter = painterResource(id = R.drawable.logo_google),
                                        contentDescription = null,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(onClick = { isLoginMode = !isLoginMode }, enabled = !isLoggingIn) {
                        Text(
                            if (isLoginMode) "¿No tienes cuenta? Regístrate aquí"
                            else "¿Ya tienes cuenta? Inicia sesión",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(28.dp),
            icon = { Icon(Icons.Default.LockReset, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp)) },
            title = { 
                Text(
                    "Recuperar cuenta", 
                    fontWeight = FontWeight.Black, 
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ) 
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Introduce tu email y te enviaremos un enlace mágico para que puedas crear una nueva contraseña y volver a jugar.",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        lineHeight = 20.sp
                    )
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it.lowercase().trim() },
                        placeholder = { Text("ejemplo@correo.com") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.LightGray
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (resetEmail.isNotEmpty()) {
                            auth.sendPasswordResetEmail(resetEmail.trim())
                                .addOnCompleteListener { resetTask ->
                                    if (resetTask.isSuccessful) {
                                        Toast.makeText(context, "¡Correo enviado! Revisa tu bandeja de entrada", Toast.LENGTH_LONG).show()
                                        showResetDialog = false
                                    } else {
                                        // En la mayoría de proyectos modernos con protección de enumeración activada,
                                        // esto solo fallará si el correo tiene un formato inválido.
                                        Toast.makeText(context, "Error: No se pudo enviar el correo. Revisa el formato.", Toast.LENGTH_LONG).show()
                                    }
                                }
                        } else {
                            Toast.makeText(context, "Por favor, introduce tu email", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Enviar Correo", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun PasswordRequirementRow(text: String, isMet: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (isMet) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            tint = if (isMet) Color(0xFF4CAF50) else Color(0xFFF44336).copy(alpha = 0.9f),
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            color = if (isMet) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.7f)
        )
    }
}