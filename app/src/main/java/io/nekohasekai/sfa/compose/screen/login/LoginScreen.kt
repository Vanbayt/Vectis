package io.nekohasekai.sfa.compose.screen.login

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import io.nekohasekai.sfa.database.Settings

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Animated Background Blobs (Cookies)
        AnimatedCookiesBackground()

        // Login Form
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Vectis",
                style = MaterialTheme.typography.displayMedium,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 48.dp)
            )

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                )
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    Settings.token = "dummy_token_${System.currentTimeMillis()}"
                    onLoginSuccess()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                enabled = username.isNotBlank() && password.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    "Login",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun AnimatedCookiesBackground() {
    val infiniteTransition = rememberInfiniteTransition()

    // Slower rotation for Blob 1
    val rotation1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(40000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // Breathing (scale) for Blob 1 to simulate varying speed/direction
    val scale1 by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Slower rotation for Blob 2 in opposite direction
    val rotation2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(45000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // Breathing (scale) for Blob 2
    val scale2 by infiniteTransition.animateFloat(
        initialValue = 1.2f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Less contrasting colors with one explicitly purple
    val color1 = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    val color2 = Color(0xFF9C27B0).copy(alpha = 0.25f)

    Box(modifier = Modifier.fillMaxSize()) {
        // Blob 1 (Top Left)
        Box(
            modifier = Modifier
                .offset(x = (-100).dp, y = (-150).dp)
                .size(450.dp)
                .graphicsLayer {
                    rotationZ = rotation1
                    scaleX = scale1
                    scaleY = scale1
                }
                .background(
                    color = color1,
                    shape = RoundedCornerShape(
                        topStartPercent = 45,
                        topEndPercent = 35,
                        bottomEndPercent = 40,
                        bottomStartPercent = 30
                    )
                )
        )

        // Blob 2 (Bottom Right)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 120.dp, y = 120.dp)
                .size(500.dp)
                .graphicsLayer {
                    rotationZ = rotation2
                    scaleX = scale2
                    scaleY = scale2
                }
                .background(
                    color = color2,
                    shape = RoundedCornerShape(
                        topStartPercent = 35,
                        topEndPercent = 45,
                        bottomEndPercent = 30,
                        bottomStartPercent = 40
                    )
                )
        )
    }
}
