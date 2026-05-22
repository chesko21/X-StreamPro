package com.chesko.x_streampro.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chesko.x_streampro.R
import com.chesko.x_streampro.data.XtreamRepository
import com.chesko.x_streampro.data.model.UserSession
import com.chesko.x_streampro.ui.theme.XStreamProTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun XtreamLoginScreen(onLoginSuccess: (UserSession) -> Unit) {
    var server by remember { mutableStateOf("http://psk-sdk.ddno.us:8080") }
    var username by remember { mutableStateOf("Etiziabd8266") }
    var password by remember { mutableStateOf("scbdA0tracVTXOb") }
    var isLoading by remember { mutableStateOf(false) }

    var notificationMessage by remember { mutableStateOf<String?>(null) }
    var isNotificationError by remember { mutableStateOf(false) }
    var showNotification by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val repository = remember { XtreamRepository() }
    
    // Animation states for entry
    val alphaAnim = remember { Animatable(0f) }
    val offsetYAnim = remember { Animatable(50f) }

    LaunchedEffect(Unit) {
        launch {
            alphaAnim.animateTo(1f, tween(1000, easing = FastOutSlowInEasing))
        }
        launch {
            offsetYAnim.animateTo(0f, spring(stiffness = Spring.StiffnessLow))
        }
    }

    fun showToast(message: String, isError: Boolean = true) {
        scope.launch {
            notificationMessage = message
            isNotificationError = isError
            showNotification = true
            delay(3000)
            showNotification = false
        }
    }

    Scaffold(
        containerColor = Color(0xFF050505)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            // Background Decorative Elements
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.1f)
                    .background(
                        Brush.verticalGradient(
                            listOf(MaterialTheme.colorScheme.primary, Color.Transparent)
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .graphicsLayer {
                        alpha = alphaAnim.value
                        translationY = offsetYAnim.value
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Sharp Premium Logo (Clean)
                Image(
                    painter = painterResource(id = R.drawable.app_icon_android),
                    contentDescription = null,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(28.dp))
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "X-STREAM PRO",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 6.sp,
                        color = Color.White
                    )
                )

                Text(
                    text = "ACCESS YOUR PREMIUM CONTENT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Light,
                        color = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(56.dp))

                // Login Form Container (Subtle Glass Effect)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(24.dp))
                        .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LoginTextField(
                        value = server,
                        onValueChange = { server = it },
                        label = "Server URL",
                        icon = Icons.Default.Dns,
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    LoginTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = "Username",
                        icon = Icons.Default.Person,
                        imeAction = ImeAction.Next
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    LoginTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        icon = Icons.Default.Lock,
                        isPassword = true,
                        imeAction = ImeAction.Done
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            if (server.isBlank() || username.isBlank() || password.isBlank()) {
                                showToast("Please fill in all login details")
                            } else {
                                isLoading = true
                                scope.launch {
                                    try {
                                        val response = repository.login(server, username, password)
                                        if (response.userInfo?.auth == 1) {
                                            showToast("Login Successful! Welcome", isError = false)
                                            delay(1000)
                                            onLoginSuccess(UserSession(server, username, password))
                                        } else {
                                            showToast("Login Failed: Invalid Username or Password")
                                        }
                                    } catch (e: Exception) {
                                        showToast("An error occurred: ${e.localizedMessage}")
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .width(180.dp)
                            .height(50.dp),
                        shape = RoundedCornerShape(25.dp), // Pill shaped
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.Black
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                "SIGN IN",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                )
                            )
                        }
                    }
                }
            }

            // Enhanced Notification Overlay
            AnimatedVisibility(
                visible = showNotification,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Surface(
                    modifier = Modifier
                        .padding(top = 40.dp)
                        .padding(horizontal = 24.dp),
                    color = if (isNotificationError) Color(0xFFE53935) else Color(0xFF43A047),
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 10.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isNotificationError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = notificationMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LoginTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.bodySmall) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
        trailingIcon = {
            if (isPassword) {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = Color.Gray,
            focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
            unfocusedLeadingIconColor = Color.Gray,
            focusedTrailingIconColor = MaterialTheme.colorScheme.primary,
            unfocusedTrailingIconColor = Color.Gray,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedContainerColor = Color.White.copy(alpha = 0.03f)
        )
    )
}

@Preview(showBackground = true)
@Composable
fun XtreamLoginScreenPreview() {
    XStreamProTheme {
        XtreamLoginScreen {}
    }
}
