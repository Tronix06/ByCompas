package com.bitronix.bycompas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bitronix.bycompas.navigation.Screen
import com.bitronix.bycompas.ui.screens.login.LoginScreen
import com.bitronix.bycompas.ui.screens.home.HomeScreen
import com.bitronix.bycompas.ui.screens.home.CreateEventScreen // Asegúrate de que el package coincida
import com.bitronix.bycompas.ui.screens.onboarding.OnboardingScreen
import com.bitronix.bycompas.ui.screens.onboarding.PermissionsScreen
import com.bitronix.bycompas.ui.theme.ByCompasTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ByCompasTheme {
                val navController = rememberNavController()
                val auth = FirebaseAuth.getInstance()

                // Si hay usuario, la ruta inicial es el contenedor Home (que tiene las 5 pestañas)
                // Si no, lo mandamos al Login.
                val startRoute = if (auth.currentUser != null) "home_main" else Screen.Login.route

                NavHost(
                    navController = navController,
                    startDestination = startRoute
                ) {
                    // --- SECCIÓN AUTENTICACIÓN ---
                    composable(Screen.Login.route) {
                        LoginScreen(navController)
                    }

                    // --- SECCIÓN CONFIGURACIÓN INICIAL (ONBOARDING) ---
                    composable("onboarding") {
                        OnboardingScreen(navController)
                    }
                    composable("permissions") {
                        PermissionsScreen(navController)
                    }

                    // --- SECCIÓN PRINCIPAL (CONTENEDOR DE PESTAÑAS) ---
                    // Usamos una ruta genérica "home_main" para el Scaffold que contiene la BottomBar
                    composable("home_main") {
                        HomeScreen(navController)
                    }

                    // --- SECCIÓN EVENTOS (PANTALLA COMPLETA FUERA DE LA BOTTOMBAR) ---
                    composable(Screen.CreateEvent.route) {
                        CreateEventScreen(navController)
                    }
                }
            }
        }
    }
}