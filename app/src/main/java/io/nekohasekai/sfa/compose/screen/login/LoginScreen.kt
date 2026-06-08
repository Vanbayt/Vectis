package io.nekohasekai.sfa.compose.screen.login

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import io.nekohasekai.sfa.database.Settings

val BlobShape1 = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    moveTo(w * 0.5f, h * 0.05f)
    cubicTo(w * 0.8f, h * 0.0f, w * 1.0f, h * 0.25f, w * 0.95f, h * 0.5f)
    cubicTo(w * 0.9f, h * 0.8f, w * 0.7f, h * 0.95f, w * 0.5f, h * 0.95f)
    cubicTo(w * 0.2f, h * 0.95f, w * 0.05f, h * 0.8f, w * 0.05f, h * 0.5f)
    cubicTo(w * 0.05f, h * 0.2f, w * 0.2f, h * 0.1f, w * 0.5f, h * 0.05f)
    close()
}

val BlobShape2 = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    moveTo(w * 0.4f, h * 0.1f)
    cubicTo(w * 0.9f, h * 0.05f, w * 0.95f, h * 0.4f, w * 0.9f, h * 0.6f)
    cubicTo(w * 0.8f, h * 0.9f, w * 0.6f, h * 0.95f, w * 0.4f, h * 0.9f)
    cubicTo(w * 0.1f, h * 0.8f, w * 0.0f, h * 0.6f, w * 0.1f, h * 0.4f)
    cubicTo(w * 0.2f, h * 0.15f, w * 0.2f, h * 0.15f, w * 0.4f, h * 0.1f)
    close()
}

@Composable
fun AnimatedBlob(
    shape: Shape,
    color: Color,
    size: Dp,
    durationMillis: Int,
    offset: DpOffset,
    reverse: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition()

    val rotation by infiniteTransition.animateFloat(
        initialValue = if (reverse) 360f else 0f,
        targetValue = if (reverse) 0f else 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = Modifier
            .offset(x = offset.x, y = offset.y)
            .size(size)
            .rotate(rotation)
            .clip(shape)
            .background(color)
    )
}

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
            
            // Native Bezier Blobs
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                // Blob 1 (Large)
                AnimatedBlob(
                    shape = BlobShape1,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    size = 350.dp,
                    offset = DpOffset(x = 60.dp, y = 50.dp),
                    durationMillis = 24000
                )
                
                // Blob 2 (Medium)
                AnimatedBlob(
                    shape = BlobShape2,
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                    size = 280.dp,
                    offset = DpOffset(x = (-80).dp, y = 20.dp),
                    durationMillis = 18000,
                    reverse = true
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
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Secure your connection",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(48.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
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
