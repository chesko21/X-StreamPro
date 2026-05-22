package com.chesko.x_streampro.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chesko.x_streampro.R
import com.chesko.x_streampro.ui.theme.XStreamProTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onNavigateToNext: () -> Unit) {
    // GSAP-inspired staggered animation states
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.5f) }
    val logoOffsetY = remember { Animatable(20f) }
    
    val titleAlpha = remember { Animatable(0f) }
    val titleOffsetY = remember { Animatable(10f) }
    
    val subtitleAlpha = remember { Animatable(0f) }
    
    val bottomContentAlpha = remember { Animatable(0f) }
    val progressWidth = remember { Animatable(0f) }

    // Particle/Background animation
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val bgShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bg_shift"
    )

    LaunchedEffect(Unit) {
        // Sequence of animations
        launch {
            // 1. Logo Pop
            logoAlpha.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
        }
        launch {
            logoScale.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow))
        }
        launch {
            logoOffsetY.animateTo(0f, tween(1000, easing = EaseOutBack))
        }
        
        delay(400) // Stagger title
        launch {
            // 2. Title Slide
            titleAlpha.animateTo(1f, tween(800))
            titleOffsetY.animateTo(0f, tween(800, easing = EaseOutQuart))
        }
        
        delay(300) // Stagger subtitle
        launch {
            // 3. Subtitle fade
            subtitleAlpha.animateTo(1f, tween(1000))
        }
        
        delay(200) // Stagger progress
        launch {
            // 4. Progress and developer tag
            bottomContentAlpha.animateTo(1f, tween(800))
            progressWidth.animateTo(1f, tween(2000, easing = EaseInOutCubic))
        }
        
        delay(3200)
        onNavigateToNext()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030303)),
        contentAlignment = Alignment.Center
    ) {
        // Dynamic Radial Gradient Background (GSAP Style movement)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.15f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(MaterialTheme.colorScheme.primary, Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(x = 500f + bgShift, y = 500f - bgShift)
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated Logo
            Image(
                painter = painterResource(id = R.drawable.app_icon_android),
                contentDescription = "Logo",
                modifier = Modifier
                    .size(150.dp)
                    .graphicsLayer {
                        alpha = logoAlpha.value
                        scaleX = logoScale.value
                        scaleY = logoScale.value
                        translationY = logoOffsetY.value
                    }
                    .clip(RoundedCornerShape(32.dp)),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Animated Title
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    alpha = titleAlpha.value
                    translationY = titleOffsetY.value
                }
            ) {
                Text(
                    text = "X-STREAM",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 14.sp,
                        color = Color.White
                    )
                )
                Text(
                    text = "PRO",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraLight,
                        letterSpacing = 22.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Animated Subtitle
            Text(
                text = "THE NEXT GEN STREAMING",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 6.sp,
                    fontWeight = FontWeight.Thin,
                    color = Color.Gray.copy(alpha = 0.6f)
                ),
                modifier = Modifier.alpha(subtitleAlpha.value)
            )
        }

        // Bottom Content (GSAP Style Loading & Branding)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
                .alpha(bottomContentAlpha.value),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Custom thin loading bar
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressWidth.value)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "POWERED BY",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 3.sp,
                    fontWeight = FontWeight.Light,
                    fontSize = 7.sp,
                    color = Color.Gray.copy(alpha = 0.3f)
                )
            )
            
            Text(
                text = "CHESKO DEV TEAM",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 5.sp,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            )
        }
    }
}

// Ease curves similar to GSAP
val EaseOutBack = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
val EaseOutQuart = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)
val EaseInOutCubic = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    XStreamProTheme {
        SplashScreen(onNavigateToNext = {})
    }
}
