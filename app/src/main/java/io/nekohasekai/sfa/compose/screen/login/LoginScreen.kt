package io.nekohasekai.sfa.compose.screen.login

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import io.nekohasekai.sfa.database.Settings

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            
            // Ambient Background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(40.dp)
            ) {
                // Large shape in top right
                AnimatedAmbientShape(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    size = 400,
                    offsetX = 100,
                    offsetY = -100,
                    durationMillis = 15000
                )
                
                // Medium shape in bottom left
                AnimatedAmbientShape(
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f),
                    size = 300,
                    offsetX = -100,
                    offsetY = 200,
                    durationMillis = 20000
                )

                // Extra small shape to balance
                AnimatedAmbientShape(
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f),
                    size = 200,
                    offsetX = 150,
                    offsetY = 300,
                    durationMillis = 18000
                )
            }

            // Foreground Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Vectis",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(48.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(24.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        Settings.token = "dummy_token_${System.currentTimeMillis()}"
                        onLoginSuccess()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    enabled = username.isNotBlank() && password.isNotBlank()
                ) {
                    Text(
                        text = "Login",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedAmbientShape(
    color: Color,
    size: Int,
    offsetX: Int,
    offsetY: Int,
    durationMillis: Int
) {
    val infiniteTransition = rememberInfiniteTransition()

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (durationMillis * 0.8).toInt(), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .offset(x = offsetX.dp, y = offsetY.dp)
            .size(size.dp)
            .graphicsLayer {
                rotationZ = rotation
                scaleX = scale
                scaleY = scale
            }
            .background(
                color = color,
                shape = RoundedCornerShape(100.dp)
            )
    )
}
