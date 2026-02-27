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
import com.bitronix.bycompas.ui.screens.onboarding.OnboardingScreen
import com.bitronix.bycompas.ui.screens.onboarding.PermissionsScreen // ¡NUEVO!
import com.bitronix.bycompas.ui.theme.ByCompasTheme
import com.google.firebase.auth.FirebaseAuth
import com.bitronix.bycompas.ui.screens.events.CreateEventScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ByCompasTheme {
                val navController = rememberNavController()

                val auth = FirebaseAuth.getInstance()
                val startRoute = if (auth.currentUser != null) "home" else "login"

                NavHost(navController = navController, startDestination = startRoute) {
                    composable("login") { LoginScreen(navController) }
                    composable("onboarding") { OnboardingScreen(navController) }

                    // ¡NUEVA PANTALLA DE PERMISOS!
                    composable("permissions") { PermissionsScreen(navController) }
                    composable("create_event") { CreateEventScreen(navController) }

                    composable("home") { HomeScreen(navController) }
                    composable("calendar") { /* Próximamente */ }
                    composable("profile") { /* Próximamente */ }
                }
            }
        }
    }
}