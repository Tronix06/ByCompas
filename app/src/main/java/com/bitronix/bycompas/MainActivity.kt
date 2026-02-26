// SI TU PAQUETE DE ARRIBA TIENE 'Y', DEJA ESTA LÍNEA ASÍ. SI TIENE 'I', CÁMBIALO.
package com.bitronix.bycompas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

// --- IMPORTANTE: RUTA HACIA LOGIN SCREEN ---
// Fíjate en tus carpetas a la izquierda. Si creaste la carpeta "ui", luego "screens" y luego "login", la ruta es esta.
// Si tu proyecto se llama "bicompas" (con I), cambia la palabra "bycompas" de esta línea por "bicompas":
import com.bitronix.bycompas.ui.screens.login.LoginScreen

// --- IMPORTANTE: RUTA HACIA EL TEMA ---
// Lo mismo aquí. Cambia "bycompas" por "bicompas" si es necesario.
import com.bitronix.bycompas.ui.theme.ByCompasTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // El tema envuelve a la aplicación.
            ByCompasTheme {
                // Aquí llamamos a la función de la pantalla.
                LoginScreen()
            }
        }
    }
}