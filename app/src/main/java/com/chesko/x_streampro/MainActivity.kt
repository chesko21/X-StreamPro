package com.chesko.x_streampro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.annotation.OptIn
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.chesko.x_streampro.data.model.Category
import com.chesko.x_streampro.data.model.LiveStream
import com.chesko.x_streampro.data.model.UserSession
import com.chesko.x_streampro.ui.screens.HomeScreen
import com.chesko.x_streampro.ui.screens.ChannelScreen
import com.chesko.x_streampro.ui.screens.PlayerScreen
import com.chesko.x_streampro.ui.screens.SplashScreen
import com.chesko.x_streampro.ui.screens.XtreamLoginScreen
import com.chesko.x_streampro.ui.theme.XStreamProTheme
import androidx.media3.common.util.UnstableApi

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // Setup edge-to-edge with a transparent/dark configuration
        enableEdgeToEdge()

        setContent {
            XStreamProTheme {
                AppNavigation()
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    var currentSession by remember { mutableStateOf<UserSession?>(null) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var selectedChannel by remember { mutableStateOf<LiveStream?>(null) }
    var channelList by remember { mutableStateOf<List<LiveStream>>(emptyList()) }

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(onNavigateToNext = {
                navController.navigate("login") {
                    popUpTo("splash") { inclusive = true }
                }
            })
        }
        composable("login") {
            XtreamLoginScreen(onLoginSuccess = { session ->
                currentSession = session
                navController.navigate("home")
            })
        }
        composable("home") {
            val session = currentSession
            if (session != null) {
                HomeScreen(
                    session = session,
                    onLogout = {
                        currentSession = null
                        navController.navigate("login") {
                            popUpTo("home") { inclusive = true }
                        }
                    },
                    onCategoryClick = { category ->
                        selectedCategory = category
                        navController.navigate("channels")
                    }
                )
            }
        }
        composable("channels") {
            val session = currentSession
            val category = selectedCategory
            if (session != null && category != null) {
                ChannelScreen(
                    session = session,
                    categoryId = category.categoryId,
                    categoryName = category.categoryName,
                    onBack = { navController.popBackStack() },
                    onChannelClick = { channel, list ->
                        selectedChannel = channel
                        channelList = list
                        navController.navigate("player")
                    }
                )
            }
        }
        composable("player") {
            val session = currentSession
            val channel = selectedChannel
            if (session != null && channel != null) {
                PlayerScreen(
                    session = session,
                    initialChannel = channel,
                    channels = channelList,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}