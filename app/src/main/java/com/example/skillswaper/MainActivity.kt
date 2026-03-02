package com.example.skillswaper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.*
import com.example.skillswaper.ui.navigation.MainNavigation
import com.example.skillswaper.ui.screens.auth.LoginScreen
import com.example.skillswaper.ui.screens.auth.SignupScreen
import com.example.skillswaper.ui.screens.auth.ForgotPasswordScreen
import com.example.skillswaper.ui.theme.SkillSwaperTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SkillSwaperTheme {
                var currentScreen by remember { mutableStateOf("login") }

                Surface(modifier = Modifier.fillMaxSize()) {
                    when (currentScreen) {
                        "login" -> LoginScreen(
                            onLoginSuccess = { currentScreen = "main" },
                            onNavigateToSignup = { currentScreen = "signup" },
                            onNavigateToForgotPassword = { currentScreen = "forgot_password" }
                        )
                        "signup" -> SignupScreen(
                            onSignupSuccess = { currentScreen = "main" },
                            onNavigateToLogin = { currentScreen = "login" }
                        )
                        "forgot_password" -> ForgotPasswordScreen(
                            onNavigateBack = { currentScreen = "login" }
                        )
                        "main" -> MainNavigation(onSignOut = { currentScreen = "login" })
                    }
                }
            }
        }
    }
}
