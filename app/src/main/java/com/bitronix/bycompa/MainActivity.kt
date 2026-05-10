package com.bitronix.bycompa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bitronix.bycompa.navigation.Screen
import com.bitronix.bycompa.ui.screens.login.LoginScreen
import com.bitronix.bycompa.ui.screens.home.HomeScreen
import com.bitronix.bycompa.ui.screens.home.IndividualChatScreen
import com.bitronix.bycompa.ui.screens.home.UserEventsScreen
import com.bitronix.bycompa.ui.screens.home.RequestManagementScreen
import com.bitronix.bycompa.ui.screens.home.EventHistoryScreen
import com.bitronix.bycompa.ui.screens.events.CreateEventScreen
import com.bitronix.bycompa.ui.screens.events.LocationPickerScreen
import com.bitronix.bycompa.ui.screens.home.RatingScreen
import com.bitronix.bycompa.ui.theme.ByCompasTheme
import com.bitronix.bycompa.utils.ThemeManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {

    override fun onResume() {
        super.onResume()
        updatePresence(true)
    }

    override fun onPause() {
        super.onPause()
        updatePresence(false)
    }

    private fun updatePresence(isOnline: Boolean) {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid
        if (uid != null) {
            val db = FirebaseFirestore.getInstance()
            val data = hashMapOf<String, Any>(
                "isOnline" to isOnline,
                "lastSeen" to System.currentTimeMillis()
            )
            db.collection("users").document(uid).set(data, com.google.firebase.firestore.SetOptions.merge())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.initialize(this)
        enableEdgeToEdge()
        setContent {
            ByCompasTheme(darkTheme = ThemeManager.isDarkMode) {
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

                    // --- SECCIÓN CONFIGURACIÓN INICIAL (Eliminada por simplificación) ---

                    // --- SECCIÓN PRINCIPAL (CONTENEDOR DE PESTAÑAS) ---
                    // Usamos una ruta genérica "home_main" para el Scaffold que contiene la BottomBar
                    // Aceptamos un argumento opcional "startTab"
                    composable(
                        route = "home_main?startTab={startTab}&subTab={subTab}",
                        arguments = listOf(
                            navArgument("startTab") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                            navArgument("subTab") {
                                type = NavType.IntType
                                defaultValue = 0
                            }
                        )
                    ) { backStackEntry ->
                        val startTab = backStackEntry.arguments?.getString("startTab")
                        val subTab = backStackEntry.arguments?.getInt("subTab") ?: 0
                        HomeScreen(navController, startTab, subTab)
                    }

                    // --- SECCIÓN EVENTOS (PANTALLA COMPLETA FUERA DE LA BOTTOMBAR) ---
                    composable(Screen.CreateEvent.route) {
                        CreateEventScreen(navController)
                    }
                    
                    composable(Screen.LocationPicker.route) {
                        LocationPickerScreen(navController)
                    }

                    // --- SECCIÓN CHATS INDIVIDUALES ---
                    composable(
                        route = Screen.IndividualChat.route,
                        arguments = listOf(
                            navArgument("chatId") { type = NavType.StringType },
                            navArgument("otherName") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
                        val otherName = backStackEntry.arguments?.getString("otherName") ?: ""
                        IndividualChatScreen(navController, chatId, otherName)
                    }

                    // --- NUEVAS PANTALLAS DE INICIO ---
                    composable(Screen.UserEvents.route) {
                        UserEventsScreen(navController)
                    }
                    composable(Screen.RequestManagement.route) {
                        RequestManagementScreen(navController)
                    }
                    composable(Screen.EventHistory.route) {
                        EventHistoryScreen(navController)
                    }
                    composable(
                        route = Screen.Rating.route,
                        arguments = listOf(
                            navArgument("chatId") { type = NavType.StringType },
                            navArgument("eventName") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
                        val eventName = backStackEntry.arguments?.getString("eventName") ?: ""
                        RatingScreen(navController, chatId, eventName)
                    }
                }
            }
        }
    }
}