package com.bitronix.bycompas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import com.bitronix.bycompas.ui.screens.login.LoginScreen
import com.bitronix.bycompas.ui.screens.home.HomeScreen
import com.bitronix.bycompas.ui.theme.ByCompasTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ByCompasTheme {
                val navController = rememberNavController()

                // MAGIA: Comprobamos si el usuario ya tiene la sesión iniciada
                val auth = FirebaseAuth.getInstance()
                val startRoute = if (auth.currentUser != null) "home" else "login"

                // Le pasamos esa variable a "startDestination"
                NavHost(navController = navController, startDestination = startRoute) {
                    composable("login") { LoginScreen(navController) }
                    composable("home") { HomeScreen(navController) }

                    composable("calendar") { /* Próximamente */ }
                    composable("profile") { /* Próximamente */ }
                }
            }
        }
    }
}