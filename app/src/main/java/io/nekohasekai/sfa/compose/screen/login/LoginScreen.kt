package io.nekohasekai.sfa.compose.screen.login

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.nekohasekai.sfa.database.Settings
import io.nekohasekai.sfa.compose.screen.login.LoginViewModel
import io.nekohasekai.sfa.compose.screen.login.LoginUiState
import org.koin.androidx.compose.koinViewModel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin


@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = koinViewModel()
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) {
            onLoginSuccess()
        } else if (uiState is LoginUiState.Error) {
            snackbarHostState.showSnackbar((uiState as LoginUiState.Error).message)
            viewModel.resetState()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            
            // Native Bezier Blobs
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Blob 1 (Bottom Left)
                AnimatedBlob(
                    shape = WavyCookieShape(points = 12, waveDepth = 0.12f),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    size = 500.dp,
                    offset = DpOffset(x = (-150).dp, y = 250.dp),
                    durationMillis = 30000
                )
                
                // Blob 2 (Right)
                AnimatedBlob(
                    shape = WavyCookieShape(points = 14, waveDepth = 0.1f),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f),
                    size = 400.dp,
                    offset = DpOffset(x = 180.dp, y = 100.dp),
                    durationMillis = 25000,
                    reverse = true
                )
            }

            // Foreground Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                var isLoginMode by remember { mutableStateOf(true) }
                
                Text(
                    text = "Vectis",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                androidx.compose.animation.AnimatedContent(
                    targetState = isLoginMode,
                    transitionSpec = {
                        (androidx.compose.animation.fadeIn(animationSpec = tween(300)) + androidx.compose.animation.slideInVertically { height -> height }).togetherWith(
                            androidx.compose.animation.fadeOut(animationSpec = tween(300)) + androidx.compose.animation.slideOutVertically { height -> -height }
                        )
                    },
                    label = "header"
                ) { loginMode ->
                    Text(
                        text = if (loginMode) "Безопасное подключение" else "Создание аккаунта",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Имя пользователя") },
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
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Пароль") },
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
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        )

                        var confirmPassword by remember { mutableStateOf("") }
                        val passwordsMatch = password == confirmPassword || confirmPassword.isEmpty()

                        androidx.compose.animation.AnimatedVisibility(
                            visible = !isLoginMode,
                            enter = androidx.compose.animation.expandVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) + androidx.compose.animation.fadeIn(),
                            exit = androidx.compose.animation.shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) + androidx.compose.animation.fadeOut()
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedTextField(
                                    value = confirmPassword,
                                    onValueChange = { confirmPassword = it },
                                    label = { Text("Подтвердите пароль") },
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
                                    isError = !passwordsMatch && confirmPassword.isNotEmpty(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        errorBorderColor = MaterialTheme.colorScheme.error,
                                        errorLeadingIconColor = MaterialTheme.colorScheme.error,
                                        errorLabelColor = MaterialTheme.colorScheme.error
                                    )
                                )
                                androidx.compose.animation.AnimatedVisibility(visible = !passwordsMatch && confirmPassword.isNotEmpty()) {
                                    Text(
                                        text = "Пароли не совпадают",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        val isFormValid = if (isLoginMode) {
                            username.isNotBlank() && password.isNotBlank()
                        } else {
                            username.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank() && passwordsMatch
                        }

                        Button(
                            onClick = { 
                                if (isLoginMode) {
                                    viewModel.login(username, password)
                                } else {
                                    viewModel.register(username, password)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            enabled = isFormValid && uiState !is LoginUiState.Loading
                        ) {
                            androidx.compose.animation.AnimatedContent(
                                targetState = uiState is LoginUiState.Loading,
                                label = "button_content"
                            ) { isLoading ->
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = if (isLoginMode) "Войти" else "Зарегистрироваться",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        TextButton(
                            onClick = { 
                                isLoginMode = !isLoginMode
                                if (isLoginMode) {
                                    confirmPassword = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            androidx.compose.animation.AnimatedContent(
                                targetState = isLoginMode,
                                label = "switch_mode"
                            ) { loginMode ->
                                Text(
                                    text = if (loginMode) "Нет аккаунта? Зарегистрироваться" else "Уже есть аккаунт? Войти",
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
