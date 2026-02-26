package com.bitronix.bycompas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.MenuProvider
import com.bitronix.bycompas.ui.theme.ByCompasTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ByCompasTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun addMenuProvider(
        provider: MenuProvider,
        owner: androidx.lifecycle.LifecycleOwner,
        state: androidx.lifecycle.Lifecycle.State
    ) {
        TODO("Not yet implemented")
    }

    override fun addMenuProvider(
        provider: MenuProvider,
        owner: androidx.lifecycle.LifecycleOwner,
        state: androidx.lifecycle.Lifecycle.State
    ) {
        TODO("Not yet implemented")
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ByCompasTheme {
        Greeting("Android")
    }
}