package com.example.nfcshoppingapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nfcshoppingapp.ui.theme.NFCShoppingAppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Delay for 4 seconds, then navigate to MainActivity
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 4000)

        setContent {
            NFCShoppingAppTheme {
                AnimatedSplashScreenContent()
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class) // Needed for slideInVertically and fadeIn
@Composable
fun AnimatedSplashScreenContent() {
    var showLogoInitialDrop by remember { mutableStateOf(false) }
    val offsetY = remember { Animatable(0f) } // For the bouncing effect
    var showBottomText by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Initial delay before the logo starts dropping
        delay(300)
        showLogoInitialDrop = true

        // Animate the initial drop and then the bounce
        launch {
            // Drop from above
            offsetY.animateTo(
                targetValue = 0f, // Final position (relative to its aligned center)
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            // Optional: Add a slight delay after the first drop before the bounce cycles begin
            delay(100)

            // Bounce 1
            offsetY.animateTo(
                targetValue = -30f, // Bounce up
                animationSpec = tween(durationMillis = 200)
            )
            offsetY.animateTo(
                targetValue = 0f, // Back down
                animationSpec = tween(durationMillis = 200)
            )

            // Bounce 2 (smaller bounce)
            offsetY.animateTo(
                targetValue = -15f, // Bounce up
                animationSpec = tween(durationMillis = 150)
            )
            offsetY.animateTo(
                targetValue = 0f, // Back down
                animationSpec = tween(durationMillis = 150)
            )
        }


        // *** MODIFICATION HERE ***
        // Delay for bottom text animation after logo animation largely completes
        // Reduced from 2000ms to 1500ms (or even less if desired)
        delay(800) // This delay should be roughly after the logo bouncing is done
        showBottomText = true
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Logo with animation, placed in the center
        AnimatedVisibility(
            visible = showLogoInitialDrop,
            enter = fadeIn(animationSpec = tween(durationMillis = 500)), // Fade in during the drop
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = offsetY.value.dp) // Apply the animated offset for bouncing
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo), // Using R.drawable.logo
                contentDescription = "Splash Logo",
                modifier = Modifier.size(230.dp) // Retaining the size
            )
        }

        // Bottom text with animation, placed at the bottom-center
        AnimatedVisibility(
            visible = showBottomText,
            enter = fadeIn(animationSpec = tween(durationMillis = 1000)), // Fade in for bottom text
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier.padding(bottom = 24.dp), // Add padding from the bottom edge
                horizontalAlignment = Alignment.CenterHorizontally // Center text horizontally within the Column
            ) {
                Text(
                    text = "Swipe. Scan. Go❤️",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "A Mobile Computing Project",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                Text(
                    text = "Samee | Hanifan | Nana",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewAnimatedSplashScreenContent() {
    NFCShoppingAppTheme {
        AnimatedSplashScreenContent()
    }
}