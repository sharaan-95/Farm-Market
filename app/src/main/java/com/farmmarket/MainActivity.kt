package com.farmmarket

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF2E7D32), secondary = Color(0xFF8BC34A),
                    primaryContainer = Color(0xFFDCEDC8), background = Color(0xFFF7FAF4), surface = Color.White,
                )
            ) {
                val vm: AppVm = viewModel()
                if (vm.loggedIn) HomeScreen(vm) else LoginScreen(vm)
            }
        }
    }
}
