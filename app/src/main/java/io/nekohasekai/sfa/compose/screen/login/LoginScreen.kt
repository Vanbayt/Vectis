package io.nekohasekai.sfa.compose.screen.login

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import io.nekohasekai.sfa.database.Settings
import kotlin.math.cos
import kotlin.math.sin

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
    
    val rotation1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val rotation2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val color1 = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
    val color2 = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Draw First Cookie Blob
        rotate(degrees = rotation1, pivot = Offset(width * 0.2f, height * 0.2f)) {
            val path = createBlobPath(width * 0.2f, height * 0.2f, width * 0.4f)
            drawPath(path, color = color1)
        }

        // Draw Second Cookie Blob
        rotate(degrees = rotation2, pivot = Offset(width * 0.8f, height * 0.8f)) {
            val path = createBlobPath(width * 0.8f, height * 0.8f, width * 0.5f)
            drawPath(path, color = color2)
        }
    }
}

private fun createBlobPath(centerX: Float, centerY: Float, radius: Float): Path {
    val path = Path()
    val points = 8
    val angleStep = (2 * Math.PI) / points

    for (i in 0 until points) {
        val angle = i * angleStep
        // Randomize radius slightly for blob shape
        val r = radius * (0.8f + 0.4f * (i % 2)) 
        val x = centerX + r * cos(angle).toFloat()
        val y = centerY + r * sin(angle).toFloat()
        
        if (i == 0) {
            path.moveTo(x, y)
        } else {
            // Smooth curve to next point
            val prevAngle = (i - 1) * angleStep
            val prevR = radius * (0.8f + 0.4f * ((i - 1) % 2))
            val prevX = centerX + prevR * cos(prevAngle).toFloat()
            val prevY = centerY + prevR * sin(prevAngle).toFloat()
            
            val ctrlX = centerX + radius * cos(prevAngle + angleStep / 2).toFloat()
            val ctrlY = centerY + radius * sin(prevAngle + angleStep / 2).toFloat()
            
            path.quadraticBezierTo(ctrlX, ctrlY, x, y)
        }
    }
    path.close()
    return path
}
