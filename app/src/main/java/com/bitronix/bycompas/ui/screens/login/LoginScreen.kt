package com.bitronix.bycompas.ui.screens.login

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreen() {
    // Estas variables guardan lo que el usuario escribe en tiempo real
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Column apila los elementos uno debajo de otro verticalmente
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Título de la App
        Text(
            text = "BiCompas",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Campo de texto para el Email
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Campo de texto para la Contraseña
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(), // Oculta la contraseña con asteriscos
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Botón de Entrar
        Button(
            onClick = {
                // TODO: Aquí conectaremos Firebase más adelante
                println("Usuario intentó loguearse con: $email")
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Entrar", fontSize = 18.sp)
        }
    }
}